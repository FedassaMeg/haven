package org.haven.safetyassessment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.UUID;

public record RiskScoreChanged(
    UUID assessmentId,
    UUID clientId,
    int previousRiskScore,
    int newRiskScore,
    CodeableConcept riskLevel,
    String riskFactors,
    String assessedBy,
    UUID assessedByUserId,
    String changeReason,
    boolean triggersImmediateIntervention,
    Instant occurredAt
) implements DomainEvent {
    
    public RiskScoreChanged {
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (newRiskScore < 0 || newRiskScore > 100) throw new IllegalArgumentException("Risk score must be between 0 and 100");
        if (riskLevel == null) throw new IllegalArgumentException("Risk level cannot be null");
        if (assessedBy == null || assessedBy.trim().isEmpty()) throw new IllegalArgumentException("Assessed by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return assessmentId;
    }
    
    @Override
    public String eventType() {
        return "RiskScoreChanged";
    }
}