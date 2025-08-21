package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.AddressConfidentiality;
import java.time.Instant;
import java.util.UUID;

public record ConfidentialAddressSet(
    UUID clientId,
    AddressConfidentiality addressConfidentiality,
    Instant occurredAt
) implements DomainEvent {
    
    public ConfidentialAddressSet(UUID clientId, AddressConfidentiality addressConfidentiality) {
        this(clientId, addressConfidentiality, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ConfidentialAddressSet";
    }
}