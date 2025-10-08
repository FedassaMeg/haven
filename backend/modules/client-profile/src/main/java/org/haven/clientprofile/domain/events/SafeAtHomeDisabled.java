package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class SafeAtHomeDisabled extends DomainEvent {
    private final UUID clientId;

    public SafeAtHomeDisabled(UUID clientId, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
    }

    public SafeAtHomeDisabled(UUID clientId) {
        this(clientId, Instant.now());
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }
}