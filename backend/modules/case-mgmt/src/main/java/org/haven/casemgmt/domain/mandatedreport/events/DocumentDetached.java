package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a document is removed from a mandated report
 */
public record DocumentDetached(
    UUID reportId,
    UUID documentId,
    String reason,
    UUID removedByUserId,
    Instant removedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return removedAt;
    }
    
    @Override
    public String eventType() {
        return "DocumentDetached";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}