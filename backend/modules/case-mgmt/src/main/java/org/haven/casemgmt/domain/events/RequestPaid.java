package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequestPaid(
    UUID requestId,
    UUID clientId,
    BigDecimal approvedAmount,
    BigDecimal paidAmount,
    String paymentMethod,
    String vendorName,
    String paymentReference,
    String paidBy,
    Instant occurredAt
) implements DomainEvent {
    
    public RequestPaid {
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (paidAmount == null) throw new IllegalArgumentException("Paid amount cannot be null");
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) throw new IllegalArgumentException("Payment method cannot be null or empty");
        if (paidBy == null || paidBy.trim().isEmpty()) throw new IllegalArgumentException("Paid by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return requestId;
    }
    
    @Override
    public String eventType() {
        return "RequestPaid";
    }
}