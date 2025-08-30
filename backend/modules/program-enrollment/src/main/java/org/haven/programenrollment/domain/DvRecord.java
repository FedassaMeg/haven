package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HMIS-compliant Domestic Violence Record
 * Represents HMIS Element 4.11 Domestic Violence per FY2024 standards
 * Supports lifecycle data collection with enhanced privacy controls
 */
public class DvRecord {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate informationDate;
    private DataCollectionStage stage;
    
    // DV assessment fields (HMIS 4.11)
    private HmisFivePoint dvHistory;
    private HmisFivePoint currentlyFleeing;
    private DomesticViolenceRecency whenExperienced;
    
    // Correction tracking
    private boolean isCorrection;
    private UUID correctsRecordId;
    
    // Audit fields
    private String collectedBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public DvRecord() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isCorrection = false;
    }
    
    /**
     * Create DV record at project start
     */
    public static DvRecord createAtProjectStart(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate entryDate,
            HmisFivePoint dvHistory,
            String collectedBy) {
        
        DvRecord record = new DvRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = entryDate;
        record.stage = DataCollectionStage.PROJECT_START;
        record.dvHistory = dvHistory;
        record.currentlyFleeing = HmisFivePoint.DATA_NOT_COLLECTED;
        record.whenExperienced = DomesticViolenceRecency.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create update record due to change in circumstances
     */
    public static DvRecord createUpdate(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate changeDate,
            HmisFivePoint dvHistory,
            String collectedBy) {
        
        DvRecord record = new DvRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = changeDate;
        record.stage = DataCollectionStage.UPDATE;
        record.dvHistory = dvHistory;
        record.currentlyFleeing = HmisFivePoint.DATA_NOT_COLLECTED;
        record.whenExperienced = DomesticViolenceRecency.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create record at project exit
     */
    public static DvRecord createAtProjectExit(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate exitDate,
            HmisFivePoint dvHistory,
            String collectedBy) {
        
        DvRecord record = new DvRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = exitDate;
        record.stage = DataCollectionStage.PROJECT_EXIT;
        record.dvHistory = dvHistory;
        record.currentlyFleeing = HmisFivePoint.DATA_NOT_COLLECTED;
        record.whenExperienced = DomesticViolenceRecency.DATA_NOT_COLLECTED;
        record.collectedBy = collectedBy;
        
        return record;
    }
    
    /**
     * Create correction record for an existing record
     */
    public static DvRecord createCorrection(
            DvRecord originalRecord,
            HmisFivePoint dvHistory,
            String collectedBy) {
        
        DvRecord correction = new DvRecord();
        correction.enrollmentId = originalRecord.enrollmentId;
        correction.clientId = originalRecord.clientId;
        correction.informationDate = originalRecord.informationDate;
        correction.stage = originalRecord.stage;
        correction.dvHistory = dvHistory;
        correction.currentlyFleeing = originalRecord.currentlyFleeing;
        correction.whenExperienced = originalRecord.whenExperienced;
        correction.isCorrection = true;
        correction.correctsRecordId = originalRecord.recordId;
        correction.collectedBy = collectedBy;
        
        return correction;
    }
    
    /**
     * Update currently fleeing status
     */
    public void updateCurrentlyFleeing(HmisFivePoint currentlyFleeing) {
        this.currentlyFleeing = currentlyFleeing;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update when DV was experienced
     */
    public void updateWhenExperienced(DomesticViolenceRecency whenExperienced) {
        this.whenExperienced = whenExperienced;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update complete DV status
     */
    public void updateDvStatus(
            HmisFivePoint dvHistory,
            HmisFivePoint currentlyFleeing,
            DomesticViolenceRecency whenExperienced) {
        this.dvHistory = dvHistory;
        this.currentlyFleeing = currentlyFleeing;
        this.whenExperienced = whenExperienced;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if record meets HMIS data quality standards
     */
    public boolean meetsDataQuality() {
        // DV history must be a known response
        if (dvHistory == null || !dvHistory.isKnownResponse()) {
            return false;
        }
        
        // If DV history = YES, recency should be collected
        if (dvHistory.isYes()) {
            if (whenExperienced == null || !whenExperienced.isKnownResponse()) {
                return false;
            }
            // Currently fleeing should also be collected if DV = YES
            if (currentlyFleeing == null || !currentlyFleeing.isKnownResponse()) {
                return false;
            }
        }
        
        // If DV history = NO, recency and fleeing should not be collected
        if (dvHistory.isNo()) {
            if (whenExperienced != null && whenExperienced != DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                return false;
            }
            if (currentlyFleeing != null && currentlyFleeing != HmisFivePoint.DATA_NOT_COLLECTED) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if client has DV history
     */
    public boolean hasDvHistory() {
        return dvHistory != null && dvHistory.isYes();
    }
    
    /**
     * Check if client is currently fleeing DV
     */
    public boolean isCurrentlyFleeing() {
        return currentlyFleeing != null && currentlyFleeing.isYes();
    }
    
    /**
     * Check if DV was experienced recently (within 6 months)
     */
    public boolean isDvRecent() {
        return whenExperienced != null && whenExperienced.isRecent();
    }
    
    /**
     * Check if DV was experienced very recently (within 3 months)
     */
    public boolean isDvVeryRecent() {
        return whenExperienced != null && whenExperienced.isVeryRecent();
    }
    
    /**
     * Check if client requires enhanced privacy/safety measures
     */
    public boolean requiresEnhancedSafety() {
        return hasDvHistory() && (isCurrentlyFleeing() || isDvVeryRecent());
    }
    
    /**
     * Check if this is a high-risk DV situation
     */
    public boolean isHighRisk() {
        return isCurrentlyFleeing() && isDvVeryRecent();
    }
    
    /**
     * Validate that information date is not in future
     */
    public boolean isInformationDateValid() {
        return informationDate != null && !informationDate.isAfter(LocalDate.now());
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
    public HmisFivePoint getDvHistory() { return dvHistory; }
    public HmisFivePoint getCurrentlyFleeing() { return currentlyFleeing; }
    public DomesticViolenceRecency getWhenExperienced() { return whenExperienced; }
    public boolean isCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DvRecord that = (DvRecord) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}