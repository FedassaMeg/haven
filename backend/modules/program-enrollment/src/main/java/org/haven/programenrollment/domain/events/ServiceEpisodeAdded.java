package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ServiceEpisodeAdded extends DomainEvent {
    private final UUID enrollmentId;
    private final UUID serviceEpisodeId;
    private final CodeableConcept serviceType;
    private final LocalDate serviceDate;
    private final String providedBy;
    private final String description;

    public ServiceEpisodeAdded(
        UUID enrollmentId,
        UUID serviceEpisodeId,
        CodeableConcept serviceType,
        LocalDate serviceDate,
        String providedBy,
        String description,
        Instant occurredAt
    ) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (serviceEpisodeId == null) throw new IllegalArgumentException("Service episode ID cannot be null");
        if (serviceType == null) throw new IllegalArgumentException("Service type cannot be null");
        if (serviceDate == null) throw new IllegalArgumentException("Service date cannot be null");
        if (providedBy == null) throw new IllegalArgumentException("Provider cannot be null");

        this.enrollmentId = enrollmentId;
        this.serviceEpisodeId = serviceEpisodeId;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.providedBy = providedBy;
        this.description = description;
    }

    @Override
    public String eventType() {
        return "ServiceEpisodeAdded";
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public UUID serviceEpisodeId() {
        return serviceEpisodeId;
    }

    public CodeableConcept serviceType() {
        return serviceType;
    }

    public LocalDate serviceDate() {
        return serviceDate;
    }

    public String providedBy() {
        return providedBy;
    }

    public String description() {
        return description;
    }

    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getServiceEpisodeId() { return serviceEpisodeId; }
    public CodeableConcept getServiceType() { return serviceType; }
    public LocalDate getServiceDate() { return serviceDate; }
    public String getProvidedBy() { return providedBy; }
    public String getDescription() { return description; }
}