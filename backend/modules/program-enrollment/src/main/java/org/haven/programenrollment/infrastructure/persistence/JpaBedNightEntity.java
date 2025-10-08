package org.haven.programenrollment.infrastructure.persistence;

import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Bed Night Records
 * Maps to bed_nights table
 */
@Entity
@Table(name = "bed_nights")
public class JpaBedNightEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "bed_night_date", nullable = false)
    private LocalDate bedNightDate;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    protected JpaBedNightEntity() {
        // JPA constructor
    }
    
    public JpaBedNightEntity(BedNight bedNight) {
        this.id = bedNight.getRecordId();
        this.enrollmentId = bedNight.getEnrollmentId().value();
        this.clientId = bedNight.getClientId().value();
        this.bedNightDate = bedNight.getBedNightDate();
        this.createdAt = bedNight.getCreatedAt();
        this.createdBy = bedNight.getCreatedBy();
    }
    
    public BedNight toDomainObject() {
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        
        BedNight bedNight = BedNight.create(enrollmentDomainId, clientDomainId, bedNightDate, createdBy);
        
        // Override generated audit fields with persisted values
        setPrivateAuditFields(bedNight, id, createdAt);
        
        return bedNight;
    }
    
    private void setPrivateAuditFields(BedNight bedNight, UUID id, Instant createdAt) {
        try {
            var idField = BedNight.class.getDeclaredField("recordId");
            idField.setAccessible(true);
            idField.set(bedNight, id);
            
            var createdField = BedNight.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(bedNight, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set audit fields", e);
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getBedNightDate() { return bedNightDate; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}