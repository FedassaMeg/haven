package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.Client.ClientStatus;
import java.time.Instant;
import java.util.UUID;

public class ClientStatusChanged extends DomainEvent {
    private final UUID clientId;
    private final ClientStatus oldStatus;
    private final ClientStatus newStatus;

    public ClientStatusChanged(UUID clientId, ClientStatus oldStatus, ClientStatus newStatus, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public ClientStatus oldStatus() {
        return oldStatus;
    }

    public ClientStatus newStatus() {
        return newStatus;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public ClientStatus getOldStatus() {
        return oldStatus;
    }

    public ClientStatus getNewStatus() {
        return newStatus;
    }
}