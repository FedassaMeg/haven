package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.services.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceEpisodeCreated(
    UUID episodeId,
    UUID clientId,
    String enrollmentId,
    String programId,
    String programName,
    ServiceType serviceType,
    ServiceDeliveryMode deliveryMode,
    LocalDate serviceDate,
    Integer plannedDurationMinutes,
    String primaryProviderId,
    String primaryProviderName,
    FundingSource fundingSource,
    String serviceDescription,
    boolean isConfidential,
    String createdBy,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }
    
    @Override
    public String eventType() {
        return "ServiceEpisodeCreated";
    }
}