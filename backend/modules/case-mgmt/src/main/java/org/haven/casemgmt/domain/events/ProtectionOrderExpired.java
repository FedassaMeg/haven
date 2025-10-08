package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProtectionOrderExpired extends DomainEvent {
    private final UUID clientId;
    private final UUID protectionOrderId;
    private final LocalDate originalExpirationDate;
    private final LocalDate actualExpirationDate;
    private final boolean wasRenewed;
    private final UUID renewedOrderId;
    private final String expirationNotes;
    private final boolean autoExpired;

    public ProtectionOrderExpired(UUID caseId, UUID clientId, UUID protectionOrderId, LocalDate originalExpirationDate, LocalDate actualExpirationDate, boolean wasRenewed, UUID renewedOrderId, String expirationNotes, boolean autoExpired, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (protectionOrderId == null) throw new IllegalArgumentException("Protection order ID cannot be null");
        if (originalExpirationDate == null) throw new IllegalArgumentException("Original expiration date cannot be null");
        if (actualExpirationDate == null) throw new IllegalArgumentException("Actual expiration date cannot be null");

        this.clientId = clientId;
        this.protectionOrderId = protectionOrderId;
        this.originalExpirationDate = originalExpirationDate;
        this.actualExpirationDate = actualExpirationDate;
        this.wasRenewed = wasRenewed;
        this.renewedOrderId = renewedOrderId;
        this.expirationNotes = expirationNotes;
        this.autoExpired = autoExpired;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID protectionOrderId() {
        return protectionOrderId;
    }

    public LocalDate originalExpirationDate() {
        return originalExpirationDate;
    }

    public LocalDate actualExpirationDate() {
        return actualExpirationDate;
    }

    public boolean wasRenewed() {
        return wasRenewed;
    }

    public UUID renewedOrderId() {
        return renewedOrderId;
    }

    public String expirationNotes() {
        return expirationNotes;
    }

    public boolean autoExpired() {
        return autoExpired;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getProtectionOrderId() { return protectionOrderId; }
    public LocalDate getOriginalExpirationDate() { return originalExpirationDate; }
    public LocalDate getActualExpirationDate() { return actualExpirationDate; }
    public boolean getWasRenewed() { return wasRenewed; }
    public UUID getRenewedOrderId() { return renewedOrderId; }
    public String getExpirationNotes() { return expirationNotes; }
    public boolean getAutoExpired() { return autoExpired; }
}