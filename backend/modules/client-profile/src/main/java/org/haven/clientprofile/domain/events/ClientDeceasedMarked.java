package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ClientDeceasedMarked extends DomainEvent {
    private final UUID clientId;
    private final Instant deceasedDate;

    public ClientDeceasedMarked(UUID clientId, Instant deceasedDate, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.deceasedDate = deceasedDate;
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public Instant deceasedDate() {
        return deceasedDate;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public Instant getDeceasedDate() {
        return deceasedDate;
    }
}