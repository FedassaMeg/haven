package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.haven.programenrollment.application.services.CeAssessmentService;
import org.haven.programenrollment.domain.ce.CeAssessment;
import org.haven.programenrollment.domain.ce.CeAssessmentLevel;
import org.haven.programenrollment.domain.ce.CeAssessmentType;
import org.haven.programenrollment.domain.ce.CeHashAlgorithm;
import org.haven.programenrollment.domain.ce.CePrioritizationStatus;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/ce-assessments")
@Tag(name = "Coordinated Entry Assessments", description = "Consent-aware CE assessment capture")
public class CeAssessmentController {

    private final CeAssessmentService ceAssessmentService;

    public CeAssessmentController(CeAssessmentService ceAssessmentService) {
        this.ceAssessmentService = ceAssessmentService;
    }

    @Operation(summary = "Record Coordinated Entry assessment")
    @PostMapping
    public ResponseEntity<CeAssessmentResponse> recordAssessment(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Valid @RequestBody CreateCeAssessmentRequest request) {

        CeAssessmentService.CreateAssessmentCommand command = new CeAssessmentService.CreateAssessmentCommand(
            enrollmentId,
            request.clientId(),
            request.assessmentDate(),
            request.assessmentType(),
            request.assessmentLevel(),
            request.toolUsed(),
            request.score(),
            request.prioritizationStatus(),
            request.location(),
            request.consentId(),
            request.consentLedgerId(),
            toShareScopes(request.shareScopes()),
            request.hashAlgorithm(),
            request.encryptionScheme(),
            request.encryptionKeyId(),
            request.encryptionMetadata(),
            request.encryptionTags(),
            request.createdBy(),
            request.recipientOrganization()
        );

        CeAssessment assessment = ceAssessmentService.recordAssessment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CeAssessmentResponse.from(assessment));
    }

    @Operation(summary = "List Coordinated Entry assessments for enrollment")
    @GetMapping
    public ResponseEntity<List<CeAssessmentResponse>> listAssessments(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId) {

        List<CeAssessmentResponse> responses = ceAssessmentService.getAssessmentsForEnrollment(enrollmentId)
            .stream()
            .map(CeAssessmentResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private Set<CeShareScope> toShareScopes(Set<CeShareScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return EnumSet.noneOf(CeShareScope.class);
        }
        return EnumSet.copyOf(scopes);
    }

    public record CreateCeAssessmentRequest(
        @NotNull UUID clientId,
        @NotNull LocalDate assessmentDate,
        @NotNull CeAssessmentType assessmentType,
        CeAssessmentLevel assessmentLevel,
        String toolUsed,
        BigDecimal score,
        CePrioritizationStatus prioritizationStatus,
        String location,
        @NotNull UUID consentId,
        UUID consentLedgerId,
        Set<CeShareScope> shareScopes,
        CeHashAlgorithm hashAlgorithm,
        String encryptionScheme,
        @NotBlank String encryptionKeyId,
        Map<String, String> encryptionMetadata,
        List<String> encryptionTags,
        @NotBlank String createdBy,
        String recipientOrganization
    ) {}

    public record CeAssessmentResponse(
        UUID id,
        UUID enrollmentId,
        UUID clientId,
        LocalDate assessmentDate,
        CeAssessmentType assessmentType,
        CeAssessmentLevel assessmentLevel,
        String toolUsed,
        BigDecimal score,
        CePrioritizationStatus prioritizationStatus,
        String location,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        UUID packetId,
        UUID consentLedgerId,
        Set<CeShareScope> consentScope
    ) {
        static CeAssessmentResponse from(CeAssessment assessment) {
            return new CeAssessmentResponse(
                assessment.getRecordId(),
                assessment.getEnrollmentId().value(),
                assessment.getClientId().value(),
                assessment.getAssessmentDate(),
                assessment.getAssessmentType(),
                assessment.getAssessmentLevel(),
                assessment.getToolUsed(),
                assessment.getScore(),
                assessment.getPrioritizationStatus(),
                assessment.getLocation(),
                assessment.getCreatedBy(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt(),
                assessment.getPacketId() != null ? assessment.getPacketId().value() : null,
                assessment.getConsentLedgerId(),
                Set.copyOf(assessment.getConsentScope())
            );
        }
    }
}
