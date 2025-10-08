package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when consent expiration date is extended
 */
public class ConsentExtended extends DomainEvent {
    private final UUID consentId;
    private final UUID clientId;
    private final Instant previousExpirationDate;
    private final Instant newExpirationDate;
    private final UUID extendedByUserId;

    public ConsentExtended(
        UUID consentId,
        UUID clientId,
        Instant previousExpirationDate,
        Instant newExpirationDate,
        UUID extendedByUserId,
        Instant extendedAt
    ) {
        super(consentId, extendedAt);
        this.consentId = consentId;
        this.clientId = clientId;
        this.previousExpirationDate = previousExpirationDate;
        this.newExpirationDate = newExpirationDate;
        this.extendedByUserId = extendedByUserId;
    }

    public UUID consentId() {
        return consentId;
    }

    public UUID clientId() {
        return clientId;
    }

    public Instant previousExpirationDate() {
        return previousExpirationDate;
    }

    public Instant newExpirationDate() {
        return newExpirationDate;
    }

    public UUID extendedByUserId() {
        return extendedByUserId;
    }

    // JavaBean-style getters
    public UUID getConsentId() { return consentId; }
    public UUID getClientId() { return clientId; }
    public Instant getPreviousExpirationDate() { return previousExpirationDate; }
    public Instant getNewExpirationDate() { return newExpirationDate; }
    public UUID getExtendedByUserId() { return extendedByUserId; }
}