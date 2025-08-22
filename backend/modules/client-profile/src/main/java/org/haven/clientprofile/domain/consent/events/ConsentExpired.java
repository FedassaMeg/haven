package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when consent expires due to time limits
 */
public record ConsentExpired(
    UUID consentId,
    UUID clientId,
    ConsentType consentType,
    Instant expirationDate,
    Instant expiredAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return expiredAt;
    }
    
    @Override
    public String eventType() {
        return "ConsentExpired";
    }
    
    @Override
    public UUID aggregateId() {
        return consentId;
    }
}