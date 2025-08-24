package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record DocumentAttached(
    UUID episodeId,
    String documentId,
    String documentType,
    String description,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }

    @Override
    public String eventType() {
        return "DocumentAttached";
    }
}