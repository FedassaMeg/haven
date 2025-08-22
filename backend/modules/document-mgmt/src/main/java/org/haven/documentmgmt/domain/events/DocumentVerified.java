package org.haven.documentmgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentVerified(
    UUID documentId,
    String verifiedBy,
    String notes,
    LocalDate verifiedDate,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return documentId;
    }
    
    @Override
    public String eventType() {
        return "DocumentVerified";
    }
}