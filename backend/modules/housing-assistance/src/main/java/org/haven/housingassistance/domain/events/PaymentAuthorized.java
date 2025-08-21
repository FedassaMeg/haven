package org.haven.housingassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentAuthorized(
    UUID housingAssistanceId,
    UUID paymentId,
    BigDecimal amount,
    LocalDate paymentDate,
    String paymentType, // RENT, DEPOSIT, UTILITIES, etc.
    String payeeId,
    String payeeName,
    String fundingSourceCode,
    String authorizedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public PaymentAuthorized {
        if (housingAssistanceId == null) throw new IllegalArgumentException("Housing assistance ID cannot be null");
        if (paymentId == null) throw new IllegalArgumentException("Payment ID cannot be null");
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        if (paymentDate == null) throw new IllegalArgumentException("Payment date cannot be null");
        if (paymentType == null) throw new IllegalArgumentException("Payment type cannot be null");
        if (payeeId == null) throw new IllegalArgumentException("Payee ID cannot be null");
        if (authorizedBy == null) throw new IllegalArgumentException("Authorized by cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return housingAssistanceId;
    }
    
    @Override
    public String eventType() {
        return "PaymentAuthorized";
    }
}