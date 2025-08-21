package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.AddressConfidentiality;
import java.time.Instant;
import java.util.UUID;

public record AddressConfidentialityUpdated(
    UUID clientId,
    AddressConfidentiality previousAddressConfidentiality,
    AddressConfidentiality newAddressConfidentiality,
    String updatedBy,
    UUID updatedByUserId,
    String updateReason,
    Instant occurredAt
) implements DomainEvent {
    
    public AddressConfidentialityUpdated(
        UUID clientId, 
        AddressConfidentiality previousAddressConfidentiality, 
        AddressConfidentiality newAddressConfidentiality, 
        String updatedBy, 
        UUID updatedByUserId, 
        String updateReason
    ) {
        this(clientId, previousAddressConfidentiality, newAddressConfidentiality, updatedBy, updatedByUserId, updateReason, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "AddressConfidentialityUpdated";
    }
}