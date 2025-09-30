package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.haven.programenrollment.application.services.CeEventService;
import org.haven.programenrollment.domain.ce.CeEvent;
import org.haven.programenrollment.domain.ce.CeEventResult;
import org.haven.programenrollment.domain.ce.CeEventStatus;
import org.haven.programenrollment.domain.ce.CeEventType;
import org.haven.programenrollment.domain.ce.CeHashAlgorithm;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/ce-events")
@Tag(name = "Coordinated Entry Events", description = "Consent-scoped CE events and referrals")
public class CeEventController {

    private final CeEventService ceEventService;

    public CeEventController(CeEventService ceEventService) {
        this.ceEventService = ceEventService;
    }

    @Operation(summary = "Record Coordinated Entry event or referral")
    @PostMapping
    public ResponseEntity<CeEventResponse> recordEvent(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId,
        @Valid @RequestBody CreateCeEventRequest request) {

        CeEventService.CreateEventCommand command = new CeEventService.CreateEventCommand(
            enrollmentId,
            request.clientId(),
            request.eventDate(),
            request.eventType(),
            request.result(),
            request.status(),
            request.referralDestination(),
            request.outcomeDate(),
            request.consentId(),
            request.consentLedgerId(),
            toShareScopes(request.shareScopes()),
            request.hashAlgorithm(),
            request.encryptionScheme(),
            request.encryptionKeyId(),
            request.encryptionMetadata(),
            request.encryptionTags(),
            request.createdBy()
        );

        CeEvent event = ceEventService.recordEvent(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CeEventResponse.from(event));
    }

    @Operation(summary = "List Coordinated Entry events for enrollment")
    @GetMapping
    public ResponseEntity<List<CeEventResponse>> listEvents(
        @Parameter(description = "Program enrollment identifier")
        @PathVariable UUID enrollmentId) {

        List<CeEventResponse> responses = ceEventService.getEventsForEnrollment(enrollmentId)
            .stream()
            .map(CeEventResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private Set<CeShareScope> toShareScopes(Set<CeShareScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return EnumSet.noneOf(CeShareScope.class);
        }
        return EnumSet.copyOf(scopes);
    }

    public record CreateCeEventRequest(
        @NotNull UUID clientId,
        @NotNull LocalDate eventDate,
        @NotNull CeEventType eventType,
        CeEventResult result,
        @NotNull CeEventStatus status,
        String referralDestination,
        LocalDate outcomeDate,
        @NotNull UUID consentId,
        UUID consentLedgerId,
        Set<CeShareScope> shareScopes,
        CeHashAlgorithm hashAlgorithm,
        String encryptionScheme,
        @NotBlank String encryptionKeyId,
        Map<String, String> encryptionMetadata,
        List<String> encryptionTags,
        @NotBlank String createdBy
    ) {}

    public record CeEventResponse(
        UUID id,
        UUID enrollmentId,
        UUID clientId,
        LocalDate eventDate,
        CeEventType eventType,
        CeEventResult result,
        CeEventStatus status,
        String referralDestination,
        LocalDate outcomeDate,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        UUID packetId,
        UUID consentLedgerId,
        Set<CeShareScope> consentScope
    ) {
        static CeEventResponse from(CeEvent event) {
            return new CeEventResponse(
                event.getRecordId(),
                event.getEnrollmentId().value(),
                event.getClientId().value(),
                event.getEventDate(),
                event.getEventType(),
                event.getResult(),
                event.getStatus(),
                event.getReferralDestination(),
                event.getOutcomeDate(),
                event.getCreatedBy(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getPacketId() != null ? event.getPacketId().value() : null,
                event.getConsentLedgerId(),
                Set.copyOf(event.getConsentScope())
            );
        }
    }
}
