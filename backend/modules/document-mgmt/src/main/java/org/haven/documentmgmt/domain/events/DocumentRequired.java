package org.haven.documentmgmt.domain.events;

import org.haven.documentmgmt.domain.DocumentLifecycle.DocumentType;
import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentRequired(
    UUID documentId,
    UUID clientId,
    UUID caseId,
    String documentName,
    DocumentType documentType,
    LocalDate requiredDate,
    LocalDate expirationDate,
    String requiredBy,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return documentId;
    }
    
    @Override
    public String eventType() {
        return "DocumentRequired";
    }
}