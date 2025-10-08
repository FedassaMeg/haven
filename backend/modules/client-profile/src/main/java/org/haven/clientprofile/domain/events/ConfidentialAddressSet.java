package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.AddressConfidentiality;
import java.time.Instant;
import java.util.UUID;

public class ConfidentialAddressSet extends DomainEvent {
    private final UUID clientId;
    private final AddressConfidentiality addressConfidentiality;

    public ConfidentialAddressSet(UUID clientId, AddressConfidentiality addressConfidentiality, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.addressConfidentiality = addressConfidentiality;
    }

    public ConfidentialAddressSet(UUID clientId, AddressConfidentiality addressConfidentiality) {
        this(clientId, addressConfidentiality, Instant.now());
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public AddressConfidentiality addressConfidentiality() {
        return addressConfidentiality;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public AddressConfidentiality getAddressConfidentiality() {
        return addressConfidentiality;
    }
}