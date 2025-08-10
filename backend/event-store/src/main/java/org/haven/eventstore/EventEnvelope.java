package org.haven.eventstore;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<EV extends DomainEvent>(
    UUID aggregateId,
    long sequence,
    Instant recordedAt,
    EV event
) {}
