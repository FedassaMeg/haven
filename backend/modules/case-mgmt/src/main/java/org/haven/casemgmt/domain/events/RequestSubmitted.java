package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequestSubmitted(
    UUID requestId,
    UUID clientId,
    UUID caseId,
    UUID enrollmentId,
    CodeableConcept assistanceType,
    BigDecimal requestedAmount,
    String purpose,
    String justification,
    String requestedBy,
    boolean isEmergency,
    Instant occurredAt
) implements DomainEvent {
    
    public RequestSubmitted {
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (purpose == null || purpose.trim().isEmpty()) throw new IllegalArgumentException("Purpose cannot be null or empty");
        if (requestedBy == null || requestedBy.trim().isEmpty()) throw new IllegalArgumentException("Requested by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return requestId;
    }
    
    @Override
    public String eventType() {
        return "RequestSubmitted";
    }
}