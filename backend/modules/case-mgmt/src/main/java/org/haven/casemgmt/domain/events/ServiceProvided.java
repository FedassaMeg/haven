package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ServiceProvided extends DomainEvent {
    private final UUID clientId;
    private final UUID enrollmentId;
    private final UUID caseId;
    private final CodeableConcept serviceType;
    private final CodeableConcept serviceCategory;
    private final LocalDate serviceDate;
    private final String providedBy;
    private final UUID providerId;
    private final String description;
    private final Integer durationMinutes;
    private final String location;
    private final boolean isConfidential;

    public ServiceProvided(UUID serviceEpisodeId, UUID clientId, UUID enrollmentId, UUID caseId, CodeableConcept serviceType, CodeableConcept serviceCategory, LocalDate serviceDate, String providedBy, UUID providerId, String description, Integer durationMinutes, String location, boolean isConfidential, Instant occurredAt) {
        super(serviceEpisodeId, occurredAt != null ? occurredAt : Instant.now());
        if (serviceEpisodeId == null) throw new IllegalArgumentException("Service episode ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (serviceType == null) throw new IllegalArgumentException("Service type cannot be null");
        if (serviceDate == null) throw new IllegalArgumentException("Service date cannot be null");
        if (providedBy == null || providedBy.trim().isEmpty()) throw new IllegalArgumentException("Provider cannot be null or empty");

        this.clientId = clientId;
        this.enrollmentId = enrollmentId;
        this.caseId = caseId;
        this.serviceType = serviceType;
        this.serviceCategory = serviceCategory;
        this.serviceDate = serviceDate;
        this.providedBy = providedBy;
        this.providerId = providerId;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.isConfidential = isConfidential;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public UUID caseId() {
        return caseId;
    }

    public CodeableConcept serviceType() {
        return serviceType;
    }

    public CodeableConcept serviceCategory() {
        return serviceCategory;
    }

    public LocalDate serviceDate() {
        return serviceDate;
    }

    public String providedBy() {
        return providedBy;
    }

    public UUID providerId() {
        return providerId;
    }

    public String description() {
        return description;
    }

    public Integer durationMinutes() {
        return durationMinutes;
    }

    public String location() {
        return location;
    }

    public boolean isConfidential() {
        return isConfidential;
    }


    public UUID serviceEpisodeId() {
        return getAggregateId();
    }

    @Override
    public String eventType() {
        return "ServiceProvided";
    }

    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getCaseId() { return caseId; }
    public CodeableConcept getServiceType() { return serviceType; }
    public CodeableConcept getServiceCategory() { return serviceCategory; }
    public LocalDate getServiceDate() { return serviceDate; }
    public String getProvidedBy() { return providedBy; }
    public UUID getProviderId() { return providerId; }
    public String getDescription() { return description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getLocation() { return location; }
    public boolean IsConfidential() { return isConfidential; }
}