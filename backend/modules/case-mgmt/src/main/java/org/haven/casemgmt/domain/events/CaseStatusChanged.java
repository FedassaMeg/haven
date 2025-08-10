package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.CaseRecord.CaseStatus;
import java.time.Instant;
import java.util.UUID;

public record CaseStatusChanged(
    UUID caseId,
    CaseStatus oldStatus,
    CaseStatus newStatus,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "CaseStatusChanged";
    }
}