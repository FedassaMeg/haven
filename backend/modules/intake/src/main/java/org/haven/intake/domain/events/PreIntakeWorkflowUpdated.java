package org.haven.intake.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class PreIntakeWorkflowUpdated extends DomainEvent {
    private final UUID tempClientId;
    private final int step;
    private final Map<String, Object> stepData;

    public PreIntakeWorkflowUpdated(
            UUID tempClientId,
            int step,
            Map<String, Object> stepData,
            Instant occurredAt) {
        super(tempClientId, occurredAt);
        this.tempClientId = tempClientId;
        this.step = step;
        this.stepData = stepData;
    }

    public UUID tempClientId() { return tempClientId; }
    public int step() { return step; }
    public Map<String, Object> stepData() { return stepData; }
}
