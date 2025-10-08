package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.ContactPoint;
import java.time.Instant;
import java.util.UUID;

public class ClientTelecomAdded extends DomainEvent {
    private final UUID clientId;
    private final ContactPoint telecom;

    public ClientTelecomAdded(UUID clientId, ContactPoint telecom, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.telecom = telecom;
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public ContactPoint telecom() {
        return telecom;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public ContactPoint getTelecom() {
        return telecom;
    }
}