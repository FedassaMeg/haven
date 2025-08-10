package org.haven.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events
 */
public interface DomainEvent {
    UUID aggregateId();
    String eventType();
    Instant occurredAt();
}