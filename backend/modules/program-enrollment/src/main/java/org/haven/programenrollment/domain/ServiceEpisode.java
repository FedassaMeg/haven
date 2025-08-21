package org.haven.programenrollment.domain;

import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import java.time.Instant;
import java.time.LocalDate;

/**
 * HMIS-aligned service episode entity
 * Tracks individual service interactions within a program enrollment
 */
public class ServiceEpisode {
    private ServiceEpisodeId id;
    private CodeableConcept serviceType;
    private LocalDate serviceDate;
    private Period servicePeriod;
    private String description;
    private ServiceOutcome outcome;
    private String notes;
    private String providedBy;
    private Instant recordedAt;
    
    public ServiceEpisode(ServiceEpisodeId id, CodeableConcept serviceType, 
                         LocalDate serviceDate, String providedBy) {
        this.id = id;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.providedBy = providedBy;
        this.recordedAt = Instant.now();
    }
    
    public void updateOutcome(ServiceOutcome outcome, String notes) {
        this.outcome = outcome;
        this.notes = notes;
    }
    
    public void setServicePeriod(Period period) {
        this.servicePeriod = period;
    }
    
    public enum ServiceOutcome {
        COMPLETED,
        PARTIALLY_COMPLETED,
        NOT_COMPLETED,
        CANCELLED,
        NO_SHOW
    }
    
    // Getters
    public ServiceEpisodeId getId() { return id; }
    public CodeableConcept getServiceType() { return serviceType; }
    public LocalDate getServiceDate() { return serviceDate; }
    public Period getServicePeriod() { return servicePeriod; }
    public String getDescription() { return description; }
    public ServiceOutcome getOutcome() { return outcome; }
    public String getNotes() { return notes; }
    public String getProvidedBy() { return providedBy; }
    public Instant getRecordedAt() { return recordedAt; }
}