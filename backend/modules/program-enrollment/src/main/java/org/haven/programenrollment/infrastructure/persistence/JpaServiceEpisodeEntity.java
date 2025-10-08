package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.programenrollment.domain.ServiceEpisode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "service_episodes", schema = "haven")
public class JpaServiceEpisodeEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private JpaProgramEnrollmentEntity enrollment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceEpisodeType serviceType;
    
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;
    
    @Column(name = "service_start_time")
    private LocalTime serviceStartTime;
    
    @Column(name = "service_end_time")
    private LocalTime serviceEndTime;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "provided_by")
    private UUID providedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private ServiceOutcome outcome;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "is_billable")
    private Boolean isBillable;
    
    @Column(name = "billing_code")
    private String billingCode;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    // Constructors
    protected JpaServiceEpisodeEntity() {}
    
    public JpaServiceEpisodeEntity(UUID id, JpaProgramEnrollmentEntity enrollment,
                                 ServiceEpisodeType serviceType, LocalDate serviceDate,
                                 UUID providedBy) {
        this.id = id;
        this.enrollment = enrollment;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.providedBy = providedBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Enums
    public enum ServiceEpisodeType {
        CASE_MANAGEMENT,
        COUNSELING_INDIVIDUAL,
        COUNSELING_GROUP,
        COUNSELING_FAMILY,
        CRISIS_INTERVENTION,
        SAFETY_PLANNING,
        LEGAL_ADVOCACY,
        COURT_ACCOMPANIMENT,
        HOUSING_ASSISTANCE,
        FINANCIAL_ASSISTANCE,
        TRANSPORTATION,
        CHILDCARE,
        EDUCATION_SERVICES,
        EMPLOYMENT_SERVICES,
        HEALTHCARE_SERVICES,
        MENTAL_HEALTH_SERVICES,
        SUBSTANCE_ABUSE_SERVICES,
        OTHER
    }
    
    public enum ServiceOutcome {
        COMPLETED,
        PARTIALLY_COMPLETED,
        NOT_COMPLETED,
        CANCELLED,
        NO_SHOW
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public JpaProgramEnrollmentEntity getEnrollment() { return enrollment; }
    public void setEnrollment(JpaProgramEnrollmentEntity enrollment) { this.enrollment = enrollment; }
    
    public ServiceEpisodeType getServiceType() { return serviceType; }
    public void setServiceType(ServiceEpisodeType serviceType) { this.serviceType = serviceType; }
    
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    
    public LocalTime getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(LocalTime serviceStartTime) { this.serviceStartTime = serviceStartTime; }
    
    public LocalTime getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(LocalTime serviceEndTime) { this.serviceEndTime = serviceEndTime; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public UUID getProvidedBy() { return providedBy; }
    public void setProvidedBy(UUID providedBy) { this.providedBy = providedBy; }
    
    public ServiceOutcome getOutcome() { return outcome; }
    public void setOutcome(ServiceOutcome outcome) { this.outcome = outcome; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Boolean getIsBillable() { return isBillable; }
    public void setIsBillable(Boolean isBillable) { this.isBillable = isBillable; }
    
    public String getBillingCode() { return billingCode; }
    public void setBillingCode(String billingCode) { this.billingCode = billingCode; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    
    // Conversion methods - placeholder implementations
    public static JpaServiceEpisodeEntity fromDomainObject(ServiceEpisode episode) {
        JpaServiceEpisodeEntity entity = new JpaServiceEpisodeEntity();
        // TODO: Implement proper conversion when domain objects are finalized
        return entity;
    }
    
    public ServiceEpisode toDomainObject() {
        // TODO: Implement proper conversion when domain objects are finalized
        throw new UnsupportedOperationException("ServiceEpisode conversion not yet implemented");
    }
}