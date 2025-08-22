package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CaseAssignmentEnded(
    UUID caseId,
    UUID assignmentId,
    String assigneeId,
    String endReason,
    String endedBy,
    Instant endedAt,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "CaseAssignmentEnded";
    }
}