package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when client grants consent for a specific purpose
 */
public record ConsentGranted(
    UUID consentId,
    UUID clientId,
    ConsentType consentType,
    String purpose,
    String recipientOrganization,
    String recipientContact,
    UUID grantedByUserId,
    Instant grantedAt,
    Instant expiresAt,
    boolean isVAWAProtected,
    String limitations
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return grantedAt;
    }
    
    @Override
    public String eventType() {
        return "ConsentGranted";
    }
    
    @Override
    public UUID aggregateId() {
        return consentId;
    }
}