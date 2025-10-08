package org.haven.reporting.domain.events;

import org.haven.reporting.domain.ExportJobState;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event: Export job state transitioned
 */
public class ExportJobStateChanged extends DomainEvent {
    private final ExportJobState previousState;
    private final ExportJobState newState;
    private final String reason;
    private final Long recordsProcessed;

    public ExportJobStateChanged(
            UUID exportJobId,
            ExportJobState previousState,
            ExportJobState newState,
            String reason,
            Long recordsProcessed,
            Instant occurredOn) {
        super(exportJobId, occurredOn);
        this.previousState = previousState;
        this.newState = newState;
        this.reason = reason;
        this.recordsProcessed = recordsProcessed;
    }

    public ExportJobState getPreviousState() {
        return previousState;
    }

    public ExportJobState getNewState() {
        return newState;
    }

    public String getReason() {
        return reason;
    }

    public Long getRecordsProcessed() {
        return recordsProcessed;
    }
}
