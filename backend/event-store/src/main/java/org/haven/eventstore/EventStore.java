package org.haven.eventstore;

import org.haven.shared.events.DomainEvent;

import java.util.List;
import java.util.UUID;

public interface EventStore {
    <EV extends DomainEvent> void append(UUID aggregateId, long expectedVersion, List<EV> events);
    List<EventEnvelope<? extends DomainEvent>> load(UUID aggregateId);
}
