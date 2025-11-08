package org.haven.intake.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class PreIntakeContactExpired extends DomainEvent {
    private final UUID tempClientId;

    public PreIntakeContactExpired(
            UUID tempClientId,
            Instant occurredAt) {
        super(tempClientId, occurredAt);
        this.tempClientId = tempClientId;
    }

    public UUID tempClientId() { return tempClientId; }
}
