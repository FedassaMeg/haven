package org.haven.programenrollment.infrastructure.persistence;

import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Date of Engagement Records
 * Maps to date_of_engagement_records table
 */
@Entity
@Table(name = "date_of_engagement_records")
public class JpaDateOfEngagementEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "engagement_date", nullable = false)
    private LocalDate engagementDate;
    
    // Correction tracking
    @Column(name = "is_correction", nullable = false)
    private Boolean isCorrection = false;
    
    @Column(name = "corrects_record_id")
    private UUID correctsRecordId;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    protected JpaDateOfEngagementEntity() {
        // JPA constructor
    }
    
    public JpaDateOfEngagementEntity(DateOfEngagement engagement) {
        this.id = engagement.getRecordId();
        this.enrollmentId = engagement.getEnrollmentId().value();
        this.clientId = engagement.getClientId().value();
        this.engagementDate = engagement.getEngagementDate();
        this.isCorrection = engagement.isCorrection();
        this.correctsRecordId = engagement.getCorrectsRecordId();
        this.createdAt = engagement.getCreatedAt();
        this.updatedAt = engagement.getUpdatedAt();
        this.createdBy = engagement.getCreatedBy();
    }
    
    public DateOfEngagement toDomainObject() {
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        
        DateOfEngagement engagement;
        
        if (isCorrection && correctsRecordId != null) {
            // This is a correction - need to create base and correct
            DateOfEngagement original = new DateOfEngagement();
            setPrivateFields(original, enrollmentDomainId, clientDomainId, engagementDate, createdBy);
            engagement = DateOfEngagement.createCorrection(original, engagementDate, createdBy);
        } else {
            engagement = DateOfEngagement.create(enrollmentDomainId, clientDomainId, engagementDate, createdBy);
        }
        
        // Override generated audit fields with persisted values
        setPrivateAuditFields(engagement, id, createdAt, updatedAt);
        
        return engagement;
    }
    
    private void setPrivateFields(DateOfEngagement engagement, ProgramEnrollmentId enrollmentId,
                                ClientId clientId, LocalDate engagementDate, String createdBy) {
        try {
            var enrollmentField = DateOfEngagement.class.getDeclaredField("enrollmentId");
            enrollmentField.setAccessible(true);
            enrollmentField.set(engagement, enrollmentId);
            
            var clientField = DateOfEngagement.class.getDeclaredField("clientId");
            clientField.setAccessible(true);
            clientField.set(engagement, clientId);
            
            var dateField = DateOfEngagement.class.getDeclaredField("engagementDate");
            dateField.setAccessible(true);
            dateField.set(engagement, engagementDate);
            
            var createdByField = DateOfEngagement.class.getDeclaredField("createdBy");
            createdByField.setAccessible(true);
            createdByField.set(engagement, createdBy);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct domain object", e);
        }
    }
    
    private void setPrivateAuditFields(DateOfEngagement engagement, UUID id, Instant createdAt, Instant updatedAt) {
        try {
            var idField = DateOfEngagement.class.getDeclaredField("recordId");
            idField.setAccessible(true);
            idField.set(engagement, id);
            
            var createdField = DateOfEngagement.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(engagement, createdAt);
            
            var updatedField = DateOfEngagement.class.getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(engagement, updatedAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set audit fields", e);
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getEngagementDate() { return engagementDate; }
    public Boolean getIsCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
}