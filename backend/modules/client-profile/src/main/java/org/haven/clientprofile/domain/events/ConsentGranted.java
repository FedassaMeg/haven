package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ConsentGranted extends DomainEvent {
    private final UUID clientId;
    private final String consentType;
    private final String purpose;
    private final Instant expirationDate;
    private final String grantedBy;

    public ConsentGranted(UUID clientId, String consentType, String purpose, Instant expirationDate, String grantedBy, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.consentType = consentType;
        this.purpose = purpose;
        this.expirationDate = expirationDate;
        this.grantedBy = grantedBy;
    }

    public ConsentGranted(UUID clientId, String consentType, String purpose, Instant expirationDate, String grantedBy) {
        this(clientId, consentType, purpose, expirationDate, grantedBy, Instant.now());
    }

    @Override
    public String eventType() {
        return "ConsentGranted";
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public String consentType() {
        return consentType;
    }

    public String purpose() {
        return purpose;
    }

    public Instant expirationDate() {
        return expirationDate;
    }

    public String grantedBy() {
        return grantedBy;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public String getConsentType() {
        return consentType;
    }

    public String getPurpose() {
        return purpose;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public String getGrantedBy() {
        return grantedBy;
    }
}