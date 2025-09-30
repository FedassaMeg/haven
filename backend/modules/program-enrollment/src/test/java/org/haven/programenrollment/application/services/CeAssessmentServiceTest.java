package org.haven.programenrollment.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.Consent;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.clientprofile.domain.consent.ConsentRepository;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.programenrollment.application.security.CePacketCryptoService;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.*;
import org.haven.shared.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CeAssessmentServiceTest {

    @Mock
    private CeAssessmentRepository assessmentRepository;

    @Mock
    private CePacketRepository packetRepository;

    @Mock
    private CePacketCryptoService cryptoService;

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private ConsentLedgerUpdatePublisher ledgerUpdatePublisher;

    @Mock
    private AuditService auditService;

    private CeAssessmentService service;

    @BeforeEach
    void setUp() {
        service = new CeAssessmentService(
            assessmentRepository,
            packetRepository,
            cryptoService,
            consentRepository,
            ledgerUpdatePublisher,
            auditService
        );
    }

    @Test
    void recordAssessment_withValidConsent_shouldCreateAssessmentAndPacket() {
        // Arrange
        UUID enrollmentId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();
        LocalDate assessmentDate = LocalDate.now();

        Consent consent = createActiveConsent(consentId);
        when(consentRepository.findById(any())).thenReturn(Optional.of(consent));
        when(packetRepository.findByEnrollmentIdAndConsentId(any(), any())).thenReturn(Optional.empty());
        when(cryptoService.hashClientId(any(), any())).thenReturn("hashed-client-id");
        when(cryptoService.generateSalt()).thenReturn(new byte[32]);
        when(cryptoService.calculateChecksum(any())).thenReturn("checksum");

        CePacket savedPacket = CePacket.builder()
            .packetId(CePacketId.newId())
            .clientId(new ClientId(clientId))
            .enrollmentId(new ProgramEnrollmentId(enrollmentId))
            .consentId(ConsentId.fromString(consentId.toString()))
            .consentStatus(ConsentStatus.GRANTED)
            .consentVersion(1)
            .consentEffectiveAt(Instant.now())
            .clientHash("hashed-client-id")
            .hashAlgorithm(CeHashAlgorithm.SHA256_SALT)
            .hashSalt(new byte[32])
            .hashIterations(10000)
            .allowedShareScopes(Set.of(CeShareScope.COC_COORDINATED_ENTRY))
            .encryptionScheme("AES-256-GCM")
            .encryptionKeyId("test-key")
            .packetChecksum("checksum")
            .build();
        when(packetRepository.save(any())).thenReturn(savedPacket);

        CeAssessmentService.CreateAssessmentCommand command = new CeAssessmentService.CreateAssessmentCommand(
            enrollmentId,
            clientId,
            assessmentDate,
            CeAssessmentType.CRISIS_NEEDS,
            CeAssessmentLevel.FULL_ASSESSMENT,
            "VI-SPDAT",
            new BigDecimal("12.5"),
            CePrioritizationStatus.PRIORITIZED,
            "Main Office",
            consentId,
            null,
            Set.of(CeShareScope.COC_COORDINATED_ENTRY),
            CeHashAlgorithm.SHA256_SALT,
            "AES-256-GCM",
            "test-key",
            Map.of("version", "1.0"),
            List.of("assessment", "critical"),
            "John Doe",
            null
        );

        // Act
        CeAssessment result = service.recordAssessment(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnrollmentId().value()).isEqualTo(enrollmentId);
        assertThat(result.getClientId().value()).isEqualTo(clientId);
        assertThat(result.getAssessmentDate()).isEqualTo(assessmentDate);
        assertThat(result.getAssessmentType()).isEqualTo(CeAssessmentType.CRISIS_NEEDS);
        assertThat(result.getScore()).isEqualTo(new BigDecimal("12.5"));
        assertThat(result.getPacketId()).isEqualTo(savedPacket.getPacketId());

        verify(assessmentRepository).save(any(CeAssessment.class));
        verify(packetRepository).save(any(CePacket.class));
        verify(ledgerUpdatePublisher).publishUpdate(any());
        verify(auditService).logAction(eq("CE_ASSESSMENT_CREATED"), any());
    }

    @Test
    void recordAssessment_withInactiveConsent_shouldThrowException() {
        // Arrange
        UUID enrollmentId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();

        Consent consent = createInactiveConsent(consentId);
        when(consentRepository.findById(any())).thenReturn(Optional.of(consent));

        CeAssessmentService.CreateAssessmentCommand command = new CeAssessmentService.CreateAssessmentCommand(
            enrollmentId,
            clientId,
            LocalDate.now(),
            CeAssessmentType.CRISIS_NEEDS,
            null,
            null,
            null,
            null,
            null,
            consentId,
            null,
            Set.of(CeShareScope.COC_COORDINATED_ENTRY),
            CeHashAlgorithm.SHA256_SALT,
            null,
            "test-key",
            null,
            null,
            "John Doe",
            null
        );

        // Act & Assert
        assertThatThrownBy(() -> service.recordAssessment(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot create assessment with inactive consent");

        verify(assessmentRepository, never()).save(any());
    }

    @Test
    void recordAssessment_withRestrictedShareScopes_shouldValidateScopes() {
        // Arrange
        UUID enrollmentId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();

        Consent consent = createActiveConsent(consentId);
        when(consentRepository.findById(any())).thenReturn(Optional.of(consent));

        CePacket existingPacket = CePacket.builder()
            .packetId(CePacketId.newId())
            .clientId(new ClientId(clientId))
            .enrollmentId(new ProgramEnrollmentId(enrollmentId))
            .consentId(ConsentId.fromString(consentId.toString()))
            .consentStatus(ConsentStatus.GRANTED)
            .consentVersion(1)
            .consentEffectiveAt(Instant.now())
            .clientHash("hashed-client-id")
            .hashAlgorithm(CeHashAlgorithm.SHA256_SALT)
            .hashSalt(new byte[32])
            .hashIterations(10000)
            .allowedShareScopes(Set.of(CeShareScope.COC_COORDINATED_ENTRY))
            .encryptionScheme("AES-256-GCM")
            .encryptionKeyId("test-key")
            .packetChecksum("checksum")
            .build();
        when(packetRepository.findByEnrollmentIdAndConsentId(any(), any())).thenReturn(Optional.of(existingPacket));

        // Request scopes that are not allowed
        Set<CeShareScope> requestedScopes = Set.of(
            CeShareScope.COC_COORDINATED_ENTRY,
            CeShareScope.VAWA_RESTRICTED_PARTNERS  // This is not in allowed scopes
        );

        CeAssessmentService.CreateAssessmentCommand command = new CeAssessmentService.CreateAssessmentCommand(
            enrollmentId,
            clientId,
            LocalDate.now(),
            CeAssessmentType.CRISIS_NEEDS,
            null,
            null,
            null,
            null,
            null,
            consentId,
            null,
            requestedScopes,
            CeHashAlgorithm.SHA256_SALT,
            null,
            "test-key",
            null,
            null,
            "John Doe",
            null
        );

        // Act
        CeAssessment result = service.recordAssessment(command);

        // Assert
        assertThat(result).isNotNull();
        // Only the allowed scope should be included
        assertThat(result.getConsentScope()).containsExactly(CeShareScope.COC_COORDINATED_ENTRY);
    }

    @Test
    void getAssessmentsForEnrollment_shouldReturnAssessments() {
        // Arrange
        UUID enrollmentId = UUID.randomUUID();
        ProgramEnrollmentId programEnrollmentId = new ProgramEnrollmentId(enrollmentId);

        List<CeAssessment> assessments = List.of(
            createAssessment(enrollmentId, LocalDate.now().minusDays(7)),
            createAssessment(enrollmentId, LocalDate.now().minusDays(3)),
            createAssessment(enrollmentId, LocalDate.now())
        );
        when(assessmentRepository.findByEnrollmentId(programEnrollmentId)).thenReturn(assessments);

        // Act
        List<CeAssessment> result = service.getAssessmentsForEnrollment(enrollmentId);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(assessments);
        verify(auditService).logAction(eq("CE_ASSESSMENTS_ACCESSED"), any());
    }

    @Test
    void recordAssessment_withVAWAProtection_shouldApplyRestrictedScopes() {
        // Arrange
        UUID enrollmentId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();

        Consent consent = createVAWAProtectedConsent(consentId);
        when(consentRepository.findById(any())).thenReturn(Optional.of(consent));
        when(packetRepository.findByEnrollmentIdAndConsentId(any(), any())).thenReturn(Optional.empty());
        when(cryptoService.hashClientId(any(), any())).thenReturn("hashed-client-id");
        when(cryptoService.generateSalt()).thenReturn(new byte[32]);
        when(cryptoService.calculateChecksum(any())).thenReturn("checksum");

        ArgumentCaptor<CePacket> packetCaptor = ArgumentCaptor.forClass(CePacket.class);
        when(packetRepository.save(packetCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        CeAssessmentService.CreateAssessmentCommand command = new CeAssessmentService.CreateAssessmentCommand(
            enrollmentId,
            clientId,
            LocalDate.now(),
            CeAssessmentType.CRISIS_NEEDS,
            null,
            null,
            null,
            null,
            null,
            consentId,
            null,
            Set.of(CeShareScope.VAWA_RESTRICTED_PARTNERS),
            CeHashAlgorithm.SHA256_SALT,
            null,
            "test-key",
            Map.of("vawa", "true"),
            List.of("vawa-protected"),
            "John Doe",
            null
        );

        // Act
        service.recordAssessment(command);

        // Assert
        CePacket savedPacket = packetCaptor.getValue();
        assertThat(savedPacket.getAllowedShareScopes()).contains(CeShareScope.VAWA_RESTRICTED_PARTNERS);
        assertThat(savedPacket.getEncryptionMetadata()).containsEntry("vawa", "true");
        assertThat(savedPacket.getEncryptionTags()).contains("vawa-protected");
    }

    private Consent createActiveConsent(UUID consentId) {
        Consent consent = mock(Consent.class);
        when(consent.getConsentId()).thenReturn(ConsentId.fromString(consentId.toString()));
        when(consent.isActive()).thenReturn(true);
        when(consent.getStatus()).thenReturn(ConsentStatus.GRANTED);
        when(consent.getVersion()).thenReturn(1L);
        when(consent.getEffectiveDate()).thenReturn(Instant.now());
        when(consent.getExpirationDate()).thenReturn(null);
        return consent;
    }

    private Consent createInactiveConsent(UUID consentId) {
        Consent consent = mock(Consent.class);
        when(consent.getConsentId()).thenReturn(ConsentId.fromString(consentId.toString()));
        when(consent.isActive()).thenReturn(false);
        when(consent.getStatus()).thenReturn(ConsentStatus.REVOKED);
        return consent;
    }

    private Consent createVAWAProtectedConsent(UUID consentId) {
        Consent consent = mock(Consent.class);
        when(consent.getConsentId()).thenReturn(ConsentId.fromString(consentId.toString()));
        when(consent.isActive()).thenReturn(true);
        when(consent.getStatus()).thenReturn(ConsentStatus.GRANTED);
        when(consent.getVersion()).thenReturn(1L);
        when(consent.getEffectiveDate()).thenReturn(Instant.now());
        when(consent.getExpirationDate()).thenReturn(null);
        when(consent.isVAWAProtected()).thenReturn(true);
        return consent;
    }

    private CeAssessment createAssessment(UUID enrollmentId, LocalDate assessmentDate) {
        return CeAssessment.create(
            new ProgramEnrollmentId(enrollmentId),
            new ClientId(UUID.randomUUID()),
            assessmentDate,
            CeAssessmentType.CRISIS_NEEDS,
            CeAssessmentLevel.FULL_ASSESSMENT,
            "VI-SPDAT",
            new BigDecimal("10.0"),
            CePrioritizationStatus.PRIORITIZED,
            "Office",
            "Test User",
            CePacketId.newId(),
            UUID.randomUUID(),
            Set.of(CeShareScope.COC_COORDINATED_ENTRY)
        );
    }
}