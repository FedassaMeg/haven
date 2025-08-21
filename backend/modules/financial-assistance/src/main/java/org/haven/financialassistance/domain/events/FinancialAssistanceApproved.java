package org.haven.financialassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.FinancialAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialAssistanceApproved(
    UUID financialAssistanceId,
    UUID clientId,
    UUID enrollmentId,
    FinancialAssistanceType assistanceType,
    BigDecimal requestedAmount,
    BigDecimal approvedAmount,
    String approvalReason,
    String approvedBy,
    UUID approvedByUserId,
    LocalDate approvalDate,
    LocalDate paymentDueDate,
    String paymentMethod,
    String conditions,
    Instant occurredAt
) implements DomainEvent {
    
    public FinancialAssistanceApproved {
        if (financialAssistanceId == null) throw new IllegalArgumentException("Financial assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (approvedBy == null || approvedBy.trim().isEmpty()) throw new IllegalArgumentException("Approved by cannot be null or empty");
        if (approvalDate == null) throw new IllegalArgumentException("Approval date cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return financialAssistanceId;
    }
    
    @Override
    public String eventType() {
        return "FinancialAssistanceApproved";
    }
}