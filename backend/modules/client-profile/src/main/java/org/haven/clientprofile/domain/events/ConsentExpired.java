package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ConsentExpired extends DomainEvent {
    private final UUID clientId;
    private final String consentType;
    private final LocalDate originalExpirationDate;
    private final String expiredConsentPurpose;
    private final boolean autoExpired;

    public ConsentExpired(UUID clientId, String consentType, LocalDate originalExpirationDate, String expiredConsentPurpose, boolean autoExpired, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.consentType = consentType;
        this.originalExpirationDate = originalExpirationDate;
        this.expiredConsentPurpose = expiredConsentPurpose;
        this.autoExpired = autoExpired;
    }

    public ConsentExpired(UUID clientId, String consentType, LocalDate originalExpirationDate, String expiredConsentPurpose, boolean autoExpired) {
        this(clientId, consentType, originalExpirationDate, expiredConsentPurpose, autoExpired, Instant.now());
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public String consentType() {
        return consentType;
    }

    public LocalDate originalExpirationDate() {
        return originalExpirationDate;
    }

    public String expiredConsentPurpose() {
        return expiredConsentPurpose;
    }

    public boolean autoExpired() {
        return autoExpired;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public String getConsentType() {
        return consentType;
    }

    public LocalDate getOriginalExpirationDate() {
        return originalExpirationDate;
    }

    public String getExpiredConsentPurpose() {
        return expiredConsentPurpose;
    }

    public boolean isAutoExpired() {
        return autoExpired;
    }
}