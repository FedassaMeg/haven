package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service Episode aggregate for tracking individual service delivery
 * Represents a specific service provided to a client within a program enrollment
 */
public class ServiceEpisodeAggregate extends AggregateRoot<ServiceEpisodeId> {
    
    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    private CaseId caseId;
    private CodeableConcept serviceType;
    private CodeableConcept serviceCategory;
    private LocalDate serviceDate;
    private String providedBy;
    private UUID providerId;
    private String description;
    private Integer durationMinutes;
    private String location;
    private boolean isConfidential;
    private String outcome;
    private String followUpRequired;
    private String notes;
    private Instant createdAt;
    
    public static ServiceEpisodeAggregate provide(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                                 CaseId caseId, CodeableConcept serviceType,
                                                 CodeableConcept serviceCategory, LocalDate serviceDate,
                                                 String providedBy, UUID providerId, String description,
                                                 Integer durationMinutes, String location, boolean isConfidential) {
        ServiceEpisodeId episodeId = ServiceEpisodeId.generate();
        ServiceEpisodeAggregate episode = new ServiceEpisodeAggregate();
        episode.apply(new ServiceProvided(
            episodeId.value(),
            clientId.value(),
            enrollmentId.value(),
            caseId.value(),
            serviceType,
            serviceCategory,
            serviceDate,
            providedBy,
            providerId,
            description,
            durationMinutes,
            location,
            isConfidential,
            Instant.now()
        ));
        return episode;
    }
    
    public void updateOutcome(String outcome, String followUpRequired, String notes) {
        this.outcome = outcome;
        this.followUpRequired = followUpRequired;
        this.notes = notes;
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ServiceProvided e) {
            this.id = new ServiceEpisodeId(e.serviceEpisodeId());
            this.clientId = new ClientId(e.clientId());
            this.enrollmentId = new ProgramEnrollmentId(e.enrollmentId());
            this.caseId = new CaseId(e.caseId());
            this.serviceType = e.serviceType();
            this.serviceCategory = e.serviceCategory();
            this.serviceDate = e.serviceDate();
            this.providedBy = e.providedBy();
            this.providerId = e.providerId();
            this.description = e.description();
            this.durationMinutes = e.durationMinutes();
            this.location = e.location();
            this.isConfidential = e.isConfidential();
            this.createdAt = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public CaseId getCaseId() { return caseId; }
    public CodeableConcept getServiceType() { return serviceType; }
    public CodeableConcept getServiceCategory() { return serviceCategory; }
    public LocalDate getServiceDate() { return serviceDate; }
    public String getProvidedBy() { return providedBy; }
    public UUID getProviderId() { return providerId; }
    public String getDescription() { return description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getLocation() { return location; }
    public boolean isConfidential() { return isConfidential; }
    public String getOutcome() { return outcome; }
    public String getFollowUpRequired() { return followUpRequired; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    
    public boolean requiresConfidentialHandling() {
        return isConfidential;
    }
}