package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.Address;
import java.time.Instant;
import java.util.UUID;

public record ClientAddressAdded(
    UUID clientId,
    Address address,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ClientAddressAdded";
    }
}