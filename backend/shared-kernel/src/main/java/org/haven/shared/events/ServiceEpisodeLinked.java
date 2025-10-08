package org.haven.shared.events;

import org.haven.shared.vo.services.ServiceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain Event: Service Episode Linked to Enrollment
 * Published when a service episode is created and needs to be linked to enrollment
 * Enables decoupling between service-delivery and program-enrollment modules
 */
public class ServiceEpisodeLinked extends DomainEvent {

    private final UUID episodeId;
    private final UUID clientId;
    private final ServiceType serviceType;
    private final LocalDate serviceDate;
    private final String providerName;

    public ServiceEpisodeLinked(
            UUID episodeId,
            UUID enrollmentId,
            UUID clientId,
            ServiceType serviceType,
            LocalDate serviceDate,
            String providerName) {
        super(enrollmentId, Instant.now());
        this.episodeId = episodeId;
        this.clientId = clientId;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.providerName = providerName;
    }

    public UUID getEpisodeId() {
        return episodeId;
    }

    public UUID episodeId() {
        return episodeId;
    }

    public UUID getEnrollmentId() {
        return getAggregateId();
    }

    public UUID enrollmentId() {
        return getAggregateId();
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID clientId() {
        return clientId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public LocalDate serviceDate() {
        return serviceDate;
    }

    public String getProviderName() {
        return providerName;
    }

    public String providerName() {
        return providerName;
    }
}
