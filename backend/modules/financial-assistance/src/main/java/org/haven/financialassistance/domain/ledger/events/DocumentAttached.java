package org.haven.financialassistance.domain.ledger.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DocumentAttached(
    UUID ledgerId,
    String documentId,
    String documentName,
    String documentType,
    byte[] documentContent,
    String uploadedBy,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return ledgerId;
    }

    @Override
    public String eventType() {
        return "DocumentAttached";
    }

    public static DocumentAttached create(UUID ledgerId, String documentId, String documentName,
                                        String documentType, byte[] documentContent, String uploadedBy) {
        return new DocumentAttached(
            ledgerId, documentId, documentName, documentType, documentContent, uploadedBy, Instant.now()
        );
    }
}