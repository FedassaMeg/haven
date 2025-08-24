package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ServiceMarkedCourtOrdered(
    UUID episodeId,
    String courtOrderNumber,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }

    @Override
    public String eventType() {
        return "ServiceMarkedCourtOrdered";
    }
}