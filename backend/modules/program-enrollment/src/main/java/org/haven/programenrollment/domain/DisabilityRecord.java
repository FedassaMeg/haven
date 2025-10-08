package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HMIS-compliant Disability Record
 * Represents UDE 3.08-3.13 Disability data per HMIS FY2024 standards
 * Supports all disability kinds with lifecycle data collection
 */
public class DisabilityRecord {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate informationDate;
    private DataCollectionStage stage;
    private DisabilityKind disabilityKind;
    
    // Disability assessment fields
    private HmisFivePoint hasDisability;
    private HmisFivePoint expectedLongTerm;
    
    // Correction tracking
    private boolean isCorrection;
    private UUID correctsRecordId;
    
    // Audit fields
    private String collectedBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public DisabilityRecord() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isCorrection = false;
    }
    
    /**
     * Create disability record at project start
     */
    public static DisabilityRecord createAtProjectStart(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate entryDate,
            DisabilityKind disabilityKind,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        DisabilityRecord record = new DisabilityRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = entryDate;
        record.stage = DataCollectionStage.PROJECT_START;
        record.disabilityKind = disabilityKind;
        record.hasDisability = hasDisability;
        record.expectedLongTerm = HmisFivePoint.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create update record due to change in circumstances
     */
    public static DisabilityRecord createUpdate(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate changeDate,
            DisabilityKind disabilityKind,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        DisabilityRecord record = new DisabilityRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = changeDate;
        record.stage = DataCollectionStage.UPDATE;
        record.disabilityKind = disabilityKind;
        record.hasDisability = hasDisability;
        record.expectedLongTerm = HmisFivePoint.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create record at project exit
     */
    public static DisabilityRecord createAtProjectExit(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate exitDate,
            DisabilityKind disabilityKind,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        DisabilityRecord record = new DisabilityRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = exitDate;
        record.stage = DataCollectionStage.PROJECT_EXIT;
        record.disabilityKind = disabilityKind;
        record.hasDisability = hasDisability;
        record.expectedLongTerm = HmisFivePoint.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create correction record for an existing record
     */
    public static DisabilityRecord createCorrection(
            DisabilityRecord originalRecord,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        DisabilityRecord correction = new DisabilityRecord();
        correction.enrollmentId = originalRecord.enrollmentId;
        correction.clientId = originalRecord.clientId;
        correction.informationDate = originalRecord.informationDate;
        correction.stage = originalRecord.stage;
        correction.disabilityKind = originalRecord.disabilityKind;
        correction.hasDisability = hasDisability;
        correction.expectedLongTerm = originalRecord.expectedLongTerm;
        correction.isCorrection = true;
        correction.correctsRecordId = originalRecord.recordId;
        correction.collectedBy = collectedBy;
        
        return correction;
    }
    
    /**
     * Update expected long-term response
     */
    public void updateExpectedLongTerm(HmisFivePoint expectedLongTerm) {
        this.expectedLongTerm = expectedLongTerm;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update both disability responses
     */
    public void updateDisabilityStatus(HmisFivePoint hasDisability, HmisFivePoint expectedLongTerm) {
        this.hasDisability = hasDisability;
        this.expectedLongTerm = expectedLongTerm;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if record meets HMIS data quality standards
     */
    public boolean meetsDataQuality() {
        // Has disability must be a known response
        if (hasDisability == null || !hasDisability.isKnownResponse()) {
            return false;
        }
        
        // If has disability = YES, expected long-term is required
        if (hasDisability.isYes()) {
            return expectedLongTerm != null && expectedLongTerm.isKnownResponse();
        }
        
        // If has disability = NO, expected long-term should not be collected
        if (hasDisability.isNo()) {
            return expectedLongTerm == null || expectedLongTerm == HmisFivePoint.DATA_NOT_COLLECTED;
        }
        
        return true;
    }
    
    /**
     * Check if this record indicates a disabling condition
     * For HMIS purposes: disability = YES AND expected long-term = YES
     */
    public boolean indicatesDisablingCondition() {
        return hasDisability != null && hasDisability.isYes() && 
               expectedLongTerm != null && expectedLongTerm.isYes();
    }
    
    /**
     * Check if client has this disability
     */
    public boolean hasDisability() {
        return hasDisability != null && hasDisability.isYes();
    }
    
    /**
     * Check if disability is expected to be long-term
     */
    public boolean isExpectedLongTerm() {
        return expectedLongTerm != null && expectedLongTerm.isYes();
    }
    
    /**
     * Validate that information date is not in future
     */
    public boolean isInformationDateValid() {
        return informationDate != null && !informationDate.isAfter(LocalDate.now());
    }
    
    /**
     * Check if this is a behavioral health disability
     */
    public boolean isBehavioralHealthDisability() {
        return disabilityKind != null && disabilityKind.isBehavioralHealth();
    }
    
    /**
     * Check if this is a medical disability
     */
    public boolean isMedicalDisability() {
        return disabilityKind != null && disabilityKind.isMedical();
    }
    
    // Stage check methods
    public boolean isProjectStartRecord() {
        return stage != null && stage.isProjectStart();
    }
    
    public boolean isUpdateRecord() {
        return stage != null && stage.isUpdate();
    }
    
    public boolean isProjectExitRecord() {
        return stage != null && stage.isProjectExit();
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public DataCollectionStage getStage() { return stage; }
    public DisabilityKind getDisabilityKind() { return disabilityKind; }
    public HmisFivePoint getHasDisability() { return hasDisability; }
    public HmisFivePoint getExpectedLongTerm() { return expectedLongTerm; }
    public boolean isCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisabilityRecord that = (DisabilityRecord) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}