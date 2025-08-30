package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HMIS-compliant Physical Disability Record
 * Represents UDE 3.08 Physical Disability per HMIS FY2024 standards
 * Supports lifecycle data collection at project start, updates, and exit
 */
public class PhysicalDisabilityRecord {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate informationDate;
    private DataCollectionStage stage;
    
    // UDE 3.08 Physical Disability fields
    private HmisFivePointResponse physicalDisability;
    private HmisFivePointResponse physicalExpectedLongTerm;
    
    // Correction tracking
    private boolean isCorrection;
    private UUID correctsRecordId;
    
    // Audit fields
    private String collectedBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public PhysicalDisabilityRecord() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isCorrection = false;
    }
    
    /**
     * Create physical disability record at project start
     */
    public static PhysicalDisabilityRecord createAtProjectStart(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate entryDate,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        PhysicalDisabilityRecord record = new PhysicalDisabilityRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = entryDate;
        record.stage = DataCollectionStage.PROJECT_START;
        record.physicalDisability = physicalDisability;
        record.physicalExpectedLongTerm = HmisFivePointResponse.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create update record due to change in circumstances
     */
    public static PhysicalDisabilityRecord createUpdate(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate changeDate,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        PhysicalDisabilityRecord record = new PhysicalDisabilityRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = changeDate;
        record.stage = DataCollectionStage.UPDATE;
        record.physicalDisability = physicalDisability;
        record.physicalExpectedLongTerm = HmisFivePointResponse.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create record at project exit
     */
    public static PhysicalDisabilityRecord createAtProjectExit(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate exitDate,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        PhysicalDisabilityRecord record = new PhysicalDisabilityRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = exitDate;
        record.stage = DataCollectionStage.PROJECT_EXIT;
        record.physicalDisability = physicalDisability;
        record.physicalExpectedLongTerm = HmisFivePointResponse.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create correction record for an existing record
     */
    public static PhysicalDisabilityRecord createCorrection(
            PhysicalDisabilityRecord originalRecord,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        PhysicalDisabilityRecord correction = new PhysicalDisabilityRecord();
        correction.enrollmentId = originalRecord.enrollmentId;
        correction.clientId = originalRecord.clientId;
        correction.informationDate = originalRecord.informationDate;
        correction.stage = originalRecord.stage;
        correction.physicalDisability = physicalDisability;
        correction.physicalExpectedLongTerm = originalRecord.physicalExpectedLongTerm;
        correction.isCorrection = true;
        correction.correctsRecordId = originalRecord.recordId;
        correction.collectedBy = collectedBy;
        
        return correction;
    }
    
    /**
     * Update physical expected long-term response
     */
    public void updatePhysicalExpectedLongTerm(HmisFivePointResponse expectedLongTerm) {
        this.physicalExpectedLongTerm = expectedLongTerm;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update both physical disability responses
     */
    public void updatePhysicalDisabilityStatus(
            HmisFivePointResponse physicalDisability,
            HmisFivePointResponse expectedLongTerm) {
        this.physicalDisability = physicalDisability;
        this.physicalExpectedLongTerm = expectedLongTerm;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if record meets HMIS data quality standards
     */
    public boolean meetsDataQuality() {
        // Physical disability must be a known response
        if (physicalDisability == null || !physicalDisability.isKnownResponse()) {
            return false;
        }
        
        // If physical disability = YES, expected long-term is required
        if (physicalDisability == HmisFivePointResponse.YES) {
            return physicalExpectedLongTerm != null && physicalExpectedLongTerm.isKnownResponse();
        }
        
        // If physical disability = NO, expected long-term should not be collected
        if (physicalDisability == HmisFivePointResponse.NO) {
            return physicalExpectedLongTerm == null || 
                   physicalExpectedLongTerm == HmisFivePointResponse.DATA_NOT_COLLECTED;
        }
        
        return true;
    }
    
    /**
     * Check if this record indicates a disabling condition per UDE 3.08
     * For HMIS purposes: physical disability = YES AND expected long-term = YES
     */
    public boolean indicatesDisablingCondition() {
        return physicalDisability == HmisFivePointResponse.YES && 
               physicalExpectedLongTerm == HmisFivePointResponse.YES;
    }
    
    /**
     * Check if client has physical disability (regardless of long-term expectation)
     */
    public boolean hasPhysicalDisability() {
        return physicalDisability == HmisFivePointResponse.YES;
    }
    
    /**
     * Check if physical disability is expected to be long-term
     */
    public boolean isExpectedLongTerm() {
        return physicalExpectedLongTerm == HmisFivePointResponse.YES;
    }
    
    /**
     * Validate that information date is not in future
     */
    public boolean isInformationDateValid() {
        return informationDate != null && !informationDate.isAfter(LocalDate.now());
    }
    
    /**
     * Check if this is a project start record
     */
    public boolean isProjectStartRecord() {
        return stage != null && stage.isProjectStart();
    }
    
    /**
     * Check if this is an update record
     */
    public boolean isUpdateRecord() {
        return stage != null && stage.isUpdate();
    }
    
    /**
     * Check if this is a project exit record
     */
    public boolean isProjectExitRecord() {
        return stage != null && stage.isProjectExit();
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public DataCollectionStage getStage() { return stage; }
    public HmisFivePointResponse getPhysicalDisability() { return physicalDisability; }
    public HmisFivePointResponse getPhysicalExpectedLongTerm() { return physicalExpectedLongTerm; }
    public boolean isCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhysicalDisabilityRecord that = (PhysicalDisabilityRecord) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}