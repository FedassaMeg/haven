package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.ContactPoint;
import java.time.Instant;
import java.util.UUID;

public record ClientTelecomAdded(
    UUID clientId,
    ContactPoint telecom,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ClientTelecomAdded";
    }
}