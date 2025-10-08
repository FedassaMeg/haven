package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ServiceEpisodeLinked extends DomainEvent {
    private final UUID enrollmentId;
    private final UUID serviceEpisodeId;
    private final String serviceType;
    private final LocalDate serviceDate;
    private final String providedBy;

    public ServiceEpisodeLinked(
        UUID enrollmentId,
        UUID serviceEpisodeId,
        String serviceType,
        LocalDate serviceDate,
        String providedBy,
        Instant occurredAt
    ) {
        super(enrollmentId, occurredAt);
        this.enrollmentId = enrollmentId;
        this.serviceEpisodeId = serviceEpisodeId;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.providedBy = providedBy;
    }

    @Override
    public String eventType() {
        return "ServiceEpisodeLinked";
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public UUID serviceEpisodeId() {
        return serviceEpisodeId;
    }

    public String serviceType() {
        return serviceType;
    }

    public LocalDate serviceDate() {
        return serviceDate;
    }

    public String providedBy() {
        return providedBy;
    }

    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getServiceEpisodeId() { return serviceEpisodeId; }
    public String getServiceType() { return serviceType; }
    public LocalDate getServiceDate() { return serviceDate; }
    public String getProvidedBy() { return providedBy; }
}