package org.haven.eventstore;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID aggregateId();
    String eventType();
    Instant occurredAt();
}
