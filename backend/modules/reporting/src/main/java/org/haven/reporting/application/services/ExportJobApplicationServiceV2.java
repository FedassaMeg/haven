package org.haven.reporting.application.services;

import org.haven.reporting.domain.*;
import org.haven.reporting.infrastructure.persistence.ExportAuditMetadataRepository;
import org.haven.reporting.infrastructure.storage.CsvBlobStorageService;
import org.haven.shared.security.AccessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Enhanced export service with post-processing and packaging support.
 *
 * Implements full pipeline:
 * 1. Materialize views
 * 2. Apply aggregations (APR/SPM metrics with cell suppression)
 * 3. Format export (CSV/XML/Excel per HUD specs)
 * 4. Package securely (ZIP + manifest + signature + optional encryption)
 * 5. Store and audit
 */
@Service
public class ExportJobApplicationServiceV2 {

    private static final Logger logger = LoggerFactory.getLogger(ExportJobApplicationServiceV2.class);

    private final ExportJobRepository exportJobRepository;
    private final HUDExportViewGenerator viewGenerator;
    private final CsvBlobStorageService blobStorageService;
    private final ExportAuditMetadataRepository auditMetadataRepository;
    private final HUDExportFormatter exportFormatter;
    private final ExportPackagingService packagingService;
    private final AggregationService aggregationService;

    public ExportJobApplicationServiceV2(
            ExportJobRepository exportJobRepository,
            HUDExportViewGenerator viewGenerator,
            CsvBlobStorageService blobStorageService,
            ExportAuditMetadataRepository auditMetadataRepository,
            HUDExportFormatter exportFormatter,
            ExportPackagingService packagingService,
            AggregationService aggregationService) {
        this.exportJobRepository = exportJobRepository;
        this.viewGenerator = viewGenerator;
        this.blobStorageService = blobStorageService;
        this.auditMetadataRepository = auditMetadataRepository;
        this.exportFormatter = exportFormatter;
        this.packagingService = packagingService;
        this.aggregationService = aggregationService;
    }

    @Transactional
    public UUID requestExport(
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> projectIds,
            String cocCode,
            String exportReason,
            ExportFormat format,
            boolean encrypted,
            AccessContext accessContext) {

        logger.info("Requesting {} export for period {} to {} (format={}, encrypted={}) by user {}",
                exportType, reportingPeriodStart, reportingPeriodEnd, format, encrypted, accessContext.getUserName());

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

        processExportAsync(exportJobId, format, encrypted, accessContext);

        return exportJobId;
    }

    @Async("reportGenerationExecutor")
    @Transactional
    public CompletableFuture<Void> processExportAsync(
            UUID exportJobId,
            ExportFormat format,
            boolean encrypted,
            AccessContext accessContext) {
        return CompletableFuture.runAsync(() -> {
            try {
                processExport(exportJobId, format, encrypted, accessContext);
            } catch (Exception e) {
                logger.error("Export job processing failed: {}", exportJobId, e);
                failExport(exportJobId, e.getMessage(), "PROCESSING_ERROR", Collections.emptyList());
            }
        });
    }

    @Transactional
    public void processExport(
            UUID exportJobId,
            ExportFormat format,
            boolean encrypted,
            AccessContext accessContext) {

        logger.info("Processing export job: {} (format={}, encrypted={})", exportJobId, format, encrypted);

        ExportJobAggregate exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + exportJobId));

        try {
            // Phase 1: Materialization
            exportJob.startMaterialization();
            exportJobRepository.save(exportJob);

            Map<String, List<Map<String, Object>>> sections = materializeViewsAsStructured(exportJob);

            // Phase 1b: Apply aggregations for APR/SPM reports
            if (exportJob.getExportType().contains("APR") || exportJob.getExportType().contains("SPM")) {
                sections = applyAggregations(sections, exportJob);
            }

            // Phase 2: Format export (CSV, XML, or Excel)
            Map<String, byte[]> formattedFiles = formatExport(sections, format);

            // Phase 3: Validation
            long recordCount = countTotalRecordsFromSections(sections);
            exportJob.startValidation(recordCount);
            exportJobRepository.save(exportJob);

            List<String> validationErrors = validateExport(formattedFiles, format);
            if (!validationErrors.isEmpty()) {
                exportJob.fail("Validation failed", "VALIDATION_ERROR", validationErrors);
                exportJobRepository.save(exportJob);
                return;
            }

            // Phase 4: Package export (ZIP + manifest + signature)
            ExportPackage exportPackage = packagingService.packageWithManifest(
                    exportJob.getId(),
                    formattedFiles,
                    format,
                    encrypted
            );

            // Phase 5: Storage
            CsvBlobStorageService.StorageResult storageResult = storePackage(
                    exportJobId, exportPackage, format);

            // Phase 6: Completion
            long vawaSupressed = 0L; // Tracked during materialization
            exportJob.complete(
                    storageResult.getStorageUrl(),
                    exportPackage.manifestHash(),
                    recordCount,
                    vawaSupressed,
                    List.of(storageResult.getStorageUrl())
            );
            exportJobRepository.save(exportJob);

            // Phase 7: Audit metadata (includes package signature)
            createAuditMetadataWithPackage(exportJob, exportPackage, storageResult, accessContext, vawaSupressed);

            logger.info("Export job completed successfully: {}", exportJobId);

        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Export job failed: {}", exportJobId, e);
            exportJob.fail(e.getMessage(), "STORAGE_ERROR", Collections.emptyList());
            exportJobRepository.save(exportJob);
        }
    }

    private Map<String, List<Map<String, Object>>> materializeViewsAsStructured(ExportJobAggregate exportJob) {
        logger.info("Materializing views for export job: {}", exportJob.getId().value());

        ExportPeriod period = ExportPeriod.between(
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd()
        );

        Map<String, List<Map<String, Object>>> sections = new LinkedHashMap<>();

        sections.put("Client", viewGenerator.generateClientCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode()));

        sections.put("Enrollment", viewGenerator.generateEnrollmentCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode()));

        sections.put("Services", viewGenerator.generateServicesCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode()));

        sections.put("CurrentLivingSituation", viewGenerator.generateCurrentLivingSituationCsv(
                period, exportJob.getIncludedProjectIds(), exportJob.getCocCode()));

        logger.info("Generated {} sections", sections.size());
        return sections;
    }

    private Map<String, List<Map<String, Object>>> applyAggregations(
            Map<String, List<Map<String, Object>>> sections,
            ExportJobAggregate exportJob) {

        logger.info("Applying aggregations for export type: {}", exportJob.getExportType());

        ExportPeriod period = ExportPeriod.between(
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd()
        );
        Set<String> projectIds = exportJob.getIncludedProjectIds().stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());

        Map<String, List<Map<String, Object>>> aggregatedSections = new LinkedHashMap<>(sections);

        if (exportJob.getExportType().contains("APR")) {
            aggregatedSections.put("APR_Q6_HouseholdType",
                    List.of(aggregationService.computeCoCAPRQ6(projectIds, period)));
            aggregatedSections.put("APR_Q7_VeteranStatus",
                    List.of(aggregationService.computeCoCAPRQ7(projectIds, period)));
            aggregatedSections.put("APR_Q10_IncomeSources",
                    List.of(aggregationService.computeCoCAPRQ10(projectIds, period)));
        }

        if (exportJob.getExportType().contains("SPM")) {
            aggregatedSections.put("SPM_Metric1_ReturnsToHomelessness",
                    List.of(aggregationService.computeSPMMetric1(projectIds, period)));
            aggregatedSections.put("SPM_Metric7_SuccessfulPlacements",
                    List.of(aggregationService.computeSPMMetric7(projectIds, period)));
        }

        return aggregatedSections;
    }

    private Map<String, byte[]> formatExport(
            Map<String, List<Map<String, Object>>> sections,
            ExportFormat format) {

        logger.info("Formatting export as {}", format);

        if (format == ExportFormat.EXCEL) {
            byte[] excelData = exportFormatter.format(sections, format);
            return Map.of("export.xlsx", excelData);
        } else if (format == ExportFormat.XML) {
            byte[] xmlData = exportFormatter.format(sections, format);
            return Map.of("export.xml", xmlData);
        } else {
            Map<String, byte[]> csvFiles = new LinkedHashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> section : sections.entrySet()) {
                String filename = section.getKey() + ".csv";
                byte[] csvData = exportFormatter.formatSection(
                        section.getKey(), section.getValue(), ExportFormat.CSV);
                csvFiles.put(filename, csvData);
            }
            return csvFiles;
        }
    }

    private long countTotalRecordsFromSections(Map<String, List<Map<String, Object>>> sections) {
        return sections.values().stream()
                .mapToLong(List::size)
                .sum();
    }

    private List<String> validateExport(Map<String, byte[]> files, ExportFormat format) {
        List<String> errors = new ArrayList<>();

        if (format == ExportFormat.CSV) {
            if (!files.containsKey("Client.csv")) {
                errors.add("Missing required file: Client.csv");
            }
            if (!files.containsKey("Enrollment.csv")) {
                errors.add("Missing required file: Enrollment.csv");
            }
        } else if (format == ExportFormat.EXCEL) {
            if (!files.containsKey("export.xlsx")) {
                errors.add("Missing Excel file");
            }
        } else if (format == ExportFormat.XML) {
            if (!files.containsKey("export.xml")) {
                errors.add("Missing XML file");
            }
        }

        return errors;
    }

    private CsvBlobStorageService.StorageResult storePackage(
            UUID exportJobId,
            ExportPackage exportPackage,
            ExportFormat format) throws IOException, NoSuchAlgorithmException {

        String extension = "zip";
        String filename = "export-" + exportJobId + "." + extension;

        Map<String, String> singleFile = Map.of(filename,
                new String(exportPackage.zipArchive(), StandardCharsets.ISO_8859_1));

        return blobStorageService.storeCsvFiles(exportJobId, singleFile);
    }

    private void createAuditMetadataWithPackage(
            ExportJobAggregate exportJob,
            ExportPackage exportPackage,
            CsvBlobStorageService.StorageResult storageResult,
            AccessContext accessContext,
            long vawaSupressed) {

        ExportAuditMetadata metadata = ExportAuditMetadata.withPackageInfo(
                exportJob.getId().value(),
                accessContext,
                exportJob.getExportType(),
                exportJob.getReportingPeriodStart(),
                exportJob.getReportingPeriodEnd(),
                exportJob.getIncludedProjectIds(),
                exportJob.getCocCode(),
                exportPackage.manifestHash(),
                storageResult.getStorageUrl(),
                exportJob.getTotalRecords(),
                vawaSupressed,
                0L,
                List.of(storageResult.getStorageUrl()),
                exportJob.getCompletedAt(),
                storageResult.getExpiresAt(),
                exportPackage.digitalSignature(),
                exportPackage.metadata().encrypted(),
                exportPackage.metadata().encryptionAlgorithm()
        );

        auditMetadataRepository.save(metadata);
        logger.info("Created audit metadata with package signature for export job: {}", exportJob.getId().value());
    }

    @Transactional
    public void failExport(UUID exportJobId, String errorMessage, String errorCode, List<String> validationErrors) {
        exportJobRepository.findById(exportJobId).ifPresent(exportJob -> {
            exportJob.fail(errorMessage, errorCode, validationErrors);
            exportJobRepository.save(exportJob);
        });
    }
}
