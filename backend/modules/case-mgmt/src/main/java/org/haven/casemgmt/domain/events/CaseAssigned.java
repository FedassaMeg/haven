package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.UUID;

public record CaseAssigned(
    UUID caseId,
    String assigneeId,
    CodeableConcept role,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "CaseAssigned";
    }
}