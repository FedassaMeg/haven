package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when consent expiration date is extended
 */
public record ConsentExtended(
    UUID consentId,
    UUID clientId,
    Instant previousExpirationDate,
    Instant newExpirationDate,
    UUID extendedByUserId,
    Instant extendedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return extendedAt;
    }
    
    @Override
    public String eventType() {
        return "ConsentExtended";
    }
    
    @Override
    public UUID aggregateId() {
        return consentId;
    }
}