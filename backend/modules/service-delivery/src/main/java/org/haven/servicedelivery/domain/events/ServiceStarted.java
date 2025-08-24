package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceStarted(
    UUID episodeId,
    LocalDateTime startTime,
    String location,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }
    
    @Override
    public String eventType() {
        return "ServiceStarted";
    }
}