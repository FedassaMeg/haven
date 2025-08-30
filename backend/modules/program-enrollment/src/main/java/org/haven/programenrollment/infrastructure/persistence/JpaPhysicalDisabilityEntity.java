package org.haven.programenrollment.infrastructure.persistence;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Physical Disability Records
 * Maps to physical_disability_records table with HMIS-compliant structure
 */
@Entity
@Table(name = "physical_disability_records")
public class JpaPhysicalDisabilityEntity {
    
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
    
    // UDE 3.08 Physical Disability fields
    @Enumerated(EnumType.STRING)
    @Column(name = "physical_disability", nullable = false)
    private String physicalDisability;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "physical_expected_long_term")
    private String physicalExpectedLongTerm;
    
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
    
    protected JpaPhysicalDisabilityEntity() {
        // JPA constructor
    }
    
    public JpaPhysicalDisabilityEntity(PhysicalDisabilityRecord record) {
        this.id = record.getRecordId();
        this.enrollmentId = record.getEnrollmentId().value();
        this.clientId = record.getClientId().value();
        this.informationDate = record.getInformationDate();
        this.stage = record.getStage().toDatabaseValue();
        this.physicalDisability = record.getPhysicalDisability().toDatabaseValue();
        this.physicalExpectedLongTerm = record.getPhysicalExpectedLongTerm() != null ? 
            record.getPhysicalExpectedLongTerm().toDatabaseValue() : null;
        this.isCorrection = record.isCorrection();
        this.correctsRecordId = record.getCorrectsRecordId();
        this.collectedBy = record.getCollectedBy();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
    }
    
    public PhysicalDisabilityRecord toDomainObject() {
        return reconstituteDomainObject();
    }
    
    private PhysicalDisabilityRecord reconstituteDomainObject() {
        // Create appropriate factory method based on stage and correction status
        DataCollectionStage stageEnum = DataCollectionStage.fromDatabaseValue(stage);
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        HmisFivePointResponse physicalDisabilityResponse = HmisFivePointResponse.fromDatabaseValue(physicalDisability);
        
        PhysicalDisabilityRecord record;
        
        if (isCorrection && correctsRecordId != null) {
            // This is a correction - we need to reconstruct the original record first
            // For now, create a base record and mark it as correction
            record = new PhysicalDisabilityRecord();
            // Set fields directly via reflection or create a special constructor
            setPrivateFields(record, enrollmentDomainId, clientDomainId, informationDate, 
                            stageEnum, physicalDisabilityResponse, collectedBy, true, correctsRecordId);
        } else {
            // Create based on stage
            switch (stageEnum) {
                case PROJECT_START -> record = PhysicalDisabilityRecord.createAtProjectStart(
                    enrollmentDomainId, clientDomainId, informationDate, physicalDisabilityResponse, collectedBy);
                case UPDATE -> record = PhysicalDisabilityRecord.createUpdate(
                    enrollmentDomainId, clientDomainId, informationDate, physicalDisabilityResponse, collectedBy);
                case PROJECT_EXIT -> record = PhysicalDisabilityRecord.createAtProjectExit(
                    enrollmentDomainId, clientDomainId, informationDate, physicalDisabilityResponse, collectedBy);
                default -> throw new IllegalArgumentException("Unknown stage: " + stage);
            }
        }
        
        // Update physical expected long-term if present
        if (physicalExpectedLongTerm != null) {
            HmisFivePointResponse expectedLongTermResponse = HmisFivePointResponse.fromDatabaseValue(physicalExpectedLongTerm);
            record.updatePhysicalExpectedLongTerm(expectedLongTermResponse);
        }
        
        // Override the generated ID and timestamps with persisted values
        setPrivateAuditFields(record, id, createdAt, updatedAt);
        
        return record;
    }
    
    private void setPrivateFields(PhysicalDisabilityRecord record, ProgramEnrollmentId enrollmentId,
                                ClientId clientId, LocalDate informationDate, DataCollectionStage stage,
                                HmisFivePointResponse physicalDisability, String collectedBy,
                                boolean isCorrection, UUID correctsRecordId) {
        try {
            var enrollmentField = PhysicalDisabilityRecord.class.getDeclaredField("enrollmentId");
            enrollmentField.setAccessible(true);
            enrollmentField.set(record, enrollmentId);
            
            var clientField = PhysicalDisabilityRecord.class.getDeclaredField("clientId");
            clientField.setAccessible(true);
            clientField.set(record, clientId);
            
            var dateField = PhysicalDisabilityRecord.class.getDeclaredField("informationDate");
            dateField.setAccessible(true);
            dateField.set(record, informationDate);
            
            var stageField = PhysicalDisabilityRecord.class.getDeclaredField("stage");
            stageField.setAccessible(true);
            stageField.set(record, stage);
            
            var disabilityField = PhysicalDisabilityRecord.class.getDeclaredField("physicalDisability");
            disabilityField.setAccessible(true);
            disabilityField.set(record, physicalDisability);
            
            var collectedByField = PhysicalDisabilityRecord.class.getDeclaredField("collectedBy");
            collectedByField.setAccessible(true);
            collectedByField.set(record, collectedBy);
            
            var correctionField = PhysicalDisabilityRecord.class.getDeclaredField("isCorrection");
            correctionField.setAccessible(true);
            correctionField.set(record, isCorrection);
            
            var correctsField = PhysicalDisabilityRecord.class.getDeclaredField("correctsRecordId");
            correctsField.setAccessible(true);
            correctsField.set(record, correctsRecordId);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct domain object", e);
        }
    }
    
    private void setPrivateAuditFields(PhysicalDisabilityRecord record, UUID id, Instant createdAt, Instant updatedAt) {
        try {
            var idField = PhysicalDisabilityRecord.class.getDeclaredField("recordId");
            idField.setAccessible(true);
            idField.set(record, id);
            
            var createdField = PhysicalDisabilityRecord.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(record, createdAt);
            
            var updatedField = PhysicalDisabilityRecord.class.getDeclaredField("updatedAt");
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
    public String getPhysicalDisability() { return physicalDisability; }
    public String getPhysicalExpectedLongTerm() { return physicalExpectedLongTerm; }
    public Boolean getIsCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}