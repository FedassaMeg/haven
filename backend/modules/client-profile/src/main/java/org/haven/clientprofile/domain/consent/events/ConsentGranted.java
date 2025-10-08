package org.haven.clientprofile.domain.consent.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when client grants consent for a specific purpose
 */
public class ConsentGranted extends DomainEvent {
    private final UUID consentId;
    private final UUID clientId;
    private final ConsentType consentType;
    private final String purpose;
    private final String recipientOrganization;
    private final String recipientContact;
    private final UUID grantedByUserId;
    private final Instant expiresAt;
    private final boolean isVAWAProtected;
    private final String limitations;

    public ConsentGranted(
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
    ) {
        super(consentId, grantedAt);
        this.consentId = consentId;
        this.clientId = clientId;
        this.consentType = consentType;
        this.purpose = purpose;
        this.recipientOrganization = recipientOrganization;
        this.recipientContact = recipientContact;
        this.grantedByUserId = grantedByUserId;
        this.expiresAt = expiresAt;
        this.isVAWAProtected = isVAWAProtected;
        this.limitations = limitations;
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

    public String purpose() {
        return purpose;
    }

    public String recipientOrganization() {
        return recipientOrganization;
    }

    public String recipientContact() {
        return recipientContact;
    }

    public UUID grantedByUserId() {
        return grantedByUserId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isVAWAProtected() {
        return isVAWAProtected;
    }

    public String limitations() {
        return limitations;
    }

    public Instant grantedAt() {
        return getOccurredOn();
    }

    // JavaBean-style getters
    public UUID getConsentId() { return consentId; }
    public UUID getClientId() { return clientId; }
    public ConsentType getConsentType() { return consentType; }
    public String getPurpose() { return purpose; }
    public String getRecipientOrganization() { return recipientOrganization; }
    public String getRecipientContact() { return recipientContact; }
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getLimitations() { return limitations; }
    public Instant getGrantedAt() { return getOccurredOn(); }
}