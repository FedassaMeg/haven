package org.haven.reporting.application.services;

import org.haven.reporting.domain.*;
import org.haven.reporting.infrastructure.persistence.ExportAuditMetadataRepository;
import org.haven.reporting.infrastructure.storage.CsvBlobStorageService;
import org.haven.shared.audit.*;
import org.haven.shared.security.AccessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Application service for HUD export job orchestration
 *
 * Coordinates the export pipeline:
 * 1. Queue export job (create ExportJobAggregate)
 * 2. Materialize views (HUDExportViewGenerator with VAWA filtering)
 * 3. Validate data quality (HUD compliance rules)
 * 4. Store CSV artifacts (CsvBlobStorageService)
 * 5. Create audit metadata (ExportAuditMetadata)
 * 6. Complete or fail the job
 *
 * Uses @Async with dedicated thread pool to prevent blocking transaction pool
 */
@Service
public class ExportJobApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ExportJobApplicationService.class);

    private final ExportJobRepository exportJobRepository;
    private final HUDExportViewGenerator viewGenerator;
    private final CsvBlobStorageService blobStorageService;
    private final ExportAuditMetadataRepository auditMetadataRepository;
    private final PrivilegedAuditService privilegedAuditService;

    public ExportJobApplicationService(
            ExportJobRepository exportJobRepository,
            HUDExportViewGenerator viewGenerator,
            CsvBlobStorageService blobStorageService,
            ExportAuditMetadataRepository auditMetadataRepository,
            PrivilegedAuditService privilegedAuditService) {
        this.exportJobRepository = exportJobRepository;
        this.viewGenerator = viewGenerator;
        this.blobStorageService = blobStorageService;
        this.auditMetadataRepository = auditMetadataRepository;
        this.privilegedAuditService = privilegedAuditService;
    }

    /**
     * Request a new HUD export
     * Returns export job ID for tracking
     */
    @Transactional
    public UUID requestExport(
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> projectIds,
            boolean includeAggregateOnly,
            AccessContext accessContext) {

        logger.info("Requesting {} export for period {} to {} by user {}",
                exportType, reportingPeriodStart, reportingPeriodEnd, accessContext.getUserName());

        // Use default values for cocCode and exportReason
        String cocCode = "DEFAULT_COC";
        String exportReason = includeAggregateOnly ? "Aggregate-only export requested" : "Individual-level export requested";

        ExportJobAggregate exportJob = ExportJobAggregate.queueExport(
                exportType,
                reportingPeriodStart,
                reportingPeriodEnd,
                projectIds,
                accessContext.getUserId().toString(),
                accessContext.getUserName(),
                cocCode,
                exportReason
        );

        exportJobRepository.save(exportJob);

        UUID exportJobId = exportJob.getId().value();
        logger.info("Export job queued: {}", exportJobId);

        // Emit privileged audit event for export initiation
        privilegedAuditService.logAction(
            PrivilegedAuditEvent.builder()
                .eventType(PrivilegedActionType.EXPORT_INITIATED)
                .outcome(AuditOutcome.SUCCESS)
                .actorId(accessContext.getUserId())
                .actorUsername(accessContext.getUserName())
                .actorRoles(accessContext.getRoles())
                .resourceType("ExportJob")
                .resourceId(exportJobId)
                .resourceDescription(exportType + " export for " + reportingPeriodStart + " to " + reportingPeriodEnd)
                .justification(exportReason)
                .sessionId(accessContext.getSessionId())
                .ipAddress(accessContext.getIpAddress())
                .addMetadata("exportType", exportType)
                .addMetadata("reportingPeriodStart", reportingPeriodStart.toString())
                .addMetadata("reportingPeriodEnd", reportingPeriodEnd.toString())
                .addMetadata("projectCount", projectIds.size())
                .addMetadata("cocCode", cocCode)
                .addMetadata("includeAggregateOnly", includeAggregateOnly)
                .build()
        );

        // Trigger async processing
        processExportAsync(exportJobId, accessContext);

        return exportJobId;
    }

    /**
     * Process export job asynchronously
     */
    @Async("reportGenerationExecutor")
    @Transactional
    public CompletableFuture<Void> processExportAsync(UUID exportJobId, AccessContext accessContext) {
        return CompletableFuture.runAsync(() -> {
            try {
                processExport(exportJobId, accessContext);
            } catch (Exception e) {
                logger.error("Export job processing failed: {}", exportJobId, e);
                failExport(exportJobId, e.getMessage(), "PROCESSING_ERROR", Collections.emptyList());
            }
        });
    }

    /**
     * Process export job through all phases
     */
    @Transactional
    public void processExport(UUID exportJobId, AccessContext accessContext) {
        logger.info("Processing export job: {}", exportJobId);

        ExportJobAggregate exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + exportJobId));

        try {
            // Phase 1: Materialization
            exportJob.startMaterialization();
            exportJobRepository.save(exportJob);

            Map<String, String> csvFiles = materializeViews(exportJob);

            // Phase 2: Validation
            long recordCount = countTotalRecords(csvFiles);
            exportJob.startValidation(recordCount);
            exportJobRepository.save(exportJob);

            List<String> validationErrors = validateExport(csvFiles);
            if (!validationErrors.isEmpty()) {
                exportJob.fail("Validation failed", "VALIDATION_ERROR", validationErrors);
                exportJobRepository.save(exportJob);
                return;
            }

            // Phase 3: Storage
            CsvBlobStorageService.StorageResult storageResult = blobStorageService.storeCsvFiles(
                    exportJobId, csvFiles);

            // Phase 4: Completion
            long vawaSupressed = countVawaSuppressedRecords(csvFiles);
            exportJob.complete(
                    storageResult.getStorageUrl(),
                    storageResult.getSha256Hash(),
                    recordCount,
                    vawaSupressed,
                    storageResult.getStoredFiles()
            );
            exportJobRepository.save(exportJob);

            // Phase 5: Audit metadata
            createAuditMetadata(exportJob, storageResult, accessContext, vawaSupressed);

            // Emit privileged audit event for successful export completion
            privilegedAuditService.logAction(
                PrivilegedAuditEvent.builder()
                    .eventType(PrivilegedActionType.EXPORT_COMPLETED)
                    .outcome(AuditOutcome.SUCCESS)
                    .actorId(accessContext.getUserId())
                    .actorUsername(accessContext.getUserName())
                    .actorRoles(accessContext.getRoles())
                    .resourceType("ExportJob")
                    .resourceId(exportJobId)
                    .resourceDescription(exportJob.getExportType() + " export completed")
                    .hashFingerprint(storageResult.getSha256Hash())
                    .sessionId(accessContext.getSessionId())
                    .ipAddress(accessContext.getIpAddress())
                    .addMetadata("storageUrl", storageResult.getStorageUrl())
                    .addMetadata("totalRecords", recordCount)
                    .addMetadata("vawaSuppressed", vawaSupressed)
                    .addMetadata("fileCount", storageResult.getStoredFiles().size())
                    .build()
            );

            logger.info("Export job completed successfully: {}", exportJobId);

        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Export job failed: {}", exportJobId, e);
            exportJob.fail(e.getMessage(), "STORAGE_ERROR", Collections.emptyList());
            exportJobRepository.save(exportJob);

            // Emit privileged audit event for export failure
            privilegedAuditService.logAction(
                PrivilegedAuditEvent.builder()
                    .eventType(PrivilegedActionType.EXPORT_FAILED)
                    .outcome(AuditOutcome.ERROR_SYSTEM_FAILURE)
                    .actorId(accessContext.getUserId())
                    .actorUsername(accessContext.getUserName())
                    .actorRoles(accessContext.getRoles())
                    .resourceType("ExportJob")
                    .resourceId(exportJobId)
                    .resourceDescription(exportJob.getExportType() + " export failed")
                    .denialReason("STORAGE_ERROR")
                    .denialDetails(e.getMessage())
                    .sessionId(accessContext.getSessionId())
                    .ipAddress(accessContext.getIpAddress())
                    .build()
            );
        }
    }

    /**
     * Materialize CSV views
     */
    private Map<String, String> materializeViews(ExportJobAggregate exportJob) {
        logger.info("Materializing views for export job: {}", exportJob.getId().value());

        ExportPeriod period = ExportPeriod.between(
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd()
        );

        Map<String, String> csvFiles = new LinkedHashMap<>();

        // Generate Client.csv
        List<Map<String, Object>> clientData = viewGenerator.generateClientCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode());
        csvFiles.put("Client.csv", convertToCsv(clientData));

        // Generate Enrollment.csv
        List<Map<String, Object>> enrollmentData = viewGenerator.generateEnrollmentCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode());
        csvFiles.put("Enrollment.csv", convertToCsv(enrollmentData));

        // Generate Services.csv (with VAWA filtering)
        List<Map<String, Object>> servicesData = viewGenerator.generateServicesCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode());
        csvFiles.put("Services.csv", convertToCsv(servicesData));

        // Generate CurrentLivingSituation.csv (with VAWA filtering)
        List<Map<String, Object>> clsData = viewGenerator.generateCurrentLivingSituationCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode());
        csvFiles.put("CurrentLivingSituation.csv", convertToCsv(clsData));

        logger.info("Generated {} CSV files", csvFiles.size());
        return csvFiles;
    }

    /**
     * Convert list of maps to CSV string
     */
    private String convertToCsv(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return "";
        }

        StringBuilder csv = new StringBuilder();

        // Header row
        Set<String> headers = data.get(0).keySet();
        csv.append(String.join(",", headers)).append("\n");

        // Data rows
        for (Map<String, Object> row : data) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object value = row.get(header);
                String valueStr = value != null ? value.toString() : "";
                // Escape commas and quotes
                if (valueStr.contains(",") || valueStr.contains("\"")) {
                    valueStr = "\"" + valueStr.replace("\"", "\"\"") + "\"";
                }
                values.add(valueStr);
            }
            csv.append(String.join(",", values)).append("\n");
        }

        return csv.toString();
    }

    /**
     * Count total records across all CSV files
     */
    private long countTotalRecords(Map<String, String> csvFiles) {
        return csvFiles.values().stream()
                .mapToLong(csv -> csv.split("\n").length - 1) // -1 for header row
                .sum();
    }

    /**
     * Count VAWA-suppressed records
     * In production, this would track actual suppressions during generation
     */
    private long countVawaSuppressedRecords(Map<String, String> csvFiles) {
        // TODO: Implement proper tracking during view generation
        // For now, return 0 as placeholder
        return 0L;
    }

    /**
     * Validate export data quality
     */
    private List<String> validateExport(Map<String, String> csvFiles) {
        List<String> errors = new ArrayList<>();

        // Basic validation: ensure required files exist
        if (!csvFiles.containsKey("Client.csv")) {
            errors.add("Missing required file: Client.csv");
        }
        if (!csvFiles.containsKey("Enrollment.csv")) {
            errors.add("Missing required file: Enrollment.csv");
        }

        // Additional HUD validation rules would go here
        // - Check required fields are populated
        // - Validate data types and code lists
        // - Check referential integrity

        return errors;
    }

    /**
     * Create audit metadata record
     */
    private void createAuditMetadata(
            ExportJobAggregate exportJob,
            CsvBlobStorageService.StorageResult storageResult,
            AccessContext accessContext,
            long vawaSupressed) {

        ExportAuditMetadata metadata = new ExportAuditMetadata(
                exportJob.getId().value(),
                accessContext,
                exportJob.getExportType(),
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd(),
                exportJob.getIncludedProjectIds(),
                exportJob.getCocCode(),
                storageResult.getSha256Hash(),
                storageResult.getStorageUrl(),
                exportJob.getTotalRecords(),
                vawaSupressed,
                0L, // VAWA redacted fields count - would be tracked during generation
                storageResult.getStoredFiles(),
                exportJob.getCompletedAt(),
                storageResult.getExpiresAt()
        );

        auditMetadataRepository.save(metadata);
        logger.info("Created audit metadata for export job: {}", exportJob.getId().value());
    }

    /**
     * Fail export job
     */
    @Transactional
    public void failExport(UUID exportJobId, String errorMessage, String errorCode, List<String> validationErrors) {
        exportJobRepository.findById(exportJobId).ifPresent(exportJob -> {
            exportJob.fail(errorMessage, errorCode, validationErrors);
            exportJobRepository.save(exportJob);
        });
    }

    /**
     * Get export job status
     */
    @Transactional(readOnly = true)
    public ExportJobStatus getExportJobStatus(UUID exportJobId, AccessContext accessContext) {
        ExportJobAggregate exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + exportJobId));

        // Security check: user can only view their own jobs unless admin
        if (!exportJob.getRequestedByUserId().equals(accessContext.getUserId().toString())
            && !accessContext.getRoles().contains("ADMINISTRATOR")) {
            throw new SecurityException("User does not have access to this export job");
        }

        return new ExportJobStatus(
                exportJobId,
                exportJob.getState(),
                exportJob.getExportType(),
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd(),
                exportJob.getQueuedAt(),
                exportJob.getStartedAt(),
                exportJob.getCompletedAt(),
                exportJob.getErrorMessage(),
                exportJob.getDownloadUrl(),
                exportJob.getSha256Hash(),
                exportJob.getTotalRecords(),
                exportJob.getVawaSupressedRecords()
        );
    }

    /**
     * Get export jobs for user
     */
    @Transactional(readOnly = true)
    public List<ExportJobSummary> getExportJobsForUser(UUID userId, String state) {
        List<ExportJobAggregate> jobs = exportJobRepository.findByRequestedByUserId(userId.toString());

        // Filter by state if provided
        if (state != null && !state.isEmpty()) {
            ExportJobState filterState = ExportJobState.valueOf(state);
            jobs = jobs.stream()
                    .filter(job -> job.getState() == filterState)
                    .collect(Collectors.toList());
        }

        return jobs.stream()
                .map(job -> new ExportJobSummary(
                        job.getId().value(),
                        job.getState(),
                        job.getExportType(),
                        job.getReportingPeriodStart(),
                        job.getReportingPeriodEnd(),
                        job.getQueuedAt(),
                        job.getCompletedAt(),
                        job.getIncludedProjectIds() != null ? job.getIncludedProjectIds().size() : 0
                ))
                .collect(Collectors.toList());
    }

    /**
     * Cancel export job
     */
    @Transactional
    public void cancelExport(UUID exportJobId, AccessContext accessContext) {
        ExportJobAggregate exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + exportJobId));

        // Security check
        if (!exportJob.getRequestedByUserId().equals(accessContext.getUserId().toString())
            && !accessContext.getRoles().contains("ADMINISTRATOR")) {
            throw new SecurityException("User does not have access to this export job");
        }

        // Can only cancel jobs in QUEUED or MATERIALIZING state
        if (exportJob.getState() != ExportJobState.QUEUED && exportJob.getState() != ExportJobState.MATERIALIZING) {
            throw new IllegalStateException("Cannot cancel job in state: " + exportJob.getState());
        }

        exportJob.fail("Cancelled by user", "USER_CANCELLED", Collections.emptyList());
        exportJobRepository.save(exportJob);

        logger.info("Export job cancelled: {}", exportJobId);
    }

    /**
     * Retry failed export job
     */
    @Transactional
    public void retryExport(UUID exportJobId, AccessContext accessContext) {
        ExportJobAggregate exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + exportJobId));

        // Security check
        if (!exportJob.getRequestedByUserId().equals(accessContext.getUserId().toString())
            && !accessContext.getRoles().contains("ADMINISTRATOR")) {
            throw new SecurityException("User does not have access to this export job");
        }

        // Can only retry failed jobs
        if (exportJob.getState() != ExportJobState.FAILED) {
            throw new IllegalStateException("Cannot retry job in state: " + exportJob.getState());
        }

        // Reset job to queued state (would need to add method to ExportJobAggregate)
        // For now, create a new job with same parameters
        UUID newJobId = requestExport(
                exportJob.getExportType(),
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd(),
                exportJob.getIncludedProjectIds(),
                false, // includeAggregateOnly
                accessContext
        );

        logger.info("Export job retried. Old: {}, New: {}", exportJobId, newJobId);
    }

    /**
     * Get download metadata
     */
    @Transactional(readOnly = true)
    public DownloadMetadata getDownloadMetadata(UUID exportJobId, AccessContext accessContext) {
        ExportJobAggregate exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + exportJobId));

        // Security check
        boolean accessGranted = exportJob.getRequestedByUserId().equals(accessContext.getUserId().toString())
            || accessContext.getRoles().contains("ADMINISTRATOR");

        if (!accessGranted) {
            return new DownloadMetadata(false, null, null, null);
        }

        // Can only download completed jobs
        if (exportJob.getState() != ExportJobState.COMPLETE) {
            return new DownloadMetadata(false, null, null, "Export not completed");
        }

        // Log download access
        privilegedAuditService.logAction(
            PrivilegedAuditEvent.builder()
                .eventType(PrivilegedActionType.EXPORT_DOWNLOADED)
                .outcome(AuditOutcome.SUCCESS)
                .actorId(accessContext.getUserId())
                .actorUsername(accessContext.getUserName())
                .actorRoles(accessContext.getRoles())
                .resourceType("ExportJob")
                .resourceId(exportJobId)
                .resourceDescription(exportJob.getExportType() + " export download")
                .hashFingerprint(exportJob.getSha256Hash())
                .sessionId(accessContext.getSessionId())
                .ipAddress(accessContext.getIpAddress())
                .build()
        );

        return new DownloadMetadata(
                true,
                exportJob.getDownloadUrl(),
                exportJob.getSha256Hash(),
                null
        );
    }

    // DTO classes

    public static class ExportJobStatus {
        private final UUID exportJobId;
        private final ExportJobState state;
        private final String exportType;
        private final LocalDate reportingPeriodStart;
        private final LocalDate reportingPeriodEnd;
        private final java.time.Instant queuedAt;
        private final java.time.Instant startedAt;
        private final java.time.Instant completedAt;
        private final String errorMessage;
        private final String downloadUrl;
        private final String sha256Hash;
        private final Long totalRecords;
        private final Long vawaSupressedRecords;

        public ExportJobStatus(UUID exportJobId, ExportJobState state, String exportType,
                              LocalDate reportingPeriodStart, LocalDate reportingPeriodEnd,
                              java.time.Instant queuedAt, java.time.Instant startedAt,
                              java.time.Instant completedAt, String errorMessage,
                              String downloadUrl, String sha256Hash,
                              Long totalRecords, Long vawaSupressedRecords) {
            this.exportJobId = exportJobId;
            this.state = state;
            this.exportType = exportType;
            this.reportingPeriodStart = reportingPeriodStart;
            this.reportingPeriodEnd = reportingPeriodEnd;
            this.queuedAt = queuedAt;
            this.startedAt = startedAt;
            this.completedAt = completedAt;
            this.errorMessage = errorMessage;
            this.downloadUrl = downloadUrl;
            this.sha256Hash = sha256Hash;
            this.totalRecords = totalRecords;
            this.vawaSupressedRecords = vawaSupressedRecords;
        }

        public UUID getExportJobId() { return exportJobId; }
        public ExportJobState getState() { return state; }
        public String getExportType() { return exportType; }
        public LocalDate getReportingPeriodStart() { return reportingPeriodStart; }
        public LocalDate getReportingPeriodEnd() { return reportingPeriodEnd; }
        public java.time.Instant getQueuedAt() { return queuedAt; }
        public java.time.Instant getStartedAt() { return startedAt; }
        public java.time.Instant getCompletedAt() { return completedAt; }
        public String getErrorMessage() { return errorMessage; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getSha256Hash() { return sha256Hash; }
        public Long getTotalRecords() { return totalRecords; }
        public Long getVawaSupressedRecords() { return vawaSupressedRecords; }
    }

    public static class ExportJobSummary {
        private final UUID exportJobId;
        private final ExportJobState state;
        private final String exportType;
        private final LocalDate reportingPeriodStart;
        private final LocalDate reportingPeriodEnd;
        private final java.time.Instant queuedAt;
        private final java.time.Instant completedAt;
        private final int projectCount;

        public ExportJobSummary(UUID exportJobId, ExportJobState state, String exportType,
                               LocalDate reportingPeriodStart, LocalDate reportingPeriodEnd,
                               java.time.Instant queuedAt, java.time.Instant completedAt,
                               int projectCount) {
            this.exportJobId = exportJobId;
            this.state = state;
            this.exportType = exportType;
            this.reportingPeriodStart = reportingPeriodStart;
            this.reportingPeriodEnd = reportingPeriodEnd;
            this.queuedAt = queuedAt;
            this.completedAt = completedAt;
            this.projectCount = projectCount;
        }

        public UUID getExportJobId() { return exportJobId; }
        public ExportJobState getState() { return state; }
        public String getExportType() { return exportType; }
        public LocalDate getReportingPeriodStart() { return reportingPeriodStart; }
        public LocalDate getReportingPeriodEnd() { return reportingPeriodEnd; }
        public java.time.Instant getQueuedAt() { return queuedAt; }
        public java.time.Instant getCompletedAt() { return completedAt; }
        public int getProjectCount() { return projectCount; }
    }

    public static class DownloadMetadata {
        private final boolean accessGranted;
        private final String downloadUrl;
        private final String sha256Hash;
        private final String denialReason;

        public DownloadMetadata(boolean accessGranted, String downloadUrl,
                               String sha256Hash, String denialReason) {
            this.accessGranted = accessGranted;
            this.downloadUrl = downloadUrl;
            this.sha256Hash = sha256Hash;
            this.denialReason = denialReason;
        }

        public boolean isAccessGranted() { return accessGranted; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getSha256Hash() { return sha256Hash; }
        public String getDenialReason() { return denialReason; }
    }
}
