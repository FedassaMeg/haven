package org.haven.programenrollment.infrastructure.persistence;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Disability Records
 * Maps to client_disability_records table
 */
@Entity
@Table(name = "client_disability_records")
public class JpaDisabilityEntity {
    
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
    private String stage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "disability_kind", nullable = false)
    private String disabilityKind;
    
    // Disability assessment fields
    @Enumerated(EnumType.STRING)
    @Column(name = "has_disability", nullable = false)
    private String hasDisability;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "expected_long_term")
    private String expectedLongTerm;
    
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
    
    protected JpaDisabilityEntity() {
        // JPA constructor
    }
    
    public JpaDisabilityEntity(DisabilityRecord record) {
        this.id = record.getRecordId();
        this.enrollmentId = record.getEnrollmentId().value();
        this.clientId = record.getClientId().value();
        this.informationDate = record.getInformationDate();
        this.stage = record.getStage().toDatabaseValue();
        this.disabilityKind = record.getDisabilityKind().toDatabaseValue();
        this.hasDisability = record.getHasDisability().toDatabaseValue();
        this.expectedLongTerm = record.getExpectedLongTerm() != null ? 
            record.getExpectedLongTerm().toDatabaseValue() : null;
        this.isCorrection = record.isCorrection();
        this.correctsRecordId = record.getCorrectsRecordId();
        this.collectedBy = record.getCollectedBy();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
    }
    
    public DisabilityRecord toDomainObject() {
        return reconstituteDomainObject();
    }
    
    private DisabilityRecord reconstituteDomainObject() {
        DataCollectionStage stageEnum = DataCollectionStage.fromDatabaseValue(stage);
        DisabilityKind kindEnum = DisabilityKind.fromDatabaseValue(disabilityKind);
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        HmisFivePoint hasDisabilityResponse = HmisFivePoint.fromDatabaseValue(hasDisability);
        
        DisabilityRecord record;
        
        if (isCorrection && correctsRecordId != null) {
            // This is a correction - create base record and mark as correction
            record = new DisabilityRecord();
            setPrivateFields(record, enrollmentDomainId, clientDomainId, informationDate, 
                            stageEnum, kindEnum, hasDisabilityResponse, collectedBy, true, correctsRecordId);
        } else {
            // Create based on stage
            switch (stageEnum) {
                case PROJECT_START -> record = DisabilityRecord.createAtProjectStart(
                    enrollmentDomainId, clientDomainId, informationDate, kindEnum, hasDisabilityResponse, collectedBy);
                case UPDATE -> record = DisabilityRecord.createUpdate(
                    enrollmentDomainId, clientDomainId, informationDate, kindEnum, hasDisabilityResponse, collectedBy);
                case PROJECT_EXIT -> record = DisabilityRecord.createAtProjectExit(
                    enrollmentDomainId, clientDomainId, informationDate, kindEnum, hasDisabilityResponse, collectedBy);
                default -> throw new IllegalArgumentException("Unknown stage: " + stage);
            }
        }
        
        // Update expected long-term if present
        if (expectedLongTerm != null) {
            HmisFivePoint expectedLongTermResponse = HmisFivePoint.fromDatabaseValue(expectedLongTerm);
            record.updateExpectedLongTerm(expectedLongTermResponse);
        }
        
        // Override the generated ID and timestamps with persisted values
        setPrivateAuditFields(record, id, createdAt, updatedAt);
        
        return record;
    }
    
    private void setPrivateFields(DisabilityRecord record, ProgramEnrollmentId enrollmentId,
                                ClientId clientId, LocalDate informationDate, DataCollectionStage stage,
                                DisabilityKind disabilityKind, HmisFivePoint hasDisability, 
                                String collectedBy, boolean isCorrection, UUID correctsRecordId) {
        try {
            var enrollmentField = DisabilityRecord.class.getDeclaredField("enrollmentId");
            enrollmentField.setAccessible(true);
            enrollmentField.set(record, enrollmentId);
            
            var clientField = DisabilityRecord.class.getDeclaredField("clientId");
            clientField.setAccessible(true);
            clientField.set(record, clientId);
            
            var dateField = DisabilityRecord.class.getDeclaredField("informationDate");
            dateField.setAccessible(true);
            dateField.set(record, informationDate);
            
            var stageField = DisabilityRecord.class.getDeclaredField("stage");
            stageField.setAccessible(true);
            stageField.set(record, stage);
            
            var kindField = DisabilityRecord.class.getDeclaredField("disabilityKind");
            kindField.setAccessible(true);
            kindField.set(record, disabilityKind);
            
            var disabilityField = DisabilityRecord.class.getDeclaredField("hasDisability");
            disabilityField.setAccessible(true);
            disabilityField.set(record, hasDisability);
            
            var collectedByField = DisabilityRecord.class.getDeclaredField("collectedBy");
            collectedByField.setAccessible(true);
            collectedByField.set(record, collectedBy);
            
            var correctionField = DisabilityRecord.class.getDeclaredField("isCorrection");
            correctionField.setAccessible(true);
            correctionField.set(record, isCorrection);
            
            var correctsField = DisabilityRecord.class.getDeclaredField("correctsRecordId");
            correctsField.setAccessible(true);
            correctsField.set(record, correctsRecordId);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct domain object", e);
        }
    }
    
    private void setPrivateAuditFields(DisabilityRecord record, UUID id, Instant createdAt, Instant updatedAt) {
        try {
            var idField = DisabilityRecord.class.getDeclaredField("recordId");
            idField.setAccessible(true);
            idField.set(record, id);
            
            var createdField = DisabilityRecord.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(record, createdAt);
            
            var updatedField = DisabilityRecord.class.getDeclaredField("updatedAt");
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
    public String getStage() { return stage; }
    public String getDisabilityKind() { return disabilityKind; }
    public String getHasDisability() { return hasDisability; }
    public String getExpectedLongTerm() { return expectedLongTerm; }
    public Boolean getIsCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}