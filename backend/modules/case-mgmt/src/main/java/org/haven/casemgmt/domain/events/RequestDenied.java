package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RequestDenied extends DomainEvent {
    private final UUID clientId;
    private final BigDecimal requestedAmount;
    private final String denialReason;
    private final String deniedBy;

    public RequestDenied(UUID requestId, UUID clientId, BigDecimal requestedAmount, String denialReason, String deniedBy, Instant occurredAt) {
        super(requestId, occurredAt != null ? occurredAt : Instant.now());
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (denialReason == null || denialReason.trim().isEmpty()) throw new IllegalArgumentException("Denial reason cannot be null or empty");
        if (deniedBy == null || deniedBy.trim().isEmpty()) throw new IllegalArgumentException("Denied by cannot be null or empty");

        this.clientId = clientId;
        this.requestedAmount = requestedAmount;
        this.denialReason = denialReason;
        this.deniedBy = deniedBy;
    }

    public UUID clientId() {
        return clientId;
    }

    public BigDecimal requestedAmount() {
        return requestedAmount;
    }

    public String denialReason() {
        return denialReason;
    }

    public String deniedBy() {
        return deniedBy;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public String getDenialReason() { return denialReason; }
    public String getDeniedBy() { return deniedBy; }
}