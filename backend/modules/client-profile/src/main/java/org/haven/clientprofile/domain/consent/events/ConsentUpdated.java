package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when consent limitations or recipient information is updated
 */
public record ConsentUpdated(
    UUID consentId,
    UUID clientId,
    String newLimitations,
    String newRecipientContact,
    UUID updatedByUserId,
    Instant updatedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return updatedAt;
    }
    
    @Override
    public String eventType() {
        return "ConsentUpdated";
    }
    
    @Override
    public UUID aggregateId() {
        return consentId;
    }
}