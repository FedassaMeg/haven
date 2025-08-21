package org.haven.financialassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.FinancialAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialAssistanceRequested(
    UUID financialAssistanceId,
    UUID clientId,
    UUID enrollmentId,
    FinancialAssistanceType assistanceType,
    BigDecimal requestedAmount,
    String purpose,
    String justification,
    String requestedBy,
    Boolean isEmergency,
    Instant occurredAt
) implements DomainEvent {
    
    public FinancialAssistanceRequested {
        if (financialAssistanceId == null) throw new IllegalArgumentException("Financial assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (purpose == null) throw new IllegalArgumentException("Purpose cannot be null");
        if (requestedBy == null) throw new IllegalArgumentException("Requested by cannot be null");
        if (isEmergency == null) isEmergency = false;
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return financialAssistanceId;
    }
    
    @Override
    public String eventType() {
        return "FinancialAssistanceRequested";
    }
}