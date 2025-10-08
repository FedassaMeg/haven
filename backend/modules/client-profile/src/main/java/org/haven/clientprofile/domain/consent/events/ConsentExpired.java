package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when consent expires due to time limits
 */
public class ConsentExpired extends DomainEvent {
    private final UUID consentId;
    private final UUID clientId;
    private final ConsentType consentType;
    private final Instant expirationDate;

    public ConsentExpired(
        UUID consentId,
        UUID clientId,
        ConsentType consentType,
        Instant expirationDate,
        Instant expiredAt
    ) {
        super(consentId, expiredAt);
        this.consentId = consentId;
        this.clientId = clientId;
        this.consentType = consentType;
        this.expirationDate = expirationDate;
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

    public Instant expirationDate() {
        return expirationDate;
    }

    // JavaBean-style getters
    public UUID getConsentId() { return consentId; }
    public UUID getClientId() { return clientId; }
    public ConsentType getConsentType() { return consentType; }
    public Instant getExpirationDate() { return expirationDate; }
}