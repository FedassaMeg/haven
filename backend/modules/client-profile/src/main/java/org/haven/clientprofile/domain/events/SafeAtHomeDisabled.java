package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SafeAtHomeDisabled(
    UUID clientId,
    Instant occurredAt
) implements DomainEvent {
    
    public SafeAtHomeDisabled(UUID clientId) {
        this(clientId, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "SafeAtHomeDisabled";
    }
}