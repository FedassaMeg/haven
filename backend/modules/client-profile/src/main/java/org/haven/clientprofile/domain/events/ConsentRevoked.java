package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ConsentRevoked extends DomainEvent {
    private final UUID clientId;
    private final String consentType;
    private final String revocationReason;
    private final String revokedBy;

    public ConsentRevoked(UUID clientId, String consentType, String revocationReason, String revokedBy, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.consentType = consentType;
        this.revocationReason = revocationReason;
        this.revokedBy = revokedBy;
    }

    public ConsentRevoked(UUID clientId, String consentType, String revocationReason, String revokedBy) {
        this(clientId, consentType, revocationReason, revokedBy, Instant.now());
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public String consentType() {
        return consentType;
    }

    public String revocationReason() {
        return revocationReason;
    }

    public String revokedBy() {
        return revokedBy;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public String getConsentType() {
        return consentType;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public String getRevokedBy() {
        return revokedBy;
    }
}