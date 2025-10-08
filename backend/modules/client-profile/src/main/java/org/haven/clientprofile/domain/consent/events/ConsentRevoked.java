package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when client revokes previously granted consent
 */
public class ConsentRevoked extends DomainEvent {
    private final UUID consentId;
    private final UUID clientId;
    private final ConsentType consentType;
    private final UUID revokedByUserId;
    private final String reason;

    public ConsentRevoked(
        UUID consentId,
        UUID clientId,
        ConsentType consentType,
        UUID revokedByUserId,
        String reason,
        Instant revokedAt
    ) {
        super(consentId, revokedAt);
        this.consentId = consentId;
        this.clientId = clientId;
        this.consentType = consentType;
        this.revokedByUserId = revokedByUserId;
        this.reason = reason;
    }

    public UUID consentId() {
        return consentId;
    }

    public UUID clientId() {
        return clientId;
    }

    public ConsentType consentType() {
        return consentType;
    }

    public UUID revokedByUserId() {
        return revokedByUserId;
    }

    public String reason() {
        return reason;
    }

    public Instant revokedAt() {
        return getOccurredOn();
    }

    // JavaBean-style getters
    public UUID getConsentId() { return consentId; }
    public UUID getClientId() { return clientId; }
    public ConsentType getConsentType() { return consentType; }
    public UUID getRevokedByUserId() { return revokedByUserId; }
    public String getReason() { return reason; }
    public Instant getRevokedAt() { return getOccurredOn(); }
}