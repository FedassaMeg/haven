package org.haven.reporting.domain;

import java.util.UUID;

/**
 * Warning about consent or policy violations for VAWA-protected records
 */
public class ConsentWarning {
    private final UUID clientId;
    private final String warningMessage;
    private final String policyRule;
    private final String recommendedAction;

    public ConsentWarning(UUID clientId, String warningMessage, String policyRule, String recommendedAction) {
        this.clientId = clientId;
        this.warningMessage = warningMessage;
        this.policyRule = policyRule;
        this.recommendedAction = recommendedAction;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public String getPolicyRule() {
        return policyRule;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    @Override
    public String toString() {
        return String.format("ConsentWarning[client=%s, policy=%s, message=%s]",
                clientId, policyRule, warningMessage);
    }
}
