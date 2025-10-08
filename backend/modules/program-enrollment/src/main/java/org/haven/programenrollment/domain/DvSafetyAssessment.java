package org.haven.programenrollment.domain;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Value object representing a comprehensive domestic violence safety assessment
 * Contains safety planning information and risk indicators
 */
public record DvSafetyAssessment(
    UUID assessmentId,
    UUID enrollmentId,
    LocalDate assessmentDate,
    boolean currentlyFleeing,
    boolean imminentDanger,
    SafetyPlanStatus safetyPlanStatus,
    Set<RiskIndicator> riskIndicators,
    String safetyPlanDetails,
    LocalDate nextReviewDate,
    String assessedBy
) {
    
    public enum SafetyPlanStatus {
        NOT_NEEDED("Safety plan not needed"),
        IN_DEVELOPMENT("Safety plan in development"),
        ACTIVE("Active safety plan in place"),
        REVIEWED("Safety plan reviewed and updated"),
        DECLINED("Client declined safety planning");
        
        private final String description;
        
        SafetyPlanStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum RiskIndicator {
        PHYSICAL_VIOLENCE,
        THREATS_OF_HARM,
        STALKING,
        ECONOMIC_CONTROL,
        ISOLATION,
        PSYCHOLOGICAL_ABUSE,
        TECHNOLOGY_FACILITATED_ABUSE,
        CHILDREN_AT_RISK,
        WEAPONS_INVOLVED,
        ESCALATING_BEHAVIOR
    }
    
    public boolean isHighRisk() {
        return imminentDanger || 
               riskIndicators.contains(RiskIndicator.WEAPONS_INVOLVED) ||
               riskIndicators.contains(RiskIndicator.ESCALATING_BEHAVIOR);
    }
    
    public boolean requiresImmediateAction() {
        return currentlyFleeing || imminentDanger;
    }
}