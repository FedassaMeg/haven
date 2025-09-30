package org.haven.programenrollment.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.programenrollment.domain.IntakePsdeRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.hmis.IntakeDataCollectionStage;
import org.haven.programenrollment.application.validation.IntakePsdeValidationService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PSDE Lifecycle Service including:
 * - Historical snapshots and versioning
 * - Timezone handling and effective dating
 * - Correction workflows and audit trails
 * - Idempotent operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PSDE Lifecycle Service Tests")
class IntakePsdeLifecycleServiceTest {

    @Mock
    private IntakePsdeRepository repository;

    @Mock
    private IntakePsdeAuditLogger auditLogger;

    @Mock
    private IntakePsdeValidationService validationService;

    @InjectMocks
    private IntakePsdeLifecycleService lifecycleService;

    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private String collectedBy;

    @BeforeEach
    void setUp() {
        enrollmentId = new ProgramEnrollmentId(UUID.randomUUID());
        clientId = new ClientId(UUID.randomUUID());
        collectedBy = "test-user";

        // Mock successful validation by default
        when(validationService.validateIntakePsdeRecord(any()))
            .thenReturn(new IntakePsdeValidationService.ValidationResult(true, List.of()));
    }

    @Nested
    @DisplayName("Historical Snapshots and Versioning")
    class HistoricalSnapshotsTests {

        @Test
        @DisplayName("Should create initial record with version 1")
        void shouldCreateInitialRecordWithVersion1() {
            // Given
            LocalDate informationDate = LocalDate.now();
            IntakeDataCollectionStage stage = IntakeDataCollectionStage.INITIAL_INTAKE;

            when(repository.findActiveByEnrollmentAndDateAndStage(enrollmentId, informationDate, stage))
                .thenReturn(Optional.empty());
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord result = lifecycleService.createRecord(
                enrollmentId, clientId, informationDate, stage, collectedBy);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getVersion());
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString(),
                        result.getLifecycleStatus());
            assertNotNull(result.getEffectiveStart());
            assertNull(result.getEffectiveEnd());
            assertFalse(result.getIsBackdated());

            verify(repository).save(result);
            verify(auditLogger).logRecordCreation(
                result.getRecordId().toString(),
                collectedBy,
                clientId.toString(),
                enrollmentId.toString()
            );
        }

        @Test
        @DisplayName("Should create new version when updating existing record")
        void shouldCreateNewVersionWhenUpdating() {
            // Given
            UUID originalRecordId = UUID.randomUUID();
            IntakePsdeRecord originalRecord = createSampleRecord(originalRecordId, 1);
            originalRecord.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString());

            IntakePsdeUpdateRequest updateRequest = new IntakePsdeUpdateRequest.Builder()
                .withIncome(2000, null)
                .withUpdateReason("Updated income information")
                .build();

            when(repository.findActiveByRecordId(originalRecordId))
                .thenReturn(Optional.of(originalRecord));
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord result = lifecycleService.updateRecord(
                originalRecordId, updateRequest, "updater");

            // Then
            // Verify original record was superseded
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.SUPERSEDED.toString(),
                        originalRecord.getLifecycleStatus());
            assertNotNull(originalRecord.getEffectiveEnd());
            assertNotNull(originalRecord.getSupersededAt());
            assertEquals("updater", originalRecord.getSupersededBy());

            // Verify new version was created
            assertNotEquals(originalRecordId, result.getRecordId());
            assertEquals(2, result.getVersion());
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString(),
                        result.getLifecycleStatus());
            assertEquals(originalRecordId, result.getSupersedes());
            assertNotNull(result.getEffectiveStart());

            verify(repository, times(2)).save(any(IntakePsdeRecord.class));
        }

        @Test
        @DisplayName("Should maintain immutable history across multiple updates")
        void shouldMaintainImmutableHistoryAcrossMultipleUpdates() {
            // Given - Create chain of 3 versions
            UUID record1Id = UUID.randomUUID();
            UUID record2Id = UUID.randomUUID();
            UUID record3Id = UUID.randomUUID();

            IntakePsdeRecord record1 = createSampleRecord(record1Id, 1);
            IntakePsdeRecord record2 = createSampleRecord(record2Id, 2);
            IntakePsdeRecord record3 = createSampleRecord(record3Id, 3);

            record1.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.SUPERSEDED.toString());
            record1.setSupersedes(null);
            record1.setEffectiveEnd(Instant.now().minusSeconds(3600));

            record2.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.SUPERSEDED.toString());
            record2.setSupersedes(record1Id);
            record2.setEffectiveEnd(Instant.now().minusSeconds(1800));

            record3.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString());
            record3.setSupersedes(record2Id);

            List<IntakePsdeRecord> auditChain = List.of(record1, record2, record3);
            when(repository.findAuditChain(record3Id)).thenReturn(auditChain);

            // When
            IntakePsdeAuditTrail auditTrail = lifecycleService.getAuditTrail(record3Id);

            // Then
            assertNotNull(auditTrail);
            assertEquals(3, auditTrail.getEntries().size());
            assertEquals(2, auditTrail.getSummary().getTotalVersions()); // 2 updates from original
            assertEquals(0, auditTrail.getSummary().getTotalCorrections());

            // Verify chronological order
            List<IntakePsdeAuditTrail.AuditTrailEntry> entries = auditTrail.getEntries();
            assertTrue(entries.get(0).getTimestamp().isBefore(entries.get(1).getTimestamp()));
            assertTrue(entries.get(1).getTimestamp().isBefore(entries.get(2).getTimestamp()));
        }
    }

    @Nested
    @DisplayName("Timezone Handling Tests")
    class TimezoneHandlingTests {

        @Test
        @DisplayName("Should handle different timezones correctly for effective dates")
        void shouldHandleTimezonesCorrectlyForEffectiveDates() {
            // Given - Different timezone scenarios
            ZoneId utcZone = ZoneId.of("UTC");
            ZoneId eastCoastZone = ZoneId.of("America/New_York");
            ZoneId westCoastZone = ZoneId.of("America/Los_Angeles");

            LocalDate informationDate = LocalDate.of(2024, 3, 15);

            // Create effective times in different zones (same wall clock time)
            ZonedDateTime utcTime = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, utcZone);
            ZonedDateTime eastTime = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, eastCoastZone);
            ZonedDateTime westTime = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, westCoastZone);

            when(repository.findActiveByEnrollmentIdAsOf(eq(enrollmentId), any(Instant.class)))
                .thenReturn(Optional.empty());

            // When - Create records with different timezone effective dates
            Instant utcInstant = utcTime.toInstant();
            Instant eastInstant = eastTime.toInstant();
            Instant westInstant = westTime.toInstant();

            // Then - Verify UTC normalization
            assertNotEquals(utcInstant, eastInstant); // Different due to timezone offset
            assertNotEquals(utcInstant, westInstant);
            assertNotEquals(eastInstant, westInstant);

            // Verify the service can query by any timezone correctly
            lifecycleService.getActiveRecordAsOf(enrollmentId, utcInstant);
            lifecycleService.getActiveRecordAsOf(enrollmentId, eastInstant);
            lifecycleService.getActiveRecordAsOf(enrollmentId, westInstant);

            verify(repository, times(3)).findActiveByEnrollmentIdAsOf(eq(enrollmentId), any(Instant.class));
        }

        @Test
        @DisplayName("Should handle daylight saving time transitions correctly")
        void shouldHandleDaylightSavingTimeTransitions() {
            // Given - DST transition dates for 2024
            ZoneId eastCoastZone = ZoneId.of("America/New_York");

            // Spring forward: 2:00 AM becomes 3:00 AM on March 10, 2024
            LocalDate springTransition = LocalDate.of(2024, 3, 10);
            ZonedDateTime beforeSpringForward = ZonedDateTime.of(2024, 3, 10, 1, 30, 0, 0, eastCoastZone);
            ZonedDateTime afterSpringForward = ZonedDateTime.of(2024, 3, 10, 3, 30, 0, 0, eastCoastZone);

            // Fall back: 2:00 AM becomes 1:00 AM on November 3, 2024
            LocalDate fallTransition = LocalDate.of(2024, 11, 3);
            ZonedDateTime beforeFallBack = ZonedDateTime.of(2024, 11, 3, 1, 30, 0, 0, eastCoastZone);
            ZonedDateTime afterFallBack = ZonedDateTime.of(2024, 11, 3, 1, 30, 0, 0, eastCoastZone);

            when(repository.findActiveByEnrollmentIdAsOf(eq(enrollmentId), any(Instant.class)))
                .thenReturn(Optional.empty());

            // When - Query during DST transitions
            lifecycleService.getActiveRecordAsOf(enrollmentId, beforeSpringForward.toInstant());
            lifecycleService.getActiveRecordAsOf(enrollmentId, afterSpringForward.toInstant());
            lifecycleService.getActiveRecordAsOf(enrollmentId, beforeFallBack.toInstant());

            // Then - All queries should work without timezone-related errors
            verify(repository, times(3)).findActiveByEnrollmentIdAsOf(eq(enrollmentId), any(Instant.class));
        }

        @Test
        @DisplayName("Should correctly order records across timezone boundaries")
        void shouldCorrectlyOrderRecordsAcrossTimezoneBoundaries() {
            // Given - Records created in different timezones but close in time
            ZoneId utcZone = ZoneId.of("UTC");
            ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");

            // Tokyo is UTC+9, so when it's noon in Tokyo, it's 3 AM UTC
            ZonedDateTime tokyoNoon = ZonedDateTime.of(2024, 3, 15, 12, 0, 0, 0, tokyoZone);
            ZonedDateTime utcMorning = ZonedDateTime.of(2024, 3, 15, 4, 0, 0, 0, utcZone);

            IntakePsdeRecord tokyoRecord = createSampleRecord(UUID.randomUUID(), 1);
            tokyoRecord.setEffectiveStart(tokyoNoon.toInstant());

            IntakePsdeRecord utcRecord = createSampleRecord(UUID.randomUUID(), 1);
            utcRecord.setEffectiveStart(utcMorning.toInstant());

            // When - Compare effective times
            boolean tokyoIsBeforeUtc = tokyoRecord.getEffectiveStart().isBefore(utcRecord.getEffectiveStart());

            // Then - Tokyo noon (3 AM UTC) should be before UTC 4 AM
            assertTrue(tokyoIsBeforeUtc, "Tokyo noon should be before UTC 4 AM when converted to UTC");
        }
    }

    @Nested
    @DisplayName("Backdated Entries Tests")
    class BackdatedEntriesTests {

        @Test
        @DisplayName("Should create backdated record with proper effective dating")
        void shouldCreateBackdatedRecordWithProperEffectiveDating() {
            // Given
            LocalDate informationDate = LocalDate.of(2024, 3, 1);
            Instant effectiveAsOf = Instant.now().minusSeconds(7 * 24 * 60 * 60); // 7 days ago
            String backdatingReason = "Late documentation received";

            when(repository.findOverlappingRecords(eq(enrollmentId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord result = lifecycleService.createBackdatedRecord(
                enrollmentId,
                clientId,
                informationDate,
                effectiveAsOf,
                IntakeDataCollectionStage.INITIAL_INTAKE,
                collectedBy,
                backdatingReason
            );

            // Then
            assertNotNull(result);
            assertTrue(result.getIsBackdated());
            assertEquals(backdatingReason, result.getBackdatingReason());
            assertEquals(effectiveAsOf, result.getEffectiveStart());
            assertEquals(1, result.getVersion());

            verify(auditLogger).logBackdatedEntry(
                result.getRecordId().toString(),
                collectedBy,
                informationDate.toString(),
                effectiveAsOf.toString(),
                backdatingReason
            );
        }

        @Test
        @DisplayName("Should reject backdating beyond 30-day limit without supervisor approval")
        void shouldRejectBackdatingBeyond30DayLimit() {
            // Given
            LocalDate informationDate = LocalDate.of(2024, 1, 1);
            Instant effectiveAsOf = Instant.now().minusSeconds(31 * 24 * 60 * 60); // 31 days ago

            // When & Then
            assertThrows(IntakePsdeLifecycleService.IntakePsdeValidationException.class, () -> {
                lifecycleService.createBackdatedRecord(
                    enrollmentId,
                    clientId,
                    informationDate,
                    effectiveAsOf,
                    IntakeDataCollectionStage.INITIAL_INTAKE,
                    collectedBy,
                    "Very late documentation"
                );
            });
        }

        @Test
        @DisplayName("Should handle timeline conflicts when creating backdated records")
        void shouldHandleTimelineConflictsWhenCreatingBackdatedRecords() {
            // Given
            Instant backDateTime = Instant.now().minusSeconds(10 * 24 * 60 * 60); // 10 days ago
            LocalDate informationDate = LocalDate.now().minusDays(10);

            // Existing overlapping record
            IntakePsdeRecord overlappingRecord = createSampleRecord(UUID.randomUUID(), 1);
            overlappingRecord.setEffectiveStart(backDateTime.minusSeconds(24 * 60 * 60)); // 11 days ago
            overlappingRecord.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString());

            when(repository.findOverlappingRecords(eq(enrollmentId), any(Instant.class), isNull()))
                .thenReturn(List.of(overlappingRecord));
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord result = lifecycleService.createBackdatedRecord(
                enrollmentId,
                clientId,
                informationDate,
                backDateTime,
                IntakeDataCollectionStage.INITIAL_INTAKE,
                collectedBy,
                "Backdated entry"
            );

            // Then
            // Verify overlapping record was superseded
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.SUPERSEDED.toString(),
                        overlappingRecord.getLifecycleStatus());
            assertEquals(backDateTime, overlappingRecord.getEffectiveEnd());

            // Verify new record is active
            assertTrue(result.getIsBackdated());
            assertEquals(backDateTime, result.getEffectiveStart());

            verify(repository, times(2)).save(any(IntakePsdeRecord.class));
        }
    }

    @Nested
    @DisplayName("Correction Workflows Tests")
    class CorrectionWorkflowsTests {

        @Test
        @DisplayName("Should create correction record with proper audit trail")
        void shouldCreateCorrectionRecordWithProperAuditTrail() {
            // Given
            UUID originalRecordId = UUID.randomUUID();
            IntakePsdeRecord originalRecord = createSampleRecord(originalRecordId, 1);
            originalRecord.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString());

            IntakePsdeUpdateRequest correctionRequest = new IntakePsdeUpdateRequest.Builder()
                .withIncome(1500, null)
                .withUpdateReason("Correcting data entry error")
                .build();

            IntakePsdeLifecycleService.CorrectionReason reason = IntakePsdeLifecycleService.CorrectionReason.DATA_ENTRY_ERROR;

            when(repository.findByRecordId(originalRecordId))
                .thenReturn(Optional.of(originalRecord));
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord correctionRecord = lifecycleService.createCorrection(
                originalRecordId,
                correctionRequest,
                reason,
                "test-corrector"
            );

            // Then
            // Verify original record marked as corrected
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.CORRECTED.toString(),
                        originalRecord.getLifecycleStatus());
            assertNotNull(originalRecord.getCorrectedAt());
            assertEquals("test-corrector", originalRecord.getCorrectedBy());

            // Verify correction record properties
            assertTrue(correctionRecord.getIsCorrection());
            assertEquals(originalRecordId, correctionRecord.getCorrectsRecordId());
            assertEquals(reason.name(), correctionRecord.getCorrectionReason());
            assertEquals(1, correctionRecord.getVersion()); // Corrections start fresh version chain
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString(),
                        correctionRecord.getLifecycleStatus());

            verify(auditLogger).logDataCorrection(
                originalRecordId.toString(),
                correctionRecord.getRecordId().toString(),
                "test-corrector",
                reason.getCode(),
                reason.getDescription()
            );
        }

        @Test
        @DisplayName("Should handle corrections of historical records")
        void shouldHandleCorrectionsOfHistoricalRecords() {
            // Given - Historical (already superseded) record
            UUID historicalRecordId = UUID.randomUUID();
            IntakePsdeRecord historicalRecord = createSampleRecord(historicalRecordId, 1);
            historicalRecord.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.SUPERSEDED.toString());
            historicalRecord.setEffectiveEnd(Instant.now().minusSeconds(3600));

            IntakePsdeUpdateRequest correctionRequest = new IntakePsdeUpdateRequest.Builder()
                .withDomesticViolence(null, null)
                .withUpdateReason("Correcting historical DV data")
                .build();

            IntakePsdeLifecycleService.CorrectionReason reason = IntakePsdeLifecycleService.CorrectionReason.AUDIT_FINDING;

            when(repository.findByRecordId(historicalRecordId))
                .thenReturn(Optional.of(historicalRecord));
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord correctionRecord = lifecycleService.createCorrection(
                historicalRecordId,
                correctionRequest,
                reason,
                "auditor"
            );

            // Then
            // Historical record should be marked as corrected but keep its superseded status info
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.CORRECTED.toString(),
                        historicalRecord.getLifecycleStatus());
            assertNotNull(historicalRecord.getEffectiveEnd()); // Should preserve original end time

            // Correction should be active
            assertEquals(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString(),
                        correctionRecord.getLifecycleStatus());
            assertTrue(correctionRecord.getIsCorrection());
        }
    }

    @Nested
    @DisplayName("Idempotent Operations Tests")
    class IdempotentOperationsTests {

        @Test
        @DisplayName("Should return existing result for duplicate idempotency key")
        void shouldReturnExistingResultForDuplicateIdempotencyKey() {
            // Given
            UUID recordId = UUID.randomUUID();
            String idempotencyKey = "test-key-12345";
            IntakePsdeUpdateRequest updateRequest = new IntakePsdeUpdateRequest.Builder()
                .withIncome(1000, null)
                .build();

            IntakePsdeRecord existingResult = createSampleRecord(UUID.randomUUID(), 2);
            existingResult.setIdempotencyKey(idempotencyKey);

            when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingResult));

            // When
            IntakePsdeRecord result = lifecycleService.idempotentUpdate(
                recordId, updateRequest, "updater", idempotencyKey);

            // Then
            assertEquals(existingResult, result);
            verify(repository, never()).save(any(IntakePsdeRecord.class));
            verify(auditLogger).logIdempotentOperation(
                recordId.toString(), "updater", idempotencyKey, "DUPLICATE_DETECTED");
        }

        @Test
        @DisplayName("Should execute operation for new idempotency key")
        void shouldExecuteOperationForNewIdempotencyKey() {
            // Given
            UUID recordId = UUID.randomUUID();
            String idempotencyKey = "test-key-67890";
            IntakePsdeUpdateRequest updateRequest = new IntakePsdeUpdateRequest.Builder()
                .withIncome(2000, null)
                .build();

            IntakePsdeRecord originalRecord = createSampleRecord(recordId, 1);
            originalRecord.setLifecycleStatus(IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.ACTIVE.toString());

            when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
            when(repository.findActiveByRecordId(recordId))
                .thenReturn(Optional.of(originalRecord));
            when(repository.save(any(IntakePsdeRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            IntakePsdeRecord result = lifecycleService.idempotentUpdate(
                recordId, updateRequest, "updater", idempotencyKey);

            // Then
            assertNotNull(result);
            assertEquals(idempotencyKey, result.getIdempotencyKey());
            assertEquals(2, result.getVersion());

            verify(repository, times(2)).save(any(IntakePsdeRecord.class)); // Original + new version
            verify(auditLogger).logIdempotentOperation(
                recordId.toString(), "updater", idempotencyKey, "OPERATION_EXECUTED");
        }
    }

    // Helper methods
    private IntakePsdeRecord createSampleRecord(UUID recordId, int version) {
        IntakePsdeRecord record = IntakePsdeRecord.createForLifecycle(
            enrollmentId,
            clientId,
            LocalDate.now(),
            IntakeDataCollectionStage.INITIAL_INTAKE,
            collectedBy
        );

        record.setRecordId(recordId);
        record.setVersion(version);
        record.setEffectiveStart(Instant.now().minusSeconds(3600 * version));

        // Set some sample data
        record.updateIncomeInformation(1000, null, false, false);
        record.updateDomesticViolenceInformation(null, null, null, null, false);

        return record;
    }
}