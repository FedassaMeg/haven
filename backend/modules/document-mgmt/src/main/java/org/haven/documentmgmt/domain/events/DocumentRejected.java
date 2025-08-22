package org.haven.documentmgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DocumentRejected(
    UUID documentId,
    String reason,
    String rejectedBy,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return documentId;
    }
    
    @Override
    public String eventType() {
        return "DocumentRejected";
    }
}