package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.Client.ClientStatus;
import java.time.Instant;
import java.util.UUID;

public record ClientStatusChanged(
    UUID clientId,
    ClientStatus oldStatus,
    ClientStatus newStatus,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ClientStatusChanged";
    }
}