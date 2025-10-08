package org.haven.programenrollment.application.services;

import org.haven.programenrollment.application.validation.IntakePsdeValidationService;
import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.programenrollment.domain.IntakePsdeRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.hmis.IntakeDataCollectionStage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PSDE Lifecycle Service handling create/update, corrections, and supersession
 * Enforces immutable history with effective_start/effective_end timestamps
 * Implements HUD audit trail requirements for HMIS data integrity
 */
@Service
@Transactional
public class IntakePsdeLifecycleService {

    private final IntakePsdeRepository repository;
    private final IntakePsdeAuditLogger auditLogger;
    private final IntakePsdeValidationService validationService;

    public IntakePsdeLifecycleService(
            IntakePsdeRepository repository,
            IntakePsdeAuditLogger auditLogger,
            IntakePsdeValidationService validationService) {
        this.repository = repository;
        this.auditLogger = auditLogger;
        this.validationService = validationService;
    }

    /**
     * Create new PSDE record with lifecycle tracking
     */
    public IntakePsdeRecord createRecord(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate informationDate,
            IntakeDataCollectionStage collectionStage,
            String collectedBy) {

        // Validate that no active record exists for this date/stage combination
        validateNoConflictingActiveRecord(enrollmentId, informationDate, collectionStage);

        // Create new record with lifecycle metadata
        IntakePsdeRecord record = IntakePsdeRecord.createForLifecycle(
            enrollmentId, clientId, informationDate, collectionStage, collectedBy
        );

        // Set lifecycle timestamps
        Instant now = Instant.now();
        record.setEffectiveStart(now);
        record.setLifecycleStatus(IntakePsdeLifecycleStatus.ACTIVE.name());
        record.setVersion(1);

        // Validate record
        var validationResult = validationService.validateIntakePsdeRecord(record);
        if (validationResult.hasErrors()) {
            throw new IntakePsdeValidationException("Validation failed",
                validationResult.errors().stream()
                    .map(e -> e.field() + ": " + e.message())
                    .toList());
        }

        // Persist record
        record = repository.save(record);

        // Log creation
        auditLogger.logRecordCreation(
            record.getRecordId().toString(),
            collectedBy,
            clientId.toString(),
            enrollmentId.toString()
        );

        return record;
    }

    /**
     * Update existing PSDE record with immutable history preservation
     */
    public IntakePsdeRecord updateRecord(
            UUID recordId,
            IntakePsdeUpdateRequest updateRequest,
            String updatedBy) {

        // Retrieve current active record
        IntakePsdeRecord currentRecord = repository.findActiveByRecordId(recordId)
            .orElseThrow(() -> new IntakePsdeNotFoundException("No active record found with ID: " + recordId));

        // Create new version with updates applied
        IntakePsdeRecord newVersion = createNewVersion(currentRecord, updateRequest, updatedBy);

        // Validate new version
        var validationResult = validationService.validateIntakePsdeRecord(newVersion);
        if (validationResult.hasErrors()) {
            throw new IntakePsdeValidationException("Validation failed",
                validationResult.errors().stream()
                    .map(e -> e.field() + ": " + e.message())
                    .toList());
        }

        // End current record's effective period
        Instant now = Instant.now();
        currentRecord.setEffectiveEnd(now);
        currentRecord.setLifecycleStatus(IntakePsdeLifecycleStatus.SUPERSEDED.name());
        currentRecord.setSupersededAt(now);
        currentRecord.setSupersededBy(updatedBy);

        // Set new record's lifecycle metadata
        newVersion.setEffectiveStart(now);
        newVersion.setLifecycleStatus(IntakePsdeLifecycleStatus.ACTIVE.name());
        newVersion.setSupersedes(currentRecord.getRecordId());

        // Save both records atomically
        repository.save(currentRecord);
        newVersion = repository.save(newVersion);

        // Log update
        auditLogger.logRecordUpdate(
            recordId.toString(),
            updatedBy,
            "VERSION_UPDATE",
            updateRequest.getChangedFields()
        );

        return newVersion;
    }

    /**
     * Create correction record with proper audit trail
     */
    public IntakePsdeRecord createCorrection(
            UUID originalRecordId,
            IntakePsdeUpdateRequest correctionRequest,
            CorrectionReason reason,
            String correctedBy) {

        // Retrieve original record (may be active or historical)
        IntakePsdeRecord originalRecord = repository.findByRecordId(originalRecordId)
            .orElseThrow(() -> new IntakePsdeNotFoundException("Original record not found: " + originalRecordId));

        // Create correction record
        IntakePsdeRecord correctionRecord = createCorrectionVersion(
            originalRecord, correctionRequest, reason, correctedBy
        );

        // Validate correction
        var validationResult = validationService.validateIntakePsdeRecord(correctionRecord);
        if (validationResult.hasErrors()) {
            throw new IntakePsdeValidationException("Correction validation failed",
                validationResult.errors().stream()
                    .map(e -> e.field() + ": " + e.message())
                    .toList());
        }

        Instant now = Instant.now();

        // If original record is still active, supersede it
        if (originalRecord.getLifecycleStatus().equals(IntakePsdeLifecycleStatus.ACTIVE.name())) {
            originalRecord.setEffectiveEnd(now);
            originalRecord.setLifecycleStatus(IntakePsdeLifecycleStatus.CORRECTED.name());
            originalRecord.setCorrectedAt(now);
            originalRecord.setCorrectedBy(correctedBy);
            repository.save(originalRecord);
        } else {
            // Mark historical record as corrected
            originalRecord.setLifecycleStatus(IntakePsdeLifecycleStatus.CORRECTED.name());
            originalRecord.setCorrectedAt(now);
            originalRecord.setCorrectedBy(correctedBy);
            repository.save(originalRecord);
        }

        // Set correction metadata
        correctionRecord.setEffectiveStart(now);
        correctionRecord.setLifecycleStatus(IntakePsdeLifecycleStatus.ACTIVE.name());
        correctionRecord.setIsCorrection(true);
        correctionRecord.setCorrectsRecordId(originalRecordId);
        correctionRecord.setCorrectionReason(reason.name());

        // Save correction
        correctionRecord = repository.save(correctionRecord);

        // Log correction
        auditLogger.logDataCorrection(
            originalRecordId.toString(),
            correctionRecord.getRecordId().toString(),
            correctedBy,
            reason.getCode(),
            reason.getDescription()
        );

        return correctionRecord;
    }

    /**
     * Create backdated entry with proper effective dating
     */
    public IntakePsdeRecord createBackdatedRecord(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate informationDate,
            Instant effectiveAsOf,
            IntakeDataCollectionStage collectionStage,
            String collectedBy,
            String backdatingReason) {

        // Validate backdating is allowed
        validateBackdatingRules(enrollmentId, informationDate, effectiveAsOf);

        // Create record
        IntakePsdeRecord record = IntakePsdeRecord.createForLifecycle(
            enrollmentId, clientId, informationDate, collectionStage, collectedBy
        );

        // Set backdated lifecycle metadata
        record.setEffectiveStart(effectiveAsOf);
        record.setLifecycleStatus(IntakePsdeLifecycleStatus.ACTIVE.name());
        record.setVersion(1);
        record.setIsBackdated(true);
        record.setBackdatingReason(backdatingReason);

        // Validate record
        var validationResult = validationService.validateIntakePsdeRecord(record);
        if (validationResult.hasErrors()) {
            throw new IntakePsdeValidationException("Backdated record validation failed",
                validationResult.errors().stream()
                    .map(e -> e.field() + ": " + e.message())
                    .toList());
        }

        // Handle timeline conflicts - supersede overlapping records
        handleTimelineConflicts(record);

        // Save record
        record = repository.save(record);

        // Log backdated creation
        auditLogger.logBackdatedEntry(
            record.getRecordId().toString(),
            collectedBy,
            informationDate.toString(),
            effectiveAsOf.toString(),
            backdatingReason
        );

        return record;
    }

    /**
     * Get active record for enrollment at specific point in time
     */
    public Optional<IntakePsdeRecord> getActiveRecordAsOf(
            ProgramEnrollmentId enrollmentId,
            Instant asOfTime) {
        return repository.findActiveByEnrollmentIdAsOf(enrollmentId, asOfTime);
    }

    /**
     * Get complete history for enrollment
     */
    public List<IntakePsdeRecord> getRecordHistory(ProgramEnrollmentId enrollmentId) {
        return repository.findHistoryByEnrollmentId(enrollmentId);
    }

    /**
     * Get audit trail for specific record
     */
    public IntakePsdeAuditTrail getAuditTrail(UUID recordId) {
        IntakePsdeRecord record = repository.findByRecordId(recordId)
            .orElseThrow(() -> new IntakePsdeNotFoundException("Record not found: " + recordId));

        List<IntakePsdeRecord> relatedRecords = repository.findAuditChain(recordId);
        return IntakePsdeAuditTrail.create(record, relatedRecords);
    }

    /**
     * Perform idempotent update with conflict resolution
     */
    public IntakePsdeRecord idempotentUpdate(
            UUID recordId,
            IntakePsdeUpdateRequest updateRequest,
            String updatedBy,
            String idempotencyKey) {

        // Check for existing operation with same idempotency key
        Optional<IntakePsdeRecord> existingResult = repository.findByIdempotencyKey(idempotencyKey);
        if (existingResult.isPresent()) {
            auditLogger.logIdempotentOperation(recordId.toString(), updatedBy, idempotencyKey, "DUPLICATE_DETECTED");
            return existingResult.get();
        }

        // Perform update
        IntakePsdeRecord result = updateRecord(recordId, updateRequest, updatedBy);

        // Store idempotency key
        result.setIdempotencyKey(idempotencyKey);
        result = repository.save(result);

        auditLogger.logIdempotentOperation(recordId.toString(), updatedBy, idempotencyKey, "OPERATION_EXECUTED");
        return result;
    }

    // Private helper methods

    private void validateNoConflictingActiveRecord(
            ProgramEnrollmentId enrollmentId,
            LocalDate informationDate,
            IntakeDataCollectionStage collectionStage) {

        Optional<IntakePsdeRecord> existing = repository.findActiveByEnrollmentAndDateAndStage(
            enrollmentId, informationDate, collectionStage
        );

        if (existing.isPresent()) {
            throw new IntakePsdeConflictException(
                "Active record already exists for enrollment " + enrollmentId +
                " on date " + informationDate + " for stage " + collectionStage
            );
        }
    }

    private IntakePsdeRecord createNewVersion(
            IntakePsdeRecord currentRecord,
            IntakePsdeUpdateRequest updateRequest,
            String updatedBy) {

        // Deep copy current record
        IntakePsdeRecord newVersion = currentRecord.createCopy();

        // Generate new ID and increment version
        newVersion.setRecordId(UUID.randomUUID());
        newVersion.setVersion(currentRecord.getVersion() + 1);
        newVersion.setUpdatedBy(updatedBy);

        // Apply updates
        updateRequest.applyTo(newVersion);

        return newVersion;
    }

    private IntakePsdeRecord createCorrectionVersion(
            IntakePsdeRecord originalRecord,
            IntakePsdeUpdateRequest correctionRequest,
            CorrectionReason reason,
            String correctedBy) {

        // Create copy with corrections applied
        IntakePsdeRecord correctionRecord = originalRecord.createCopy();
        correctionRecord.setRecordId(UUID.randomUUID());
        correctionRecord.setVersion(1); // Corrections start fresh version chain
        correctionRecord.setUpdatedBy(correctedBy);

        // Apply corrections
        correctionRequest.applyTo(correctionRecord);

        return correctionRecord;
    }

    private void validateBackdatingRules(
            ProgramEnrollmentId enrollmentId,
            LocalDate informationDate,
            Instant effectiveAsOf) {

        // HUD rules: Cannot backdate more than 30 days for most data elements
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        if (effectiveAsOf.isBefore(thirtyDaysAgo)) {
            throw new IntakePsdeValidationException(
                "Backdating more than 30 days requires supervisor approval"
            );
        }

        // Cannot backdate before enrollment start
        // TODO: Look up enrollment start date and validate
    }

    private void handleTimelineConflicts(IntakePsdeRecord newRecord) {
        // Find any records that overlap with the new record's effective period
        List<IntakePsdeRecord> overlappingRecords = repository.findOverlappingRecords(
            newRecord.getEnrollmentId(),
            newRecord.getEffectiveStart(),
            newRecord.getEffectiveEnd()
        );

        Instant now = Instant.now();
        for (IntakePsdeRecord overlapping : overlappingRecords) {
            if (overlapping.getLifecycleStatus().equals(IntakePsdeLifecycleStatus.ACTIVE.name())) {
                overlapping.setEffectiveEnd(newRecord.getEffectiveStart());
                overlapping.setLifecycleStatus(IntakePsdeLifecycleStatus.SUPERSEDED.name());
                overlapping.setSupersededAt(now);
                overlapping.setSupersededBy(newRecord.getCollectedBy());
                repository.save(overlapping);
            }
        }
    }

    // Exception classes
    public static class IntakePsdeNotFoundException extends RuntimeException {
        public IntakePsdeNotFoundException(String message) {
            super(message);
        }
    }

    public static class IntakePsdeValidationException extends RuntimeException {
        private final List<String> errors;

        public IntakePsdeValidationException(String message) {
            super(message);
            this.errors = List.of();
        }

        public IntakePsdeValidationException(String message, List<String> errors) {
            super(message);
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static class IntakePsdeConflictException extends RuntimeException {
        public IntakePsdeConflictException(String message) {
            super(message);
        }
    }

    // Supporting enums and classes
    public enum IntakePsdeLifecycleStatus {
        ACTIVE,
        SUPERSEDED,
        CORRECTED,
        DELETED
    }

    public enum CorrectionReason {
        DATA_ENTRY_ERROR("DATA_ENTRY", "Correction of data entry error"),
        CLIENT_CORRECTION("CLIENT_CORRECTION", "Client provided corrected information"),
        SYSTEM_ERROR("SYSTEM_ERROR", "System or technical error correction"),
        POLICY_CHANGE("POLICY_CHANGE", "Correction due to policy interpretation change"),
        AUDIT_FINDING("AUDIT", "Correction based on audit finding"),
        SUPERVISOR_REVIEW("SUPERVISOR", "Correction following supervisor review");

        private final String code;
        private final String description;

        CorrectionReason(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
}