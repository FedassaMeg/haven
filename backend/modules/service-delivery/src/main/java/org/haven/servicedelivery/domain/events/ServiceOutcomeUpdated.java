package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceOutcomeUpdated(
    UUID episodeId,
    String outcome,
    String followUpRequired,
    LocalDate followUpDate,
    String updatedBy,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }

    @Override
    public String eventType() {
        return "ServiceOutcomeUpdated";
    }
}