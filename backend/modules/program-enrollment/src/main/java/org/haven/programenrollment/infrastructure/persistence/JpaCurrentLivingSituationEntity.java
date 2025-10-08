package org.haven.programenrollment.infrastructure.persistence;

import org.haven.shared.vo.hmis.PriorLivingSituation;
import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Current Living Situation Records
 * Maps to current_living_situations table
 */
@Entity
@Table(name = "current_living_situations")
public class JpaCurrentLivingSituationEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "contact_date", nullable = false)
    private LocalDate contactDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "living_situation", nullable = false)
    private PriorLivingSituation livingSituation;
    
    @Column(name = "location_description")
    private String locationDescription;
    
    @Column(name = "verified_by")
    private String verifiedBy;
    
    @Column(name = "contact_time")
    private LocalTime contactTime;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    protected JpaCurrentLivingSituationEntity() {
        // JPA constructor
    }
    
    public JpaCurrentLivingSituationEntity(CurrentLivingSituation cls) {
        this.id = cls.getRecordId();
        this.enrollmentId = cls.getEnrollmentId().value();
        this.clientId = cls.getClientId().value();
        this.contactDate = cls.getContactDate();
        this.livingSituation = cls.getLivingSituation();
        this.locationDescription = cls.getLocationDescription();
        this.verifiedBy = cls.getVerifiedBy();
        this.contactTime = cls.getContactTime();
        this.durationMinutes = cls.getDurationMinutes();
        this.createdAt = cls.getCreatedAt();
        this.updatedAt = cls.getUpdatedAt();
        this.createdBy = cls.getCreatedBy();
    }
    
    public CurrentLivingSituation toDomainObject() {
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        
        CurrentLivingSituation cls = CurrentLivingSituation.createWithDetails(
            enrollmentDomainId,
            clientDomainId,
            contactDate,
            livingSituation,
            locationDescription,
            verifiedBy,
            createdBy
        );
        
        if (contactTime != null && durationMinutes != null) {
            cls.addTimeDetails(contactTime, durationMinutes);
        }
        
        // Override generated audit fields with persisted values
        setPrivateAuditFields(cls, id, createdAt, updatedAt);
        
        return cls;
    }
    
    private void setPrivateAuditFields(CurrentLivingSituation cls, UUID id, Instant createdAt, Instant updatedAt) {
        try {
            var idField = CurrentLivingSituation.class.getDeclaredField("recordId");
            idField.setAccessible(true);
            idField.set(cls, id);
            
            var createdField = CurrentLivingSituation.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(cls, createdAt);
            
            var updatedField = CurrentLivingSituation.class.getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(cls, updatedAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set audit fields", e);
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getContactDate() { return contactDate; }
    public PriorLivingSituation getLivingSituation() { return livingSituation; }
    public String getLocationDescription() { return locationDescription; }
    public String getVerifiedBy() { return verifiedBy; }
    public LocalTime getContactTime() { return contactTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
}