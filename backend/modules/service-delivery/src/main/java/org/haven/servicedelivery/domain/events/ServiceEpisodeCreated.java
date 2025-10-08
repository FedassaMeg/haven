package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.services.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ServiceEpisodeCreated extends DomainEvent {
    private final UUID clientId;
    private final String enrollmentId;
    private final String programId;
    private final String programName;
    private final ServiceType serviceType;
    private final ServiceDeliveryMode deliveryMode;
    private final LocalDate serviceDate;
    private final Integer plannedDurationMinutes;
    private final String primaryProviderId;
    private final String primaryProviderName;
    private final FundingSource fundingSource;
    private final String serviceDescription;
    private final boolean isConfidential;
    private final String createdBy;

    public ServiceEpisodeCreated(UUID episodeId, UUID clientId, String enrollmentId, String programId, String programName, ServiceType serviceType, ServiceDeliveryMode deliveryMode, LocalDate serviceDate, Integer plannedDurationMinutes, String primaryProviderId, String primaryProviderName, FundingSource fundingSource, String serviceDescription, boolean isConfidential, String createdBy, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.clientId = clientId;
        this.enrollmentId = enrollmentId;
        this.programId = programId;
        this.programName = programName;
        this.serviceType = serviceType;
        this.deliveryMode = deliveryMode;
        this.serviceDate = serviceDate;
        this.plannedDurationMinutes = plannedDurationMinutes;
        this.primaryProviderId = primaryProviderId;
        this.primaryProviderName = primaryProviderName;
        this.fundingSource = fundingSource;
        this.serviceDescription = serviceDescription;
        this.isConfidential = isConfidential;
        this.createdBy = createdBy;
    }

    public UUID episodeId() {
        return getAggregateId();
    }

    public UUID getEpisodeId() {
        return getAggregateId();
    }

    public UUID clientId() {
        return clientId;
    }

    public String enrollmentId() {
        return enrollmentId;
    }

    public String programId() {
        return programId;
    }

    public String programName() {
        return programName;
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public ServiceDeliveryMode deliveryMode() {
        return deliveryMode;
    }

    public LocalDate serviceDate() {
        return serviceDate;
    }

    public Integer plannedDurationMinutes() {
        return plannedDurationMinutes;
    }

    public String primaryProviderId() {
        return primaryProviderId;
    }

    public String primaryProviderName() {
        return primaryProviderName;
    }

    public FundingSource fundingSource() {
        return fundingSource;
    }

    public String serviceDescription() {
        return serviceDescription;
    }

    public boolean isConfidential() {
        return isConfidential;
    }

    public String createdBy() {
        return createdBy;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public String getEnrollmentId() { return enrollmentId; }
    public String getProgramId() { return programId; }
    public String getProgramName() { return programName; }
    public ServiceType getServiceType() { return serviceType; }
    public ServiceDeliveryMode getDeliveryMode() { return deliveryMode; }
    public LocalDate getServiceDate() { return serviceDate; }
    public Integer getPlannedDurationMinutes() { return plannedDurationMinutes; }
    public String getPrimaryProviderId() { return primaryProviderId; }
    public String getPrimaryProviderName() { return primaryProviderName; }
    public FundingSource getFundingSource() { return fundingSource; }
    public String getServiceDescription() { return serviceDescription; }
    public boolean IsConfidential() { return isConfidential; }
    public String getCreatedBy() { return createdBy; }
}