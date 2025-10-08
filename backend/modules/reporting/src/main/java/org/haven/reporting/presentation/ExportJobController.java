package org.haven.reporting.presentation;

import org.haven.reporting.application.services.ExportJobApplicationService;
import org.haven.reporting.application.services.ExportSecurityPolicyService;
import org.haven.reporting.domain.*;
import org.haven.reporting.infrastructure.persistence.ExportAuditMetadataRepository;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.PolicyDecision;
import org.haven.shared.security.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for HUD export jobs
 *
 * Endpoints:
 * - POST /api/exports - Request new export
 * - GET /api/exports/{exportJobId} - Get export status
 * - GET /api/exports/{exportJobId}/audit - Get audit metadata
 * - GET /api/exports - List user's exports
 */
@RestController
@RequestMapping("/api/exports")
public class ExportJobController {

    private final ExportJobApplicationService exportService;
    private final ExportJobRepository exportJobRepository;
    private final ExportAuditMetadataRepository auditMetadataRepository;
    private final ExportSecurityPolicyService securityPolicyService;

    public ExportJobController(
            ExportJobApplicationService exportService,
            ExportJobRepository exportJobRepository,
            ExportAuditMetadataRepository auditMetadataRepository,
            ExportSecurityPolicyService securityPolicyService) {
        this.exportService = exportService;
        this.exportJobRepository = exportJobRepository;
        this.auditMetadataRepository = auditMetadataRepository;
        this.securityPolicyService = securityPolicyService;
    }

    /**
     * Request a new HUD export with hash policy enforcement
     * Requires ADMINISTRATOR, SUPERVISOR, or DATA_ANALYST role
     *
     * Fail-fast validation:
     * 1. Validate consent scopes if unhashed export requested
     * 2. Verify security clearance validity
     * 3. Evaluate tenant hash policy
     * 4. Audit all attempts (permit and deny)
     * 5. Reject immediately if policy violation
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<?> requestExport(
            @RequestBody ExportJobRequest request,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        // Extract tenant ID from access context or request
        // TODO: Get from proper tenant resolution service
        UUID tenantId = accessContext.getUserId(); // Placeholder

        // Evaluate hash policy - fail fast if unhashed requested without authorization
        PolicyDecision policyDecision = securityPolicyService.evaluateExportHashPolicy(
                tenantId,
                request.isHashedExport(),
                request.getConsentScopes(),
                request.getClearance(),
                accessContext
        );

        // If policy denies, return 403 with detailed reason
        if (!policyDecision.isPermitted()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ExportPolicyViolationResponse(
                            "EXPORT_POLICY_VIOLATION",
                            policyDecision.getReason(),
                            policyDecision.getMetadata()
                    ));
        }

        // Policy permits - proceed with export
        UUID exportJobId = exportService.requestExport(
                request.getExportType(),
                request.getReportingPeriodStart(),
                request.getReportingPeriodEnd(),
                request.getProjectIds(),
                false, // includeAggregateOnly - can derive from request if needed
                accessContext
        );

        ExportJobResponse response = new ExportJobResponse(
                exportJobId,
                ExportJobState.QUEUED,
                "Export job queued for processing",
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get export job status
     */
    @GetMapping("/{exportJobId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<ExportJobResponse> getExportStatus(
            @PathVariable UUID exportJobId,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        Optional<ExportJobAggregate> exportJobOpt = exportJobRepository.findById(exportJobId);

        if (exportJobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ExportJobAggregate exportJob = exportJobOpt.get();

        // Check if user has access to this export
        // (Only requestor or admins can view)
        if (!canAccessExport(exportJob, accessContext)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ExportJobResponse response = new ExportJobResponse(
                exportJobId,
                exportJob.getState(),
                getStatusMessage(exportJob),
                exportJob.getBlobStorageUrl(),
                exportJob.getErrorMessage()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get export audit metadata
     */
    @GetMapping("/{exportJobId}/audit")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'AUDITOR')")
    public ResponseEntity<ExportAuditMetadata> getExportAudit(
            @PathVariable UUID exportJobId) {

        Optional<ExportAuditMetadata> audit = auditMetadataRepository.findByExportJobId(exportJobId);

        return audit.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * List user's export jobs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<List<ExportAuditMetadata>> listUserExports(
            @RequestAttribute("accessContext") AccessContext accessContext) {

        List<ExportAuditMetadata> exports = auditMetadataRepository
                .findByRequestedByUserIdOrderByGeneratedAtDesc(accessContext.getUserId());

        return ResponseEntity.ok(exports);
    }

    private boolean canAccessExport(ExportJobAggregate exportJob, AccessContext accessContext) {
        // Allow if user is the requestor
        if (exportJob.getRequestedByUserId().equals(accessContext.getUserId().toString())) {
            return true;
        }

        // Allow if user is admin or supervisor
        return accessContext.hasAnyRole(UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
    }

    private String getStatusMessage(ExportJobAggregate exportJob) {
        switch (exportJob.getState()) {
            case QUEUED:
                return "Export queued for processing";
            case MATERIALIZING:
                return "Generating CSV views with VAWA filtering";
            case VALIDATING:
                return "Validating HUD compliance and data quality";
            case COMPLETE:
                return "Export completed successfully. Total records: " + exportJob.getTotalRecords();
            case FAILED:
                return "Export failed: " + exportJob.getErrorMessage();
            default:
                return "Unknown state";
        }
    }

    // DTOs

    public static class ExportJobRequest {
        private String exportType;
        private LocalDate reportingPeriodStart;
        private LocalDate reportingPeriodEnd;
        private List<UUID> projectIds;
        private String cocCode;
        private String exportReason;
        private boolean hashedExport = true; // Default to hashed (secure)
        private Set<ExportConsentScope> consentScopes;
        private ExportSecurityClearance clearance;

        // Getters and setters
        public String getExportType() {
            return exportType;
        }

        public void setExportType(String exportType) {
            this.exportType = exportType;
        }

        public LocalDate getReportingPeriodStart() {
            return reportingPeriodStart;
        }

        public void setReportingPeriodStart(LocalDate reportingPeriodStart) {
            this.reportingPeriodStart = reportingPeriodStart;
        }

        public LocalDate getReportingPeriodEnd() {
            return reportingPeriodEnd;
        }

        public void setReportingPeriodEnd(LocalDate reportingPeriodEnd) {
            this.reportingPeriodEnd = reportingPeriodEnd;
        }

        public List<UUID> getProjectIds() {
            return projectIds;
        }

        public void setProjectIds(List<UUID> projectIds) {
            this.projectIds = projectIds;
        }

        public String getCocCode() {
            return cocCode;
        }

        public void setCocCode(String cocCode) {
            this.cocCode = cocCode;
        }

        public String getExportReason() {
            return exportReason;
        }

        public void setExportReason(String exportReason) {
            this.exportReason = exportReason;
        }

        public boolean isHashedExport() {
            return hashedExport;
        }

        public void setHashedExport(boolean hashedExport) {
            this.hashedExport = hashedExport;
        }

        public Set<ExportConsentScope> getConsentScopes() {
            return consentScopes;
        }

        public void setConsentScopes(Set<ExportConsentScope> consentScopes) {
            this.consentScopes = consentScopes;
        }

        public ExportSecurityClearance getClearance() {
            return clearance;
        }

        public void setClearance(ExportSecurityClearance clearance) {
            this.clearance = clearance;
        }
    }

    public static class ExportJobResponse {
        private UUID exportJobId;
        private ExportJobState state;
        private String message;
        private String downloadUrl;
        private String errorMessage;

        public ExportJobResponse(UUID exportJobId, ExportJobState state, String message,
                               String downloadUrl, String errorMessage) {
            this.exportJobId = exportJobId;
            this.state = state;
            this.message = message;
            this.downloadUrl = downloadUrl;
            this.errorMessage = errorMessage;
        }

        // Getters and setters
        public UUID getExportJobId() {
            return exportJobId;
        }

        public void setExportJobId(UUID exportJobId) {
            this.exportJobId = exportJobId;
        }

        public ExportJobState getState() {
            return state;
        }

        public void setState(ExportJobState state) {
            this.state = state;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Response for export policy violations
     * Provides detailed feedback on why unhashed export was rejected
     */
    public static class ExportPolicyViolationResponse {
        private String errorCode;
        private String reason;
        private Object metadata;

        public ExportPolicyViolationResponse(String errorCode, String reason, Object metadata) {
            this.errorCode = errorCode;
            this.reason = reason;
            this.metadata = metadata;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }
    }
}
