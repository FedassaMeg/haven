package org.haven.programenrollment.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.Consent;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.clientprofile.domain.consent.ConsentRepository;
import org.haven.programenrollment.application.security.CePacketCryptoService;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.*;
import org.haven.shared.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CeReferralService {

    private static final Logger logger = LoggerFactory.getLogger(CeReferralService.class);

    private final CeReferralRepository ceReferralRepository;
    private final CePacketRepository cePacketRepository;
    private final CePacketCryptoService cePacketCryptoService;
    private final ConsentRepository consentRepository;
    private final ConsentLedgerUpdatePublisher consentLedgerUpdatePublisher;
    private final AuditService auditService;

    public CeReferralService(
            CeReferralRepository ceReferralRepository,
            CePacketRepository cePacketRepository,
            CePacketCryptoService cePacketCryptoService,
            ConsentRepository consentRepository,
            ConsentLedgerUpdatePublisher consentLedgerUpdatePublisher,
            AuditService auditService) {
        this.ceReferralRepository = ceReferralRepository;
        this.cePacketRepository = cePacketRepository;
        this.cePacketCryptoService = cePacketCryptoService;
        this.consentRepository = consentRepository;
        this.consentLedgerUpdatePublisher = consentLedgerUpdatePublisher;
        this.auditService = auditService;
    }

    public CeReferral createReferral(CreateReferralCommand command) {
        logger.info("Creating CE referral for enrollment: {}", command.enrollmentId);

        // Verify consent
        Consent consent = consentRepository.findById(new ConsentId(command.consentId))
            .orElseThrow(() -> new IllegalArgumentException("Consent not found"));

        if (!consent.isActive()) {
            throw new IllegalStateException("Cannot create referral with inactive consent");
        }

        // Create or retrieve CE packet
        CePacket packet = createOrRetrievePacket(command, consent);

        // Validate share scopes against consent
        Set<CeShareScope> allowedScopes = validateShareScopes(command.shareScopes, packet);

        // Create referral
        CeReferral referral = CeReferral.builder()
            .enrollmentId(new ProgramEnrollmentId(command.enrollmentId))
            .clientId(new ClientId(command.clientId))
            .referralDate(command.referralDate)
            .referredProjectId(command.referredProjectId)
            .referredProjectName(command.referredProjectName)
            .referredOrganization(command.referredOrganization)
            .referralType(command.referralType)
            .priorityLevel(command.priorityLevel)
            .expirationDate(command.expirationDate)
            .packetId(packet.getPacketId())
            .consentLedgerId(command.consentLedgerId)
            .consentScope(allowedScopes)
            .vulnerabilityScore(command.vulnerabilityScore)
            .caseManagerName(command.caseManagerName)
            .caseManagerContact(command.caseManagerContact)
            .vawaProtection(command.vawaProtection)
            .createdBy(command.createdBy)
            .build();

        referral = ceReferralRepository.save(referral);

        // Write to consent ledger
        writeLedgerEntry(referral, "CE_REFERRAL_CREATED");

        // Audit trail
        auditService.logAction("CE_REFERRAL_CREATED", Map.of(
            "referralId", referral.getReferralId(),
            "enrollmentId", command.enrollmentId,
            "projectId", command.referredProjectId,
            "consentLedgerId", command.consentLedgerId != null ? command.consentLedgerId : "N/A"
        ));

        return referral;
    }

    public CeReferral updateReferralResult(UUID enrollmentId, UpdateReferralResultCommand command) {
        logger.info("Updating CE referral result: {}", command.referralId);

        CeReferral referral = ceReferralRepository.findById(command.referralId)
            .orElseThrow(() -> new IllegalArgumentException("Referral not found"));

        if (!referral.getEnrollmentId().value().equals(enrollmentId)) {
            throw new IllegalArgumentException("Referral does not belong to enrollment");
        }

        referral = referral.updateResult(
            command.referralResult,
            command.resultDate,
            command.rejectionReason,
            command.rejectionNotes,
            command.acceptedDate,
            command.housingMoveInDate
        );

        referral = ceReferralRepository.save(referral);

        // Write to consent ledger
        writeLedgerEntry(referral, "CE_REFERRAL_RESULT_UPDATED");

        // Audit trail
        auditService.logAction("CE_REFERRAL_RESULT_UPDATED", Map.of(
            "referralId", command.referralId,
            "result", command.referralResult,
            "updatedBy", command.updatedBy
        ));

        return referral;
    }

    public void cancelReferral(UUID enrollmentId, UUID referralId, String reason) {
        logger.info("Cancelling CE referral: {}", referralId);

        CeReferral referral = ceReferralRepository.findById(referralId)
            .orElseThrow(() -> new IllegalArgumentException("Referral not found"));

        if (!referral.getEnrollmentId().value().equals(enrollmentId)) {
            throw new IllegalArgumentException("Referral does not belong to enrollment");
        }

        referral = referral.cancel(reason);
        ceReferralRepository.save(referral);

        // Write to consent ledger
        writeLedgerEntry(referral, "CE_REFERRAL_CANCELLED");

        // Audit trail
        auditService.logAction("CE_REFERRAL_CANCELLED", Map.of(
            "referralId", referralId,
            "reason", reason
        ));
    }

    public CeReferral getReferral(UUID enrollmentId, UUID referralId) {
        CeReferral referral = ceReferralRepository.findById(referralId)
            .orElseThrow(() -> new IllegalArgumentException("Referral not found"));

        if (!referral.getEnrollmentId().value().equals(enrollmentId)) {
            throw new IllegalArgumentException("Referral does not belong to enrollment");
        }

        // Audit access
        auditService.logAction("CE_REFERRAL_ACCESSED", Map.of(
            "referralId", referralId
        ));

        return referral;
    }

    public List<CeReferral> getReferralsForEnrollment(UUID enrollmentId,
                                                      boolean includeExpired,
                                                      CeReferralStatus status) {
        List<CeReferral> referrals = ceReferralRepository
            .findByEnrollmentId(new ProgramEnrollmentId(enrollmentId));

        // Filter by status if provided
        if (status != null) {
            referrals = referrals.stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
        }

        // Filter expired if requested
        if (!includeExpired) {
            referrals = referrals.stream()
                .filter(r -> !r.isExpired())
                .collect(Collectors.toList());
        }

        return referrals;
    }

    private CePacket createOrRetrievePacket(CreateReferralCommand command, Consent consent) {
        // Check if packet already exists for this enrollment and consent
        Optional<CePacket> existingPacket = cePacketRepository
            .findActiveByConsent(consent.getConsentId())
            .filter(packet -> packet.getEnrollmentId() != null &&
                packet.getEnrollmentId().value().equals(command.enrollmentId));

        if (existingPacket.isPresent()) {
            return existingPacket.get();
        }

        // Create new packet with hashed client identifier
        String clientHash = cePacketCryptoService.hashClientId(
            command.clientId.toString(),
            command.hashAlgorithm != null ? command.hashAlgorithm : CeHashAlgorithm.SHA256_SALT
        );

        CePacket packet = CePacket.builder()
            .clientId(new ClientId(command.clientId))
            .enrollmentId(new ProgramEnrollmentId(command.enrollmentId))
            .consentId(consent.getConsentId())
            .consentStatus(consent.getStatus())
            .consentVersion(consent.getVersion())
            .consentEffectiveAt(consent.getEffectiveDate())
            .consentExpiresAt(consent.getExpirationDate())
            .clientHash(clientHash)
            .hashAlgorithm(command.hashAlgorithm != null ? command.hashAlgorithm : CeHashAlgorithm.SHA256_SALT)
            .hashSalt(cePacketCryptoService.generateSalt())
            .hashIterations(10000)
            .allowedShareScopes(command.shareScopes != null ? command.shareScopes : Set.of(CeShareScope.COC_COORDINATED_ENTRY))
            .encryptionScheme(command.encryptionScheme != null ? command.encryptionScheme : "AES-256-GCM")
            .encryptionKeyId(command.encryptionKeyId)
            .encryptionMetadata(command.encryptionMetadata != null ? command.encryptionMetadata : Map.of())
            .encryptionTags(command.encryptionTags != null ? command.encryptionTags : List.of())
            .packetChecksum(cePacketCryptoService.calculateChecksum(clientHash))
            .ledgerEntryId(command.consentLedgerId)
            .build();

        return cePacketRepository.save(packet);
    }

    private Set<CeShareScope> validateShareScopes(Set<CeShareScope> requestedScopes, CePacket packet) {
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            return packet.getAllowedShareScopes();
        }

        // Only allow scopes that are in both requested and packet allowed
        Set<CeShareScope> allowedScopes = EnumSet.noneOf(CeShareScope.class);
        for (CeShareScope scope : requestedScopes) {
            if (packet.allowsScope(scope)) {
                allowedScopes.add(scope);
            } else {
                logger.warn("Requested share scope {} not allowed by consent", scope);
            }
        }

        if (allowedScopes.isEmpty()) {
            throw new IllegalArgumentException("No valid share scopes for referral");
        }

        return allowedScopes;
    }

    private void writeLedgerEntry(CeReferral referral, String eventType) {
        try {
            Map<String, Object> ledgerData = Map.of(
                "eventType", eventType,
                "referralId", referral.getReferralId(),
                "enrollmentId", referral.getEnrollmentId().value(),
                "clientId", referral.getClientId().value(),
                "projectId", referral.getReferredProjectId(),
                "consentLedgerId", referral.getConsentLedgerId() != null ? referral.getConsentLedgerId() : "",
                "timestamp", Instant.now()
            );

            consentLedgerUpdatePublisher.publishUpdate(ledgerData);
        } catch (Exception e) {
            logger.error("Failed to write to consent ledger", e);
            // Don't fail the operation if ledger write fails
        }
    }

    public static record CreateReferralCommand(
        UUID enrollmentId,
        UUID clientId,
        LocalDateTime referralDate,
        UUID referredProjectId,
        String referredProjectName,
        String referredOrganization,
        CeEventType referralType,
        Integer priorityLevel,
        LocalDate expirationDate,
        UUID consentId,
        UUID consentLedgerId,
        Set<CeShareScope> shareScopes,
        CeHashAlgorithm hashAlgorithm,
        String encryptionScheme,
        String encryptionKeyId,
        Map<String, String> encryptionMetadata,
        List<String> encryptionTags,
        boolean vawaProtection,
        String createdBy,
        String caseManagerName,
        String caseManagerContact,
        Double vulnerabilityScore,
        String notes
    ) {}

    public static record UpdateReferralResultCommand(
        UUID referralId,
        CeReferralResult referralResult,
        LocalDateTime resultDate,
        String rejectionReason,
        String rejectionNotes,
        LocalDate acceptedDate,
        LocalDate housingMoveInDate,
        String updatedBy
    ) {}
}
