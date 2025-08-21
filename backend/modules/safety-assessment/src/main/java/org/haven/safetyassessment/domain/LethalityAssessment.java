package org.haven.safetyassessment.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lethality Assessment based on evidence-based DV risk factors
 * Uses standardized assessment tools like ODARA, DA, or LAP
 */
public class LethalityAssessment {
    private UUID assessmentId;
    private LethalityAssessmentTool toolUsed;
    private Map<String, Object> responses = new HashMap<>();
    private RiskLevel calculatedRiskLevel;
    private Integer totalScore;
    private String assessorId;
    private String assessorRole;
    private Instant assessmentDate;
    private String immediateRiskFactors;
    private String recommendations;
    private boolean requiresImmediateIntervention;
    
    public LethalityAssessment(LethalityAssessmentTool toolUsed, String assessorId, String assessorRole) {
        this.assessmentId = UUID.randomUUID();
        this.toolUsed = toolUsed;
        this.assessorId = assessorId;
        this.assessorRole = assessorRole;
        this.assessmentDate = Instant.now();
    }
    
    public void recordResponse(String questionId, Object response) {
        this.responses.put(questionId, response);
        recalculateRisk();
    }
    
    public void recordResponses(Map<String, Object> allResponses) {
        this.responses.putAll(allResponses);
        recalculateRisk();
    }
    
    private void recalculateRisk() {
        switch (toolUsed) {
            case ODARA -> calculateODARAScore();
            case DANGER_ASSESSMENT -> calculateDAScore();
            case LAP -> calculateLAPScore();
            case CUSTOM_DV_SCREENING -> calculateCustomScore();
        }
        
        // Determine if immediate intervention is required
        this.requiresImmediateIntervention = 
            calculatedRiskLevel == RiskLevel.EXTREME || hasImmediateRiskFactors();
    }
    
    private void calculateODARAScore() {
        // Ontario Domestic Assault Risk Assessment implementation
        int score = 0;
        
        // Key ODARA factors with weights
        if (Boolean.TRUE.equals(responses.get("prior_domestic_incident"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("prior_non_domestic_incident"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("prior_failure_to_comply"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("threats_to_harm"))) score += 2;
        if (Boolean.TRUE.equals(responses.get("confinement_of_victim"))) score += 2;
        if (Boolean.TRUE.equals(responses.get("victim_concern_for_future_assault"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("assault_during_pregnancy"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("children_in_home"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("barriers_to_support"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("substance_abuse"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("weapons_threats"))) score += 2;
        if (Boolean.TRUE.equals(responses.get("strangulation"))) score += 3;
        
        this.totalScore = score;
        
        // ODARA risk categorization
        if (score >= 7) {
            this.calculatedRiskLevel = RiskLevel.EXTREME;
        } else if (score >= 5) {
            this.calculatedRiskLevel = RiskLevel.HIGH;
        } else if (score >= 3) {
            this.calculatedRiskLevel = RiskLevel.MODERATE;
        } else if (score >= 1) {
            this.calculatedRiskLevel = RiskLevel.LOW;
        } else {
            this.calculatedRiskLevel = RiskLevel.MINIMAL;
        }
    }
    
    private void calculateDAScore() {
        // Danger Assessment tool implementation
        int score = 0;
        
        // Critical DA factors
        if (Boolean.TRUE.equals(responses.get("increased_frequency_severity"))) score += 2;
        if (Boolean.TRUE.equals(responses.get("threats_with_weapon"))) score += 3;
        if (Boolean.TRUE.equals(responses.get("access_to_gun"))) score += 3;
        if (Boolean.TRUE.equals(responses.get("threats_to_kill"))) score += 3;
        if (Boolean.TRUE.equals(responses.get("forced_sex"))) score += 2;
        if (Boolean.TRUE.equals(responses.get("choking_strangulation"))) score += 3;
        if (Boolean.TRUE.equals(responses.get("jealous_controlling"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("separation_recent"))) score += 2;
        if (Boolean.TRUE.equals(responses.get("unemployed"))) score += 1;
        if (Boolean.TRUE.equals(responses.get("avoided_family_friends"))) score += 1;
        
        this.totalScore = score;
        
        // DA risk categorization  
        if (score >= 14) {
            this.calculatedRiskLevel = RiskLevel.EXTREME;
        } else if (score >= 9) {
            this.calculatedRiskLevel = RiskLevel.HIGH;
        } else if (score >= 5) {
            this.calculatedRiskLevel = RiskLevel.MODERATE;
        } else if (score >= 2) {
            this.calculatedRiskLevel = RiskLevel.LOW;
        } else {
            this.calculatedRiskLevel = RiskLevel.MINIMAL;
        }
    }
    
    private void calculateLAPScore() {
        // Lethality Assessment Program implementation
        // LAP is binary - either high risk or lower risk
        
        boolean hasHighRiskFactors = 
            Boolean.TRUE.equals(responses.get("gun_access")) ||
            Boolean.TRUE.equals(responses.get("threats_to_kill")) ||
            Boolean.TRUE.equals(responses.get("choking_attempt")) ||
            Boolean.TRUE.equals(responses.get("beaten_while_pregnant")) ||
            Boolean.TRUE.equals(responses.get("constant_jealousy")) ||
            Boolean.TRUE.equals(responses.get("controlling_activities")) ||
            Boolean.TRUE.equals(responses.get("drunk_high_violence")) ||
            Boolean.TRUE.equals(responses.get("forced_sex")) ||
            Boolean.TRUE.equals(responses.get("escalating_violence"));
            
        if (hasHighRiskFactors) {
            this.calculatedRiskLevel = RiskLevel.HIGH;
            this.totalScore = 1; // LAP is binary
        } else {
            this.calculatedRiskLevel = RiskLevel.MODERATE;
            this.totalScore = 0;
        }
    }
    
    private void calculateCustomScore() {
        // Custom DV screening implementation
        // Can be customized based on organizational needs
        int score = 0;
        
        for (Map.Entry<String, Object> entry : responses.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                score += 1; // Basic scoring - can be enhanced
            }
        }
        
        this.totalScore = score;
        
        // Basic risk categorization
        if (score >= 8) {
            this.calculatedRiskLevel = RiskLevel.EXTREME;
        } else if (score >= 6) {
            this.calculatedRiskLevel = RiskLevel.HIGH;
        } else if (score >= 4) {
            this.calculatedRiskLevel = RiskLevel.MODERATE;
        } else if (score >= 2) {
            this.calculatedRiskLevel = RiskLevel.LOW;
        } else {
            this.calculatedRiskLevel = RiskLevel.MINIMAL;
        }
    }
    
    private boolean hasImmediateRiskFactors() {
        // Check for factors that require immediate intervention regardless of score
        return Boolean.TRUE.equals(responses.get("threats_to_kill")) ||
               Boolean.TRUE.equals(responses.get("gun_access")) ||
               Boolean.TRUE.equals(responses.get("recent_separation")) ||
               Boolean.TRUE.equals(responses.get("choking_strangulation")) ||
               Boolean.TRUE.equals(responses.get("escalating_violence"));
    }
    
    public void addRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
    
    public void flagImmediateRiskFactors(String factors) {
        this.immediateRiskFactors = factors;
    }
    
    public enum LethalityAssessmentTool {
        ODARA("Ontario Domestic Assault Risk Assessment"),
        DANGER_ASSESSMENT("Danger Assessment"),
        LAP("Lethality Assessment Program"),
        CUSTOM_DV_SCREENING("Custom DV Screening Tool");
        
        private final String displayName;
        
        LethalityAssessmentTool(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Getters
    public UUID getAssessmentId() { return assessmentId; }
    public LethalityAssessmentTool getToolUsed() { return toolUsed; }
    public Map<String, Object> getResponses() { return new HashMap<>(responses); }
    public RiskLevel getCalculatedRiskLevel() { return calculatedRiskLevel; }
    public Integer getTotalScore() { return totalScore; }
    public String getAssessorId() { return assessorId; }
    public String getAssessorRole() { return assessorRole; }
    public Instant getAssessmentDate() { return assessmentDate; }
    public String getImmediateRiskFactors() { return immediateRiskFactors; }
    public String getRecommendations() { return recommendations; }
    public boolean requiresImmediateIntervention() { return requiresImmediateIntervention; }
}