package org.haven.api.exports;

import org.haven.api.exports.dto.*;
import org.haven.reporting.application.services.ExportJobApplicationService;
import org.haven.reporting.application.services.ExportConfigurationService;
import org.haven.reporting.domain.ExportJobAggregate;
import org.haven.reporting.domain.ExportJobId;
import org.haven.reporting.domain.ExportJobState;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for HUD HMIS export configuration and job management
 * Enforces RBAC for HMIS_LEAD and PROJECT_COORDINATOR roles
 *
 * @deprecated Use ExportJobController in reporting module instead
 */
@Deprecated
@RestController
@RequestMapping("/api/export-legacy")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExportController {

    private final ExportJobApplicationService exportJobService;
    private final ExportConfigurationService configurationService;

    public ExportController(ExportJobApplicationService exportJobService,
                           ExportConfigurationService configurationService) {
        this.exportJobService = exportJobService;
        this.configurationService = configurationService;
    }

    /**
     * Create new export configuration
     * POST /api/exports/configurations
     */
    @PostMapping("/configurations")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<CreateExportConfigurationResponse> createExportConfiguration(
            @Valid @RequestBody CreateExportConfigurationRequest request,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            // Validate configuration against HUD spec and user permissions
            ExportConfigurationService.ValidationResult validation =
                configurationService.validateExportConfiguration(
                    request.getReportType(),
                    request.getOperatingYearStart(),
                    request.getOperatingYearEnd(),
                    request.getProjectIds(),
                    request.isIncludeAggregateOnly(),
                    accessContext
                );

            if (!validation.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CreateExportConfigurationResponse(
                        null,
                        ExportJobState.FAILED.name(),
                        validation.getErrors()
                    ));
            }

            // Queue export job
            UUID exportJobId = exportJobService.requestExport(
                request.getReportType(),
                request.getOperatingYearStart(),
                request.getOperatingYearEnd(),
                request.getProjectIds(),
                request.isIncludeAggregateOnly(),
                accessContext
            );

            // Start async processing
            CompletableFuture<Void> processingFuture =
                exportJobService.processExportAsync(exportJobId, accessContext);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CreateExportConfigurationResponse(
                    exportJobId,
                    ExportJobState.QUEUED.name(),
                    List.of("Export job queued successfully. Use job ID to track progress.")
                ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new CreateExportConfigurationResponse(
                    null,
                    ExportJobState.FAILED.name(),
                    List.of("Access denied: " + e.getMessage())
                ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CreateExportConfigurationResponse(
                    null,
                    ExportJobState.FAILED.name(),
                    List.of("Invalid configuration: " + e.getMessage())
                ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CreateExportConfigurationResponse(
                    null,
                    ExportJobState.FAILED.name(),
                    List.of("Failed to create export: " + e.getMessage())
                ));
        }
    }

    /**
     * Get VAWA consent warnings for export
     * GET /api/exports/{id}/consent-warnings
     * Returns list of client IDs with incomplete VAWA consents blocking individual-level data
     */
    @GetMapping("/{exportJobId}/consent-warnings")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<ConsentWarningsResponse> getConsentWarnings(
            @PathVariable UUID exportJobId,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            // Get consent warnings from configuration service
            List<org.haven.reporting.domain.ConsentWarning> domainWarnings =
                configurationService.getVawaConsentWarnings(exportJobId, accessContext);

            // Convert domain warnings to DTOs
            List<ConsentWarning> warnings = domainWarnings.stream()
                .map(dw -> new ConsentWarning(
                    dw.getClientId(),
                    "XX", // Client initials redacted for VAWA compliance
                    extractWarningType(dw.getWarningMessage()),
                    "CONSENT_REQUIRED",
                    null, // consentDate - not available in domain model
                    null, // consentExpiryDate - not available in domain model
                    true, // blocksIndividualData
                    true, // requiresAggregateOnlyMode
                    dw.getPolicyRule(),
                    dw.getRecommendedAction()
                ))
                .toList();

            // Categorize warnings
            long blockedClients = warnings.stream()
                .filter(w -> w.getBlocksIndividualData())
                .count();

            long requiresAggregateMode = warnings.stream()
                .filter(w -> w.getRequiresAggregateOnlyMode())
                .count();

            return ResponseEntity.ok(new ConsentWarningsResponse(
                warnings,
                blockedClients > 0,
                requiresAggregateMode > 0,
                "Found " + warnings.size() + " client(s) with VAWA consent issues. " +
                "Consider using aggregate-only mode or resolving consent issues."
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extract warning type from warning message
     */
    private String extractWarningType(String warningMessage) {
        if (warningMessage.contains("MISSING_CONSENT")) {
            return "MISSING_CONSENT";
        } else if (warningMessage.contains("CONSENT_REVOKED")) {
            return "CONSENT_REVOKED";
        } else if (warningMessage.contains("CONSENT_EXPIRED")) {
            return "CONSENT_EXPIRED";
        }
        return "UNKNOWN";
    }

    /**
     * Get export job status
     * GET /api/exports/{id}/status
     */
    @GetMapping("/{exportJobId}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST', 'CASE_MANAGER')")
    public ResponseEntity<ExportJobStatusResponse> getExportJobStatus(
            @PathVariable UUID exportJobId,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            ExportJobApplicationService.ExportJobStatus status =
                exportJobService.getExportJobStatus(exportJobId, accessContext);

            return ResponseEntity.ok(new ExportJobStatusResponse(
                status.getExportJobId(),
                status.getState().name(),
                status.getExportType(),
                status.getReportingPeriodStart(),
                status.getReportingPeriodEnd(),
                status.getQueuedAt(),
                status.getStartedAt(),
                status.getCompletedAt(),
                status.getErrorMessage(),
                status.getDownloadUrl(),
                status.getSha256Hash(),
                status.getTotalRecords(),
                status.getVawaSupressedRecords()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all export jobs for current user
     * GET /api/exports
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST', 'CASE_MANAGER')")
    public ResponseEntity<List<ExportJobSummaryResponse>> getAllExportJobs(
            @RequestParam(required = false) String state,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            List<ExportJobApplicationService.ExportJobSummary> jobs =
                exportJobService.getExportJobsForUser(accessContext.getUserId(), state);

            List<ExportJobSummaryResponse> responses = jobs.stream()
                .map(job -> new ExportJobSummaryResponse(
                    job.getExportJobId(),
                    job.getState().name(),
                    job.getExportType(),
                    job.getReportingPeriodStart(),
                    job.getReportingPeriodEnd(),
                    job.getQueuedAt(),
                    job.getCompletedAt(),
                    job.getProjectCount()
                ))
                .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel export job
     * POST /api/exports/{id}/cancel
     */
    @PostMapping("/{exportJobId}/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<ApiResponse> cancelExportJob(
            @PathVariable UUID exportJobId,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            exportJobService.cancelExport(exportJobId, accessContext);
            return ResponseEntity.ok(new ApiResponse("Export job cancelled successfully"));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Access denied: " + e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse("Cannot cancel: " + e.getMessage()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse("Export job not found"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Failed to cancel export: " + e.getMessage()));
        }
    }

    /**
     * Retry failed export job
     * POST /api/exports/{id}/retry
     */
    @PostMapping("/{exportJobId}/retry")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<ApiResponse> retryExportJob(
            @PathVariable UUID exportJobId,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            exportJobService.retryExport(exportJobId, accessContext);
            return ResponseEntity.ok(new ApiResponse("Export job retry initiated"));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse("Access denied: " + e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse("Cannot retry: " + e.getMessage()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse("Export job not found"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Failed to retry export: " + e.getMessage()));
        }
    }

    /**
     * Download export artifact
     * GET /api/exports/{id}/download
     */
    @GetMapping("/{exportJobId}/download")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST')")
    public ResponseEntity<byte[]> downloadExport(
            @PathVariable UUID exportJobId,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            // Get download metadata and enforce access control
            ExportJobApplicationService.DownloadMetadata metadata =
                exportJobService.getDownloadMetadata(exportJobId, accessContext);

            if (!metadata.isAccessGranted()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Return download URL redirect or file content
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", metadata.getDownloadUrl())
                .build();

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get eligible projects for export based on user's data access scope
     * GET /api/exports/eligible-projects
     */
    @GetMapping("/eligible-projects")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR', 'DATA_ANALYST', 'CASE_MANAGER')")
    public ResponseEntity<List<EligibleProjectResponse>> getEligibleProjects(
            @RequestParam(required = false) String reportType,
            @RequestAttribute("accessContext") AccessContext accessContext) {

        try {
            List<ExportConfigurationService.EligibleProject> projects =
                configurationService.getEligibleProjects(reportType, accessContext);

            List<EligibleProjectResponse> responses = projects.stream()
                .map(p -> new EligibleProjectResponse(
                    p.getProjectId(),
                    p.getProjectName(),
                    p.getProjectType(),
                    p.getHudProjectId(),
                    p.isUserHasAccess(),
                    p.getAccessReason()
                ))
                .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
