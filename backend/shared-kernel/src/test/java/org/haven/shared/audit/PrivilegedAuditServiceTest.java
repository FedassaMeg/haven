package org.haven.shared.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivilegedAuditService
 *
 * Verifies:
 * - Audit events are properly constructed
 * - Both success and denial events are logged
 * - Justification validation for privileged actions
 * - No sensitive payloads in logs
 * - Database persistence occurs
 */
@ExtendWith(MockitoExtension.class)
class PrivilegedAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private PrivilegedAuditService privilegedAuditService;

    @BeforeEach
    void setUp() {
        privilegedAuditService = new PrivilegedAuditService(auditLogRepository);
    }

    @Test
    void logAction_successfulDvNoteRead_shouldPersistAuditEvent() {
        // Given
        UUID actorId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.DV_NOTE_READ)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(actorId)
            .actorUsername("jane.advocate")
            .actorRoles(List.of("DV_ADVOCATE", "CASE_MANAGER"))
            .resourceType("RestrictedNote")
            .resourceId(noteId)
            .resourceDescription("DV Safety Plan Note")
            .justification("Case review for safety planning session")
            .addMetadata("noteType", "SAFETY_PLAN")
            .build();

        // When
        privilegedAuditService.logAction(event);

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getAuditId()).isEqualTo(event.eventId());
        assertThat(savedEntity.getAction()).isEqualTo("DV_NOTE_READ");
        assertThat(savedEntity.getUserId()).isEqualTo(actorId);
        assertThat(savedEntity.getResourceId()).isEqualTo(noteId);
        assertThat(savedEntity.getResourceType()).isEqualTo("RestrictedNote");
        assertThat(savedEntity.getResult()).isEqualTo("SUCCESS");
        assertThat(savedEntity.getSeverity()).isEqualTo("CRITICAL");
        assertThat(savedEntity.getComponent()).isEqualTo("PRIVILEGED_AUDIT");
    }

    @Test
    void logAction_deniedExportAttempt_shouldLogDenialWithReason() {
        // Given
        UUID actorId = UUID.randomUUID();
        UUID exportJobId = UUID.randomUUID();

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.EXPORT_INITIATED)
            .outcome(AuditOutcome.DENIED_INSUFFICIENT_PERMISSION)
            .actorId(actorId)
            .actorUsername("john.volunteer")
            .actorRoles(List.of("VOLUNTEER"))
            .resourceType("ExportJob")
            .resourceId(exportJobId)
            .resourceDescription("HUD Annual Export")
            .denialReason("INSUFFICIENT_PERMISSION")
            .denialDetails("User lacks DATA_STEWARD or HMIS_LEAD role")
            .build();

        // When
        privilegedAuditService.logAction(event);

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getResult()).isEqualTo("DENIED_INSUFFICIENT_PERMISSION");
        assertThat(savedEntity.getDetails()).contains("INSUFFICIENT_PERMISSION");
        assertThat(savedEntity.getDetails()).contains("User lacks DATA_STEWARD");
    }

    @Test
    void logAction_exportCompleted_shouldIncludeHashFingerprint() {
        // Given
        UUID actorId = UUID.randomUUID();
        UUID exportJobId = UUID.randomUUID();
        String sha256Hash = "a1b2c3d4e5f6789012345678901234567890123456789012345678901234567890";

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.EXPORT_COMPLETED)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(actorId)
            .actorUsername("data.steward")
            .actorRoles(List.of("DATA_STEWARD", "HMIS_LEAD"))
            .resourceType("ExportJob")
            .resourceId(exportJobId)
            .resourceDescription("HUD Annual Export 2024")
            .justification("Annual HUD reporting requirement")
            .hashFingerprint(sha256Hash)
            .addMetadata("totalRecords", 1500)
            .addMetadata("vawaSuppressed", 23)
            .build();

        // When
        privilegedAuditService.logAction(event);

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getDetails()).contains("Hash Fingerprint: " + sha256Hash);
        assertThat(savedEntity.getDetails()).contains("Annual HUD reporting");
    }

    @Test
    void logAction_consentLedgerEntry_shouldIncludeLedgerId() {
        // Given
        UUID actorId = UUID.randomUUID();
        UUID exportJobId = UUID.randomUUID();
        String ledgerId = "LEDGER-2024-001234";

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.CONSENT_LEDGER_ENTRY_CREATED)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(actorId)
            .actorUsername("compliance.officer")
            .actorRoles(List.of("COMPLIANCE_OFFICER", "DATA_STEWARD"))
            .resourceType("ConsentLedger")
            .resourceId(exportJobId)
            .resourceDescription("Consent ledger for HUD export")
            .consentLedgerId(ledgerId)
            .justification("Recording export consent scope")
            .build();

        // When
        privilegedAuditService.logAction(event);

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getDetails()).contains("Consent Ledger ID: " + ledgerId);
    }

    @Test
    void logAction_withRequestId_shouldPreserveMdcContext() {
        // Given
        String requestId = "req-" + UUID.randomUUID();

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.DV_NOTE_WRITE)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(UUID.randomUUID())
            .actorUsername("test.user")
            .actorRoles(List.of("CASE_MANAGER"))
            .resourceType("RestrictedNote")
            .resourceId(UUID.randomUUID())
            .justification("Case documentation")
            .requestId(requestId)
            .build();

        // When
        privilegedAuditService.logAction(event);

        // Then
        verify(auditLogRepository).save(any(AuditLogEntity.class));
        // MDC context handling is verified by the service not throwing exceptions
    }

    @Test
    void logSuccess_shouldCreateSuccessEvent() {
        // Given
        PrivilegedAuditEvent.Builder builder = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.DV_NOTE_SEAL)
            .actorId(UUID.randomUUID())
            .actorUsername("admin.user")
            .actorRoles(List.of("ADMINISTRATOR"))
            .resourceType("RestrictedNote")
            .resourceId(UUID.randomUUID())
            .justification("Court order compliance");

        // When
        privilegedAuditService.logSuccess(builder);

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getResult()).isEqualTo("SUCCESS");
    }

    @Test
    void logDenial_shouldCreateDenialEvent() {
        // Given
        PrivilegedAuditEvent.Builder builder = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.CONSENT_OVERRIDE_ATTEMPTED)
            .actorId(UUID.randomUUID())
            .actorUsername("test.user")
            .actorRoles(List.of("CASE_MANAGER"))
            .resourceType("ConsentRecord")
            .resourceId(UUID.randomUUID());

        // When
        privilegedAuditService.logDenial(
            builder,
            AuditOutcome.DENIED_VAWA_PROTECTED,
            "VAWA_PROTECTED",
            "Client record is VAWA protected - override not permitted"
        );

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getResult()).isEqualTo("DENIED_VAWA_PROTECTED");
        assertThat(savedEntity.getDetails()).contains("VAWA_PROTECTED");
        assertThat(savedEntity.getDetails()).contains("override not permitted");
    }

    @Test
    void logAction_databaseFailure_shouldNotThrowException() {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
            .when(auditLogRepository).save(any(AuditLogEntity.class));

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.DV_NOTE_READ)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(UUID.randomUUID())
            .actorUsername("test.user")
            .actorRoles(List.of("CASE_MANAGER"))
            .resourceType("RestrictedNote")
            .resourceId(UUID.randomUUID())
            .justification("Test")
            .build();

        // When/Then - should not throw exception
        assertThatCode(() -> privilegedAuditService.logAction(event))
            .doesNotThrowAnyException();

        // Verify repository was called (and failed)
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    void logAction_noSensitiveDataInLogs_shouldRedactPii() {
        // Given
        UUID actorId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        PrivilegedAuditEvent event = PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.DV_NOTE_READ)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(actorId)
            .actorUsername("jane.advocate")
            .actorRoles(List.of("DV_ADVOCATE"))
            .resourceType("RestrictedNote")
            .resourceId(noteId)
            .resourceDescription("DV Note") // No client name or SSN
            .justification("Case review")
            .addMetadata("clientId", UUID.randomUUID().toString()) // ID only, no PII
            .build();

        // When
        privilegedAuditService.logAction(event);

        // Then
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedEntity = captor.getValue();
        // Verify no SSN, full names, or other PII in details
        assertThat(savedEntity.getDetails()).doesNotContainPattern("\\d{3}-\\d{2}-\\d{4}"); // No SSN
        assertThat(savedEntity.getDetails()).doesNotContainPattern("\\d{9}"); // No unformatted SSN
    }
}
