package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequestDenied(
    UUID requestId,
    UUID clientId,
    BigDecimal requestedAmount,
    String denialReason,
    String deniedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public RequestDenied {
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (denialReason == null || denialReason.trim().isEmpty()) throw new IllegalArgumentException("Denial reason cannot be null or empty");
        if (deniedBy == null || deniedBy.trim().isEmpty()) throw new IllegalArgumentException("Denied by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return requestId;
    }
    
    @Override
    public String eventType() {
        return "RequestDenied";
    }
}