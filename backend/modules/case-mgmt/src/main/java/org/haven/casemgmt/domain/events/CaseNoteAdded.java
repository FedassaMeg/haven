package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CaseNoteAdded(
    UUID caseId,
    UUID noteId,
    String content,
    String authorId,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "CaseNoteAdded";
    }
}