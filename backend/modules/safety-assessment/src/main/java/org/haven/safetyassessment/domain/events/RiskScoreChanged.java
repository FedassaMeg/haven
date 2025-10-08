package org.haven.safetyassessment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.UUID;

public class RiskScoreChanged extends DomainEvent {
    private final UUID assessmentId;
    private final UUID clientId;
    private final int previousRiskScore;
    private final int newRiskScore;
    private final CodeableConcept riskLevel;
    private final String riskFactors;
    private final String assessedBy;
    private final UUID assessedByUserId;
    private final String changeReason;
    private final boolean triggersImmediateIntervention;

    public RiskScoreChanged(
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
    ) {
        super(assessmentId, occurredAt != null ? occurredAt : Instant.now());
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (newRiskScore < 0 || newRiskScore > 100) throw new IllegalArgumentException("Risk score must be between 0 and 100");
        if (riskLevel == null) throw new IllegalArgumentException("Risk level cannot be null");
        if (assessedBy == null || assessedBy.trim().isEmpty()) throw new IllegalArgumentException("Assessed by cannot be null or empty");

        this.assessmentId = assessmentId;
        this.clientId = clientId;
        this.previousRiskScore = previousRiskScore;
        this.newRiskScore = newRiskScore;
        this.riskLevel = riskLevel;
        this.riskFactors = riskFactors;
        this.assessedBy = assessedBy;
        this.assessedByUserId = assessedByUserId;
        this.changeReason = changeReason;
        this.triggersImmediateIntervention = triggersImmediateIntervention;
    }

    // Getter style methods
    public UUID getAssessmentId() { return assessmentId; }
    public UUID assessmentId() { return assessmentId; }

    public UUID getClientId() { return clientId; }
    public UUID clientId() { return clientId; }

    public int getPreviousRiskScore() { return previousRiskScore; }
    public int previousRiskScore() { return previousRiskScore; }

    public int getNewRiskScore() { return newRiskScore; }
    public int newRiskScore() { return newRiskScore; }

    public CodeableConcept getRiskLevel() { return riskLevel; }
    public CodeableConcept riskLevel() { return riskLevel; }

    public String getRiskFactors() { return riskFactors; }
    public String riskFactors() { return riskFactors; }

    public String getAssessedBy() { return assessedBy; }
    public String assessedBy() { return assessedBy; }

    public UUID getAssessedByUserId() { return assessedByUserId; }
    public UUID assessedByUserId() { return assessedByUserId; }

    public String getChangeReason() { return changeReason; }
    public String changeReason() { return changeReason; }

    public boolean isTriggersImmediateIntervention() { return triggersImmediateIntervention; }
    public boolean triggersImmediateIntervention() { return triggersImmediateIntervention; }
}