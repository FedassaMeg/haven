package org.haven.intake.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class PreIntakeContactPromoted extends DomainEvent {
    private final UUID tempClientId;
    private final UUID clientId;

    public PreIntakeContactPromoted(
            UUID tempClientId,
            UUID clientId,
            Instant occurredAt) {
        super(tempClientId, occurredAt);
        this.tempClientId = tempClientId;
        this.clientId = clientId;
    }

    public UUID tempClientId() { return tempClientId; }
    public UUID clientId() { return clientId; }
}
