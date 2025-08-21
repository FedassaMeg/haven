package org.haven.financialassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.FinancialAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialAssistancePaid(
    UUID financialAssistanceId,
    UUID clientId,
    UUID enrollmentId,
    FinancialAssistanceType assistanceType,
    BigDecimal approvedAmount,
    BigDecimal paidAmount,
    LocalDate paymentDate,
    String paymentMethod,
    String paymentReference,
    String checkNumber,
    String paidBy,
    UUID paidByUserId,
    String paymentNotes,
    boolean isPartialPayment,
    BigDecimal remainingBalance,
    Instant occurredAt
) implements DomainEvent {
    
    public FinancialAssistancePaid {
        if (financialAssistanceId == null) throw new IllegalArgumentException("Financial assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (paidAmount == null) throw new IllegalArgumentException("Paid amount cannot be null");
        if (paymentDate == null) throw new IllegalArgumentException("Payment date cannot be null");
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) throw new IllegalArgumentException("Payment method cannot be null or empty");
        if (paidBy == null || paidBy.trim().isEmpty()) throw new IllegalArgumentException("Paid by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return financialAssistanceId;
    }
    
    @Override
    public String eventType() {
        return "FinancialAssistancePaid";
    }
}