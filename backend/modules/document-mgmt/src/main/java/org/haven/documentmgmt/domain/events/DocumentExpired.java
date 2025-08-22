package org.haven.documentmgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentExpired(
    UUID documentId,
    String reason,
    LocalDate expiredDate,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return documentId;
    }
    
    @Override
    public String eventType() {
        return "DocumentExpired";
    }
}