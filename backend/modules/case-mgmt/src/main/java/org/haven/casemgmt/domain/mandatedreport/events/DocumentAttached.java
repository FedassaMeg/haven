package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a document is attached to a mandated report
 */
public record DocumentAttached(
    UUID reportId,
    UUID documentId,
    String fileName,
    String documentType,
    boolean isRequired,
    UUID attachedByUserId,
    Instant attachedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return attachedAt;
    }
    
    @Override
    public String eventType() {
        return "DocumentAttached";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}