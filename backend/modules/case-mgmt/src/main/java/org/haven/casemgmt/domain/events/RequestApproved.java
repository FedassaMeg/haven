package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequestApproved(
    UUID requestId,
    UUID clientId,
    BigDecimal requestedAmount,
    BigDecimal approvedAmount,
    String approvedBy,
    String approvalNotes,
    Instant occurredAt
) implements DomainEvent {
    
    public RequestApproved {
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (approvedBy == null || approvedBy.trim().isEmpty()) throw new IllegalArgumentException("Approved by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return requestId;
    }
    
    @Override
    public String eventType() {
        return "RequestApproved";
    }
}