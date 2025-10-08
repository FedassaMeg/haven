package org.haven.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events
 */
public abstract class DomainEvent {
    private final UUID aggregateId;
    private final Instant occurredOn;

    protected DomainEvent(UUID aggregateId, Instant occurredOn) {
        this.aggregateId = aggregateId;
        this.occurredOn = occurredOn;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public UUID aggregateId() {
        return aggregateId;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public Instant occurredAt() {
        return occurredOn;
    }

    public String eventType() {
        return this.getClass().getSimpleName();
    }
}