package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ClientDeceasedMarked(
    UUID clientId,
    Instant deceasedDate,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ClientDeceasedMarked";
    }
}