package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.services.FundingSource;
import java.time.Instant;
import java.util.UUID;

public record FundingSourceAdded(
    UUID episodeId,
    FundingSource fundingSource,
    double allocationPercentage,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }

    @Override
    public String eventType() {
        return "FundingSourceAdded";
    }
}