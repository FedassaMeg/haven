package org.haven.readmodels.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model for policy decision audit trail
 * Queryable projection for compliance reporting
 */
@Entity
@Table(name = "policy_decision_log", indexes = {
    @Index(name = "idx_policy_user", columnList = "user_id"),
    @Index(name = "idx_policy_resource", columnList = "resource_id, resource_type"),
    @Index(name = "idx_policy_rule", columnList = "policy_rule"),
    @Index(name = "idx_policy_decided_at", columnList = "decided_at"),
    @Index(name = "idx_policy_allowed", columnList = "allowed")
})
public class PolicyDecisionLog {

    @Id
    private UUID decisionId;

    @Column(nullable = false)
    private boolean allowed;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 100)
    private String policyRule;

    @Column(nullable = false)
    private UUID userId;

    @Column(length = 255)
    private String userName;

    @Column(nullable = false)
    private UUID resourceId;

    @Column(nullable = false, length = 100)
    private String resourceType;

    @Column(nullable = false)
    private Instant decidedAt;

    @Column(length = 2000)
    private String decisionContext;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 255)
    private String sessionId;

    @Column(length = 500)
    private String userAgent;

    protected PolicyDecisionLog() {
        // JPA constructor
    }

    public PolicyDecisionLog(UUID decisionId, boolean allowed, String reason, String policyRule,
                            UUID userId, String userName, UUID resourceId, String resourceType,
                            Instant decidedAt, String decisionContext, String ipAddress,
                            String sessionId, String userAgent) {
        this.decisionId = decisionId;
        this.allowed = allowed;
        this.reason = reason;
        this.policyRule = policyRule;
        this.userId = userId;
        this.userName = userName;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.decidedAt = decidedAt;
        this.decisionContext = decisionContext;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.userAgent = userAgent;
    }

    // Getters
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
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Instant getDecidedAt() {
        return decidedAt;
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

    public String getUserAgent() {
        return userAgent;
    }
}
