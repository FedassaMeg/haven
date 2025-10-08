package org.haven.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event for policy decision audit trail
 * Captures every access control decision for compliance
 */
public class PolicyDecisionMade extends DomainEvent {

    private final UUID decisionId;
    private final boolean allowed;
    private final String reason;
    private final String policyRule;
    private final UUID userId;
    private final String userName;
    private final String resourceType;
    private final String decisionContext;
    private final String ipAddress;
    private final String sessionId;

    public PolicyDecisionMade(UUID decisionId, boolean allowed, String reason, String policyRule,
                             UUID userId, String userName, UUID resourceId, String resourceType,
                             String decisionContext, String ipAddress, String sessionId) {
        super(resourceId, Instant.now());
        this.decisionId = decisionId;
        this.allowed = allowed;
        this.reason = reason;
        this.policyRule = policyRule;
        this.userId = userId;
        this.userName = userName;
        this.resourceType = resourceType;
        this.decisionContext = decisionContext;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public String getPolicyRule() {
        return policyRule;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public UUID getResourceId() {
        return getAggregateId();
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getDecisionContext() {
        return decisionContext;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return String.format("PolicyDecisionMade[%s: %s for %s (rule: %s) by user %s at %s]",
                allowed ? "ALLOW" : "DENY",
                resourceType,
                getAggregateId(),
                policyRule,
                userId,
                occurredAt());
    }
}
