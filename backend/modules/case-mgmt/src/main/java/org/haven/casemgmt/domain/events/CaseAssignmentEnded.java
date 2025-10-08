package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class CaseAssignmentEnded extends DomainEvent {
    private final UUID assignmentId;
    private final String assigneeId;
    private final String endReason;
    private final String endedBy;
    private final Instant endedAt;

    public CaseAssignmentEnded(UUID caseId, UUID assignmentId, String assigneeId, String endReason, String endedBy, Instant endedAt, Instant occurredAt) {
        super(caseId, occurredAt);
        this.assignmentId = assignmentId;
        this.assigneeId = assigneeId;
        this.endReason = endReason;
        this.endedBy = endedBy;
        this.endedAt = endedAt;
    }

    public UUID assignmentId() {
        return assignmentId;
    }

    public String assigneeId() {
        return assigneeId;
    }

    public String endReason() {
        return endReason;
    }

    public String endedBy() {
        return endedBy;
    }

    public Instant endedAt() {
        return endedAt;
    }


    // JavaBean-style getters
    public UUID getAssignmentId() { return assignmentId; }
    public String getAssigneeId() { return assigneeId; }
    public String getEndReason() { return endReason; }
    public String getEndedBy() { return endedBy; }
    public Instant getEndedAt() { return endedAt; }
}