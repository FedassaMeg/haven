package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.haven.programenrollment.application.services.CeReferralService;
import org.haven.programenrollment.domain.ce.*;
import org.haven.shared.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/ce-referrals")
@Tag(name = "Coordinated Entry Referrals", description = "Consent-aware CE referral management")
public class CeReferralController {

    private final CeReferralService ceReferralService;
    private final AuditService auditService;

    public CeReferralController(CeReferralService ceReferralService, AuditService auditService) {
        this.ceReferralService = ceReferralService;
        this.auditService = auditService;
    }

    @Operation(summary = "Create CE referral with consent verification")
    @PostMapping
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CE_COORDINATOR')")
    public ResponseEntity<CeReferralResponse> createReferral(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Valid @RequestBody CreateCeReferralRequest request) {

        // Audit the referral creation attempt
        auditService.logAction("CE_REFERRAL_CREATED", Map.of(
            "enrollmentId", enrollmentId,
            "projectId", request.referredProjectId(),
            "consentLedgerId", request.consentLedgerId()
        ));

        CeReferralService.CreateReferralCommand command = new CeReferralService.CreateReferralCommand(
            enrollmentId,
            request.clientId(),
            request.referralDate(),
            request.referredProjectId(),
            request.referredProjectName(),
            request.referredOrganization(),
            request.referralType(),
            request.priorityLevel(),
            request.expirationDate(),
            request.consentId(),
            request.consentLedgerId(),
            request.shareScopes(),
            request.hashAlgorithm(),
            request.encryptionScheme(),
            request.encryptionKeyId(),
            request.encryptionMetadata(),
            request.encryptionTags(),
            request.vawaProtection(),
            request.createdBy(),
            request.caseManagerName(),
            request.caseManagerContact(),
            request.vulnerabilityScore(),
            request.notes()
        );

        CeReferral referral = ceReferralService.createReferral(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CeReferralResponse.from(referral));
    }

    @Operation(summary = "Update CE referral result")
    @PutMapping("/{referralId}/result")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CE_COORDINATOR') or hasRole('HOUSING_PROVIDER')")
    public ResponseEntity<CeReferralResponse> updateReferralResult(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Parameter(description = "Referral identifier")
        @PathVariable UUID referralId,
        @Valid @RequestBody UpdateReferralResultRequest request) {

        // Audit the referral result update
        auditService.logAction("CE_REFERRAL_RESULT_UPDATED", Map.of(
            "enrollmentId", enrollmentId,
            "referralId", referralId,
            "result", request.referralResult()
        ));

        CeReferralService.UpdateReferralResultCommand command = new CeReferralService.UpdateReferralResultCommand(
            referralId,
            request.referralResult(),
            request.resultDate(),
            request.rejectionReason(),
            request.rejectionNotes(),
            request.acceptedDate(),
            request.housingMoveInDate(),
            request.updatedBy()
        );

        CeReferral referral = ceReferralService.updateReferralResult(enrollmentId, command);
        return ResponseEntity.ok(CeReferralResponse.from(referral));
    }

    @Operation(summary = "List CE referrals for enrollment with consent filtering")
    @GetMapping
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CE_COORDINATOR')")
    public ResponseEntity<List<CeReferralResponse>> listReferrals(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Parameter(description = "Include expired referrals")
        @RequestParam(defaultValue = "false") boolean includeExpired,
        @Parameter(description = "Filter by referral status")
        @RequestParam(required = false) CeReferralStatus status) {

        List<CeReferralResponse> responses = ceReferralService.getReferralsForEnrollment(
                enrollmentId, includeExpired, status)
            .stream()
            .map(CeReferralResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get referral details with consent verification")
    @GetMapping("/{referralId}")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CE_COORDINATOR') or hasRole('HOUSING_PROVIDER')")
    public ResponseEntity<CeReferralResponse> getReferral(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Parameter(description = "Referral identifier")
        @PathVariable UUID referralId) {

        // Audit the referral access
        auditService.logAction("CE_REFERRAL_ACCESSED", Map.of(
            "enrollmentId", enrollmentId,
            "referralId", referralId
        ));

        CeReferral referral = ceReferralService.getReferral(enrollmentId, referralId);
        return ResponseEntity.ok(CeReferralResponse.from(referral));
    }

    @Operation(summary = "Cancel CE referral")
    @DeleteMapping("/{referralId}")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CE_COORDINATOR')")
    public ResponseEntity<Void> cancelReferral(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Parameter(description = "Referral identifier")
        @PathVariable UUID referralId,
        @RequestParam @NotBlank String reason) {

        // Audit the referral cancellation
        auditService.logAction("CE_REFERRAL_CANCELLED", Map.of(
            "enrollmentId", enrollmentId,
            "referralId", referralId,
            "reason", reason
        ));

        ceReferralService.cancelReferral(enrollmentId, referralId, reason);
        return ResponseEntity.noContent().build();
    }

    public record CreateCeReferralRequest(
        @NotNull UUID clientId,
        @NotNull LocalDateTime referralDate,
        @NotNull UUID referredProjectId,
        @NotBlank String referredProjectName,
        @NotBlank String referredOrganization,
        @NotNull CeEventType referralType,
        Integer priorityLevel,
        LocalDate expirationDate,
        @NotNull UUID consentId,
        UUID consentLedgerId,
        Set<CeShareScope> shareScopes,
        CeHashAlgorithm hashAlgorithm,
        String encryptionScheme,
        @NotBlank String encryptionKeyId,
        Map<String, String> encryptionMetadata,
        List<String> encryptionTags,
        boolean vawaProtection,
        @NotBlank String createdBy,
        String caseManagerName,
        String caseManagerContact,
        Double vulnerabilityScore,
        String notes
    ) {}

    public record UpdateReferralResultRequest(
        @NotNull CeReferralResult referralResult,
        @NotNull LocalDateTime resultDate,
        String rejectionReason,
        String rejectionNotes,
        LocalDate acceptedDate,
        LocalDate housingMoveInDate,
        @NotBlank String updatedBy
    ) {}

    public record CeReferralResponse(
        UUID id,
        UUID enrollmentId,
        UUID clientId,
        LocalDateTime referralDate,
        UUID referredProjectId,
        String referredProjectName,
        String referredOrganization,
        CeEventType referralType,
        CeReferralStatus status,
        CeReferralResult result,
        LocalDate expirationDate,
        Integer priorityLevel,
        Double vulnerabilityScore,
        String caseManagerName,
        String caseManagerContact,
        boolean vawaProtection,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        UUID packetId,
        UUID consentLedgerId,
        Set<CeShareScope> consentScope
    ) {
        static CeReferralResponse from(CeReferral referral) {
            return new CeReferralResponse(
                referral.getReferralId(),
                referral.getEnrollmentId().value(),
                referral.getClientId().value(),
                referral.getReferralDate(),
                referral.getReferredProjectId(),
                referral.getReferredProjectName(),
                referral.getReferredOrganization(),
                referral.getReferralType(),
                referral.getStatus(),
                referral.getResult(),
                referral.getExpirationDate(),
                referral.getPriorityLevel(),
                referral.getVulnerabilityScore(),
                referral.getCaseManagerName(),
                referral.getCaseManagerContact(),
                referral.isVawaProtection(),
                referral.getCreatedBy(),
                referral.getCreatedAt(),
                referral.getUpdatedAt(),
                referral.getPacketId() != null ? referral.getPacketId().value() : null,
                referral.getConsentLedgerId(),
                Set.copyOf(referral.getConsentScope())
            );
        }
    }
}