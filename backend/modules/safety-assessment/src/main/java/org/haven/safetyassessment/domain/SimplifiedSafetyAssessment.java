package org.haven.safetyassessment.domain;

import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.safetyassessment.domain.events.SafetyPlanUpdated;
import org.haven.safetyassessment.domain.events.RiskScoreChanged;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Simplified Safety Assessment aggregate for demonstration
 */
public class SimplifiedSafetyAssessment extends AggregateRoot<SafetyAssessmentId> {
    private UUID clientId;
    private int currentRiskScore;
    private String riskLevel;
    private List<String> safetyStrategies;
    private Instant createdAt;
    private Instant lastModified;
    
    public static SimplifiedSafetyAssessment create(UUID clientId) {
        SafetyAssessmentId assessmentId = SafetyAssessmentId.generate();
        SimplifiedSafetyAssessment assessment = new SimplifiedSafetyAssessment();
        assessment.id = assessmentId;
        assessment.clientId = clientId;
        assessment.currentRiskScore = 0;
        assessment.riskLevel = "LOW";
        assessment.createdAt = Instant.now();
        assessment.lastModified = Instant.now();
        return assessment;
    }
    
    public void updateSafetyPlan(List<String> safetyStrategies, List<String> warningSignsIdentified,
                               List<String> resourcesIdentified, String emergencyContact,
                               String emergencyContactPhone, String safeLocation,
                               String updatedBy, UUID updatedByUserId, String updateReason) {
        apply(new SafetyPlanUpdated(
            id.value(),
            clientId,
            safetyStrategies,
            warningSignsIdentified,
            resourcesIdentified,
            emergencyContact,
            emergencyContactPhone,
            safeLocation,
            updatedBy,
            updatedByUserId,
            updateReason,
            Instant.now()
        ));
    }
    
    public void updateRiskScore(int newRiskScore, String newRiskLevel, String riskFactors,
                              String assessedBy, UUID assessedByUserId, String changeReason,
                              boolean triggersImmediateIntervention) {
        int previousScore = this.currentRiskScore;
        CodeableConcept.Coding riskCoding = new CodeableConcept.Coding("risk-level", null, newRiskLevel, newRiskLevel, null);
        CodeableConcept riskLevelConcept = new CodeableConcept(List.of(riskCoding), newRiskLevel);
        
        apply(new RiskScoreChanged(
            id.value(),
            clientId,
            previousScore,
            newRiskScore,
            riskLevelConcept,
            riskFactors,
            assessedBy,
            assessedByUserId,
            changeReason,
            triggersImmediateIntervention,
            Instant.now()
        ));
    }
    
    @Override
    protected void when(DomainEvent e) {
        if (e instanceof SafetyPlanUpdated ev) {
            this.safetyStrategies = ev.safetyStrategies();
            this.lastModified = ev.occurredAt();
        } else if (e instanceof RiskScoreChanged ev) {
            this.currentRiskScore = ev.newRiskScore();
            this.riskLevel = ev.riskLevel().text();
            this.lastModified = ev.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + e.getClass());
        }
    }
    
    // Getters
    public UUID getClientId() { return clientId; }
    public int getCurrentRiskScore() { return currentRiskScore; }
    public String getRiskLevel() { return riskLevel; }
    public List<String> getSafetyStrategies() { return safetyStrategies; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
}