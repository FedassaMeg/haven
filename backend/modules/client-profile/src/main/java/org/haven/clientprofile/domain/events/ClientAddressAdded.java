package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.Address;
import java.time.Instant;
import java.util.UUID;

public class ClientAddressAdded extends DomainEvent {
    private final UUID clientId;
    private final Address address;

    public ClientAddressAdded(UUID clientId, Address address, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.address = address;
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public Address address() {
        return address;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public Address getAddress() {
        return address;
    }
}