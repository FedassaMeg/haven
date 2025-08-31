package org.haven.programenrollment.infrastructure.persistence;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Domestic Violence Records
 * Maps to dv_records table
 */
@Entity
@Table(name = "dv_records")
public class JpaDvEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "information_date", nullable = false)
    private LocalDate informationDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private DataCollectionStage stage;
    
    // DV fields
    @Enumerated(EnumType.STRING)
    @Column(name = "dv_history", nullable = false)
    private HmisFivePoint dvHistory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currently_fleeing")
    private HmisFivePoint currentlyFleeing;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "when_experienced")
    private DomesticViolenceRecency whenExperienced;
    
    // Correction tracking
    @Column(name = "is_correction", nullable = false)
    private Boolean isCorrection = false;
    
    @Column(name = "corrects_record_id")
    private UUID correctsRecordId;
    
    // Audit fields
    @Column(name = "collected_by", nullable = false)
    private String collectedBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected JpaDvEntity() {
        // JPA constructor
    }
    
    public JpaDvEntity(DvRecord record) {
        this.id = record.getRecordId();
        this.enrollmentId = record.getEnrollmentId().value();
        this.clientId = record.getClientId().value();
        this.informationDate = record.getInformationDate();
        this.stage = record.getStage();
        this.dvHistory = record.getDvHistory();
        this.currentlyFleeing = record.getCurrentlyFleeing();
        this.whenExperienced = record.getWhenExperienced();
        this.isCorrection = record.isCorrection();
        this.correctsRecordId = record.getCorrectsRecordId();
        this.collectedBy = record.getCollectedBy();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
    }
    
    public DvRecord toDomainObject() {
        return reconstituteDomainObject();
    }
    
    private DvRecord reconstituteDomainObject() {
        DataCollectionStage stageEnum = stage;
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        HmisFivePoint dvHistoryResponse = dvHistory;
        
        DvRecord record;
        
        if (isCorrection && correctsRecordId != null) {
            // This is a correction - create base record and mark as correction
            record = new DvRecord();
            setPrivateFields(record, enrollmentDomainId, clientDomainId, informationDate, 
                            stageEnum, dvHistoryResponse, collectedBy, true, correctsRecordId);
        } else {
            // Create based on stage
            switch (stageEnum) {
                case PROJECT_START -> record = DvRecord.createAtProjectStart(
                    enrollmentDomainId, clientDomainId, informationDate, dvHistoryResponse, collectedBy);
                case UPDATE -> record = DvRecord.createUpdate(
                    enrollmentDomainId, clientDomainId, informationDate, dvHistoryResponse, collectedBy);
                case PROJECT_EXIT -> record = DvRecord.createAtProjectExit(
                    enrollmentDomainId, clientDomainId, informationDate, dvHistoryResponse, collectedBy);
                default -> throw new IllegalArgumentException("Unknown stage: " + stage);
            }
        }
        
        // Update currently fleeing if present
        if (currentlyFleeing != null) {
            record.updateCurrentlyFleeing(currentlyFleeing);
        }
        
        // Update when experienced if present
        if (whenExperienced != null) {
            record.updateWhenExperienced(whenExperienced);
        }
        
        // Override the generated ID and timestamps with persisted values
        setPrivateAuditFields(record, id, createdAt, updatedAt);
        
        return record;
    }
    
    private void setPrivateFields(DvRecord record, ProgramEnrollmentId enrollmentId,
                                ClientId clientId, LocalDate informationDate, DataCollectionStage stage,
                                HmisFivePoint dvHistory, String collectedBy, 
                                boolean isCorrection, UUID correctsRecordId) {
        try {
            var enrollmentField = DvRecord.class.getDeclaredField("enrollmentId");
            enrollmentField.setAccessible(true);
            enrollmentField.set(record, enrollmentId);
            
            var clientField = DvRecord.class.getDeclaredField("clientId");
            clientField.setAccessible(true);
            clientField.set(record, clientId);
            
            var dateField = DvRecord.class.getDeclaredField("informationDate");
            dateField.setAccessible(true);
            dateField.set(record, informationDate);
            
            var stageField = DvRecord.class.getDeclaredField("stage");
            stageField.setAccessible(true);
            stageField.set(record, stage);
            
            var dvHistoryField = DvRecord.class.getDeclaredField("dvHistory");
            dvHistoryField.setAccessible(true);
            dvHistoryField.set(record, dvHistory);
            
            var collectedByField = DvRecord.class.getDeclaredField("collectedBy");
            collectedByField.setAccessible(true);
            collectedByField.set(record, collectedBy);
            
            var correctionField = DvRecord.class.getDeclaredField("isCorrection");
            correctionField.setAccessible(true);
            correctionField.set(record, isCorrection);
            
            var correctsField = DvRecord.class.getDeclaredField("correctsRecordId");
            correctsField.setAccessible(true);
            correctsField.set(record, correctsRecordId);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct domain object", e);
        }
    }
    
    private void setPrivateAuditFields(DvRecord record, UUID id, Instant createdAt, Instant updatedAt) {
        try {
            var idField = DvRecord.class.getDeclaredField("recordId");
            idField.setAccessible(true);
            idField.set(record, id);
            
            var createdField = DvRecord.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(record, createdAt);
            
            var updatedField = DvRecord.class.getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(record, updatedAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set audit fields", e);
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public DataCollectionStage getStage() { return stage; }
    public HmisFivePoint getDvHistory() { return dvHistory; }
    public HmisFivePoint getCurrentlyFleeing() { return currentlyFleeing; }
    public DomesticViolenceRecency getWhenExperienced() { return whenExperienced; }
    public Boolean getIsCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}