package org.haven.api.vsp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.haven.api.config.SecurityUtils;
import org.haven.programenrollment.application.services.VspExportService;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.haven.shared.vo.hmis.VawaRecipientCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for VSP export operations with VAWA compliance
 */
@RestController
@RequestMapping("/api/v1/vsp-exports")
@Tag(name = "VSP Exports", description = "VSP export management with CE-specific anonymization")
@SecurityRequirement(name = "bearer-auth")
public class VspExportController {

    private static final Logger logger = LoggerFactory.getLogger(VspExportController.class);

    private final VspExportService vspExportService;
    private final SecurityUtils securityUtils;

    public VspExportController(VspExportService vspExportService, SecurityUtils securityUtils) {
        this.vspExportService = vspExportService;
        this.securityUtils = securityUtils;
    }

    /**
     * Create a new VSP export with anonymization
     */
    @PostMapping
    @Operation(summary = "Create VSP export", description = "Export data for VSP with CE-specific anonymization")
    @PreAuthorize("hasAnyRole('VSP_EXPORT_ADMIN', 'DATA_EXPORT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<VspExportResponse> createExport(@Valid @RequestBody VspExportRequest request) {
        logger.info("Creating VSP export for recipient: {}", request.recipient());

        try {
            // Validate recipient category
            VawaRecipientCategory category = VawaRecipientCategory.fromOrganizationType(request.recipientType());

            // Build service request
            VspExportService.VspExportRequest serviceRequest = new VspExportService.VspExportRequest(
                UUID.randomUUID(),
                request.recipient(),
                category,
                request.consentBasis(),
                request.cocId(),
                request.enrollmentIds(),
                request.startDate(),
                request.endDate(),
                request.shareScopes(),
                request.exportFormat(),
                request.encryptionKeyId(),
                request.expiryDays(),
                securityUtils.getCurrentUsername(),
                request.includeAssessments(),
                request.includeEvents(),
                request.includeReferrals(),
                request.exportReason(),
                request.additionalRedactions()
            );

            VspExportService.VspExportResult result = vspExportService.exportForVsp(serviceRequest);

            return ResponseEntity.ok(new VspExportResponse(
                result.exportId(),
                result.recipient(),
                result.ceHashKey(),
                result.exportTimestamp().toString(),
                result.expiryDate() != null ? result.expiryDate().toString() : null,
                result.status().name(),
                "Export created successfully"
            ));

        } catch (IllegalArgumentException e) {
            logger.error("Invalid VSP export request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new VspExportResponse(null, request.recipient(), null, null, null,
                    "FAILED", "Invalid request: " + e.getMessage())
            );
        } catch (Exception e) {
            logger.error("Failed to create VSP export", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new VspExportResponse(null, request.recipient(), null, null, null,
                    "ERROR", "Export failed: " + e.getMessage())
            );
        }
    }

    /**
     * Get share history for a recipient
     */
    @GetMapping("/history/{recipient}")
    @Operation(summary = "Get share history", description = "Get export history for a specific recipient")
    @PreAuthorize("hasAnyRole('VSP_EXPORT_ADMIN', 'DATA_EXPORT_MANAGER', 'AUDIT_VIEWER')")
    public ResponseEntity<ShareHistoryResponse> getShareHistory(
            @PathVariable @NotBlank String recipient) {

        logger.info("Retrieving share history for recipient: {}", recipient);

        try {
            VspExportService.RecipientShareHistory history = vspExportService.getShareHistory(recipient);

            return ResponseEntity.ok(ShareHistoryResponse.from(history));

        } catch (Exception e) {
            logger.error("Failed to retrieve share history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Revoke an export
     */
    @PostMapping("/{exportId}/revoke")
    @Operation(summary = "Revoke export", description = "Revoke an active VSP export with reason")
    @PreAuthorize("hasAnyRole('VSP_EXPORT_ADMIN', 'DATA_EXPORT_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<RevokeResponse> revokeExport(
            @PathVariable @NotNull UUID exportId,
            @Valid @RequestBody RevokeRequest request) {

        logger.info("Revoking export {} with reason: {}", exportId, request.reason());

        try {
            vspExportService.revokeExport(
                exportId,
                securityUtils.getCurrentUsername(),
                request.reason()
            );

            return ResponseEntity.ok(new RevokeResponse(
                exportId,
                "REVOKED",
                "Export successfully revoked",
                securityUtils.getCurrentUsername()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                new RevokeResponse(exportId, "FAILED", e.getMessage(), null)
            );
        } catch (Exception e) {
            logger.error("Failed to revoke export", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new RevokeResponse(exportId, "ERROR", "Failed to revoke: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Process expired exports
     */
    @PostMapping("/process-expired")
    @Operation(summary = "Process expired exports", description = "Check and update expired exports")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ProcessResponse> processExpired() {
        logger.info("Processing expired VSP exports");

        try {
            vspExportService.processExpiredExports();

            return ResponseEntity.ok(new ProcessResponse(
                "SUCCESS",
                "Expired exports processed successfully"
            ));

        } catch (Exception e) {
            logger.error("Failed to process expired exports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ProcessResponse("ERROR", "Failed to process: " + e.getMessage())
            );
        }
    }

    // Request and response DTOs
    public record VspExportRequest(
        @NotBlank String recipient,
        @NotBlank String recipientType,
        @NotBlank String consentBasis,
        @NotBlank String cocId,
        @NotNull List<UUID> enrollmentIds,
        LocalDateTime startDate,
        LocalDateTime endDate,
        @NotNull Set<CeShareScope> shareScopes,
        @NotBlank String exportFormat,
        @NotBlank String encryptionKeyId,
        Integer expiryDays,
        boolean includeAssessments,
        boolean includeEvents,
        boolean includeReferrals,
        String exportReason,
        Set<String> additionalRedactions
    ) {}

    public record VspExportResponse(
        UUID exportId,
        String recipient,
        String ceHashKey,
        String exportTimestamp,
        String expiryDate,
        String status,
        String message
    ) {}

    public record ShareHistoryResponse(
        String recipient,
        List<ShareHistoryEntry> exports,
        long totalExports,
        long activeExports,
        long revokedExports,
        long expiredExports,
        String firstExportDate,
        String lastExportDate
    ) {
        public static ShareHistoryResponse from(VspExportService.RecipientShareHistory history) {
            List<ShareHistoryEntry> entries = history.exports().stream()
                .map(ShareHistoryEntry::from)
                .toList();

            return new ShareHistoryResponse(
                history.recipient(),
                entries,
                history.totalExports(),
                history.activeExports(),
                history.revokedExports(),
                history.expiredExports(),
                history.firstExportDate() != null ? history.firstExportDate().toString() : null,
                history.lastExportDate() != null ? history.lastExportDate().toString() : null
            );
        }
    }

    public record ShareHistoryEntry(
        UUID exportId,
        String exportTimestamp,
        String expiryDate,
        String status,
        Set<CeShareScope> shareScopes,
        String consentBasis,
        String ceHashKey,
        String revokedAt,
        String revokedBy,
        String revocationReason
    ) {
        public static ShareHistoryEntry from(VspExportService.ShareHistoryEntry entry) {
            return new ShareHistoryEntry(
                entry.exportId(),
                entry.exportTimestamp().toString(),
                entry.expiryDate() != null ? entry.expiryDate().toString() : null,
                entry.status().name(),
                entry.shareScopes(),
                entry.consentBasis(),
                entry.ceHashKey(),
                entry.revokedAt() != null ? entry.revokedAt().toString() : null,
                entry.revokedBy(),
                entry.revocationReason()
            );
        }
    }

    public record RevokeRequest(
        @NotBlank String reason
    ) {}

    public record RevokeResponse(
        UUID exportId,
        String status,
        String message,
        String revokedBy
    ) {}

    public record ProcessResponse(
        String status,
        String message
    ) {}
}