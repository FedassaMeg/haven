package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when consent limitations or recipient information is updated
 */
public class ConsentUpdated extends DomainEvent {
    private final UUID consentId;
    private final UUID clientId;
    private final String newLimitations;
    private final String newRecipientContact;
    private final UUID updatedByUserId;

    public ConsentUpdated(
        UUID consentId,
        UUID clientId,
        String newLimitations,
        String newRecipientContact,
        UUID updatedByUserId,
        Instant updatedAt
    ) {
        super(consentId, updatedAt);
        this.consentId = consentId;
        this.clientId = clientId;
        this.newLimitations = newLimitations;
        this.newRecipientContact = newRecipientContact;
        this.updatedByUserId = updatedByUserId;
    }

    public UUID consentId() {
        return consentId;
    }

    public UUID clientId() {
        return clientId;
    }

    public String newLimitations() {
        return newLimitations;
    }

    public String newRecipientContact() {
        return newRecipientContact;
    }

    public UUID updatedByUserId() {
        return updatedByUserId;
    }

    // JavaBean-style getters
    public UUID getConsentId() { return consentId; }
    public UUID getClientId() { return clientId; }
    public String getNewLimitations() { return newLimitations; }
    public String getNewRecipientContact() { return newRecipientContact; }
    public UUID getUpdatedByUserId() { return updatedByUserId; }
}