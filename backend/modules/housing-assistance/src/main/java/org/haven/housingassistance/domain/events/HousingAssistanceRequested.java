package org.haven.housingassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.housingassistance.domain.RentalAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HousingAssistanceRequested(
    UUID housingAssistanceId,
    UUID clientId,
    UUID enrollmentId,
    RentalAssistanceType assistanceType,
    BigDecimal requestedAmount,
    Integer requestedDurationMonths,
    String justification,
    String requestedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public HousingAssistanceRequested {
        if (housingAssistanceId == null) throw new IllegalArgumentException("Housing assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (requestedBy == null) throw new IllegalArgumentException("Requested by cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return housingAssistanceId;
    }
    
    @Override
    public String eventType() {
        return "HousingAssistanceRequested";
    }
}