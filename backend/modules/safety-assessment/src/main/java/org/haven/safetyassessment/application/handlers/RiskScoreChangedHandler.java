package org.haven.safetyassessment.application.handlers;

import org.haven.safetyassessment.domain.events.RiskScoreChanged;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles risk score changes for immediate intervention and safety protocols
 */
@Component
public class RiskScoreChangedHandler implements EventHandler<RiskScoreChanged> {

    @Override
    public void handle(RiskScoreChanged event) {
        System.out.println("Risk score changed for client: " + event.clientId() + 
                          " - Previous: " + event.previousRiskScore() + 
                          " - New: " + event.newRiskScore() +
                          " - Level: " + event.riskLevel().text());
        
        // Critical safety responses based on risk level:
        if (event.triggersImmediateIntervention()) {
            System.out.println("CRITICAL: Immediate intervention required for client " + event.clientId());
            // - Alert supervisors and on-call staff
            // - Activate emergency safety protocols
            // - Schedule immediate safety planning session
            // - Consider law enforcement notification
            // - Update protection order if needed
        }
        
        if (event.newRiskScore() >= 80) {
            // High risk protocols
            // - Daily check-ins required
            // - Enhanced safety planning
            // - Coordinate with legal advocacy
            // - Consider emergency shelter placement
        } else if (event.newRiskScore() >= 60) {
            // Medium-high risk protocols  
            // - Weekly safety check-ins
            // - Review safety plan effectiveness
            // - Assess support system strength
        }
        
        // Always log for lethality assessment documentation
        System.out.println("Risk factors identified: " + event.riskFactors());
        System.out.println("Assessed by: " + event.assessedBy());
    }

    @Override
    public Class<RiskScoreChanged> getEventType() {
        return RiskScoreChanged.class;
    }
}