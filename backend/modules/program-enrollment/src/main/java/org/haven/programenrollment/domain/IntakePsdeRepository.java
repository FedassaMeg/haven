package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.IntakeDataCollectionStage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for IntakePsdeRecord with lifecycle management support
 * Supports immutable history tracking and audit trail queries
 */
public interface IntakePsdeRepository {

    /**
     * Save a PSDE record
     */
    IntakePsdeRecord save(IntakePsdeRecord record);

    /**
     * Find record by ID (any version)
     */
    Optional<IntakePsdeRecord> findByRecordId(UUID recordId);

    /**
     * Find currently active record by ID
     */
    Optional<IntakePsdeRecord> findActiveByRecordId(UUID recordId);

    /**
     * Find active record for enrollment at specific point in time
     */
    Optional<IntakePsdeRecord> findActiveByEnrollmentIdAsOf(ProgramEnrollmentId enrollmentId, Instant asOfTime);

    /**
     * Find active record for enrollment, date, and collection stage
     */
    Optional<IntakePsdeRecord> findActiveByEnrollmentAndDateAndStage(
        ProgramEnrollmentId enrollmentId,
        LocalDate informationDate,
        IntakeDataCollectionStage collectionStage
    );

    /**
     * Find all records for an enrollment (all versions and corrections)
     */
    List<IntakePsdeRecord> findHistoryByEnrollmentId(ProgramEnrollmentId enrollmentId);

    /**
     * Find all currently active records for an enrollment
     */
    List<IntakePsdeRecord> findActiveByEnrollmentId(ProgramEnrollmentId enrollmentId);

    /**
     * Find records that overlap with a given time period
     */
    List<IntakePsdeRecord> findOverlappingRecords(
        ProgramEnrollmentId enrollmentId,
        Instant effectiveStart,
        Instant effectiveEnd
    );

    /**
     * Find complete audit chain for a record (original + all versions + corrections)
     */
    List<IntakePsdeRecord> findAuditChain(UUID recordId);

    /**
     * Find record by idempotency key
     */
    Optional<IntakePsdeRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find records requiring HUD audit export
     */
    List<IntakePsdeRecord> findForHudAuditExport(
        LocalDate reportingPeriodStart,
        LocalDate reportingPeriodEnd
    );

    /**
     * Find records by correction status
     */
    List<IntakePsdeRecord> findByCorrection(boolean isCorrection);

    /**
     * Find records corrected within time period
     */
    List<IntakePsdeRecord> findCorrectedBetween(Instant startTime, Instant endTime);

    /**
     * Find backdated records within time period
     */
    List<IntakePsdeRecord> findBackdatedBetween(Instant startTime, Instant endTime);

    /**
     * Find records by lifecycle status
     */
    List<IntakePsdeRecord> findByLifecycleStatus(String lifecycleStatus);

    /**
     * Count active records for enrollment
     */
    long countActiveByEnrollmentId(ProgramEnrollmentId enrollmentId);

    /**
     * Count corrections for enrollment
     */
    long countCorrectionsByEnrollmentId(ProgramEnrollmentId enrollmentId);

    /**
     * Delete record (soft delete by setting lifecycle status)
     */
    void deleteRecord(UUID recordId, String deletedBy, String reason);

    /**
     * Find records needing compliance review
     */
    List<IntakePsdeRecord> findRecordsNeedingComplianceReview();

    /**
     * Find records with high-risk DV cases
     */
    List<IntakePsdeRecord> findHighRiskDvCases();

    /**
     * Find records with data quality issues
     */
    List<IntakePsdeRecord> findRecordsWithDataQualityIssues();
}