package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when client revokes previously granted consent
 */
public record ConsentRevoked(
    UUID consentId,
    UUID clientId,
    ConsentType consentType,
    UUID revokedByUserId,
    String reason,
    Instant revokedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return revokedAt;
    }
    
    @Override
    public String eventType() {
        return "ConsentRevoked";
    }
    
    @Override
    public UUID aggregateId() {
        return consentId;
    }
}