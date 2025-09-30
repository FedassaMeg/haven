package org.haven.programenrollment.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.*;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerEntity;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerRepository;
import org.haven.clientprofile.infrastructure.security.ConsentEnforcementService;
import org.haven.programenrollment.application.security.HmisAuditLogger;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.programenrollment.domain.ce.*;
import org.haven.programenrollment.infrastructure.persistence.JpaCeEventEntity;
import org.haven.programenrollment.infrastructure.persistence.JpaCeEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CeEventService {

    private final ProgramEnrollmentRepository enrollmentRepository;
    private final JpaCeEventRepository eventRepository;
    private final ConsentRepository consentRepository;
    private final ConsentLedgerRepository consentLedgerRepository;
    private final CePacketService cePacketService;
    private final ConsentEnforcementService consentEnforcementService;
    private final HmisAuditLogger auditLogger;

    public CeEventService(ProgramEnrollmentRepository enrollmentRepository,
                          JpaCeEventRepository eventRepository,
                          ConsentRepository consentRepository,
                          ConsentLedgerRepository consentLedgerRepository,
                          CePacketService cePacketService,
                          ConsentEnforcementService consentEnforcementService,
                          HmisAuditLogger auditLogger) {
        this.enrollmentRepository = enrollmentRepository;
        this.eventRepository = eventRepository;
        this.consentRepository = consentRepository;
        this.consentLedgerRepository = consentLedgerRepository;
        this.cePacketService = cePacketService;
        this.consentEnforcementService = consentEnforcementService;
        this.auditLogger = auditLogger;
    }

    public CeEvent recordEvent(CreateEventCommand command) {
        Objects.requireNonNull(command, "command is required");

        ProgramEnrollment enrollment = enrollmentRepository.findById(ProgramEnrollmentId.of(command.enrollmentId()))
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + command.enrollmentId()));

        ClientId enrollmentClient = enrollment.getClientId();
        UUID clientUuid = command.clientId() != null ? command.clientId() : enrollmentClient.value();
        if (!enrollmentClient.value().equals(clientUuid)) {
            throw new IllegalArgumentException("Client does not match enrollment");
        }
        ClientId clientId = new ClientId(clientUuid);

        Consent consent = loadConsent(command.consentId());
        validateConsentOwnership(consent, clientId);
        validateConsentScope(consent, command.shareScopes());

        ConsentLedgerEntity ledgerEntry = null;
        if (command.consentLedgerId() != null) {
            ledgerEntry = consentLedgerRepository.findById(command.consentLedgerId())
                .orElseThrow(() -> new IllegalArgumentException("Consent ledger entry not found: " + command.consentLedgerId()));
            if (!ledgerEntry.getClientId().equals(clientUuid)) {
                throw new IllegalArgumentException("Ledger entry does not belong to client");
            }
            if (!ledgerEntry.getStatus().equals(ConsentStatus.GRANTED)) {
                throw new IllegalStateException("Ledger entry is not active for sharing");
            }
        }

        enforceConsent(clientId, command);

        CePacket packet = cePacketService.createOrRefreshPacket(
            enrollment.getId(),
            clientId,
            consent,
            command.shareScopes(),
            command.hashAlgorithm(),
            command.encryptionScheme(),
            requireEncryptionKey(command.encryptionKeyId()),
            augmentMetadata(command.encryptionMetadata(), command.consentLedgerId()),
            sanitizeTags(command.encryptionTags()),
            command.consentLedgerId()
        );

        CeEvent ceEvent = CeEvent.create(
            enrollment.getId(),
            clientId,
            command.eventDate(),
            command.eventType(),
            command.result(),
            command.status(),
            command.referralDestination(),
            command.outcomeDate(),
            command.createdBy(),
            packet.getPacketId(),
            command.consentLedgerId(),
            command.shareScopes()
        );

        eventRepository.save(new JpaCeEventEntity(ceEvent));

        auditLogger.logDataModification(
            "CE_EVENT",
            ceEvent.getRecordId(),
            "CREATE",
            command.createdBy(),
            null,
            String.format("eventType=%s, consent=%s, ledger=%s, packet=%s",
                command.eventType(),
                command.consentId(),
                command.consentLedgerId(),
                packet.getPacketId())
        );

        return ceEvent;
    }

    @Transactional(readOnly = true)
    public List<CeEvent> getEventsForEnrollment(UUID enrollmentId) {
        return eventRepository.findByEnrollmentIdOrderByEventDateDesc(enrollmentId)
            .stream()
            .map(JpaCeEventEntity::toDomain)
            .collect(Collectors.toList());
    }

    private Consent loadConsent(UUID consentId) {
        if (consentId == null) {
            throw new IllegalArgumentException("consentId is required");
        }
        return consentRepository.findById(ConsentId.fromString(consentId.toString()))
            .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));
    }

    private void validateConsentOwnership(Consent consent, ClientId expectedClient) {
        if (!consent.getClientId().equals(expectedClient)) {
            throw new IllegalArgumentException("Consent does not belong to client");
        }
        if (!consent.isValidForUse()) {
            throw new IllegalStateException("Consent is not active for use");
        }
    }

    private void validateConsentScope(Consent consent, Set<CeShareScope> scopes) {
        if (scopes == null) {
            return;
        }
        for (CeShareScope scope : scopes) {
            if (scope.requiresVawaClearance() && !consent.isVAWAProtected()) {
                throw new IllegalStateException("Scope " + scope + " requires VAWA-protected consent");
            }
        }
    }

    private void enforceConsent(ClientId clientId, CreateEventCommand command) {
        List<ConsentType> requirements = new ArrayList<>();
        requirements.add(ConsentType.INFORMATION_SHARING);
        requirements.add(ConsentType.HMIS_PARTICIPATION);
        if (command.eventType() != null && command.eventType().isReferral()) {
            requirements.add(ConsentType.REFERRAL_SHARING);
        }
        consentEnforcementService.validateOperation(
            clientId,
            command.eventType() != null && command.eventType().isReferral() ? "ce-referral:create" : "ce-event:create",
            command.referralDestination() != null ? command.referralDestination() : "CE",
            requirements.toArray(ConsentType[]::new)
        ).throwIfDenied();
    }

    private String requireEncryptionKey(String encryptionKeyId) {
        if (encryptionKeyId == null || encryptionKeyId.isBlank()) {
            throw new IllegalArgumentException("encryptionKeyId is required");
        }
        return encryptionKeyId;
    }

    private Map<String, String> augmentMetadata(Map<String, String> metadata, UUID consentLedgerId) {
        Map<String, String> augmented = new HashMap<>();
        if (metadata != null) {
            augmented.putAll(metadata);
        }
        if (consentLedgerId != null) {
            augmented.put("consentLedgerId", consentLedgerId.toString());
        }
        return Map.copyOf(augmented);
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.collectingAndThen(Collectors.toList(), List::copyOf));
    }

    public record CreateEventCommand(
        UUID enrollmentId,
        UUID clientId,
        LocalDate eventDate,
        CeEventType eventType,
        CeEventResult result,
        CeEventStatus status,
        String referralDestination,
        LocalDate outcomeDate,
        UUID consentId,
        UUID consentLedgerId,
        Set<CeShareScope> shareScopes,
        CeHashAlgorithm hashAlgorithm,
        String encryptionScheme,
        String encryptionKeyId,
        Map<String, String> encryptionMetadata,
        List<String> encryptionTags,
        String createdBy
    ) {}
}
