package org.haven.shared.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an access control policy decision
 * Immutable value object with full audit trail
 */
public class PolicyDecision {

    private final UUID decisionId;
    private final boolean allowed;
    private final String reason;
    private final String policyRule;
    private final UUID userId;
    private final UUID resourceId;
    private final String resourceType;
    private final Instant decidedAt;
    private final String decisionContext;
    private final String policyName;
    private final String policyVersion;
    private final Map<String, Object> metadata;

    private PolicyDecision(UUID decisionId, boolean allowed, String reason, String policyRule,
                          UUID userId, UUID resourceId, String resourceType,
                          Instant decidedAt, String decisionContext, String policyName,
                          String policyVersion, Map<String, Object> metadata) {
        this.decisionId = decisionId;
        this.allowed = allowed;
        this.reason = reason;
        this.policyRule = policyRule;
        this.userId = userId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.decidedAt = decidedAt;
        this.decisionContext = decisionContext;
        this.policyName = policyName;
        this.policyVersion = policyVersion;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static PolicyDecision allow(String reason, String policyRule, UUID userId,
                                      UUID resourceId, String resourceType, String context) {
        return new PolicyDecision(
                UUID.randomUUID(),
                true,
                reason,
                policyRule,
                userId,
                resourceId,
                resourceType,
                Instant.now(),
                context,
                policyRule,
                "1.0",
                new HashMap<>()
        );
    }

    public static PolicyDecision deny(String reason, String policyRule, UUID userId,
                                     UUID resourceId, String resourceType, String context) {
        return new PolicyDecision(
                UUID.randomUUID(),
                false,
                reason,
                policyRule,
                userId,
                resourceId,
                resourceType,
                Instant.now(),
                context,
                policyRule,
                "1.0",
                new HashMap<>()
        );
    }

    /**
     * Create permit decision with policy name, version, and metadata
     */
    public static PolicyDecision permit(String policyName, String policyVersion, String reason,
                                       Map<String, Object> metadata, AccessContext accessContext) {
        return new PolicyDecision(
                UUID.randomUUID(),
                true,
                reason,
                policyName,
                accessContext.getUserId(),
                UUID.randomUUID(), // Resource ID would be extracted from context
                "ExportJob",
                Instant.now(),
                accessContext.getAccessReason(),
                policyName,
                policyVersion,
                metadata
        );
    }

    /**
     * Create deny decision with policy name, version, and metadata
     */
    public static PolicyDecision deny(String policyName, String policyVersion, String reason,
                                     Map<String, Object> metadata, AccessContext accessContext) {
        return new PolicyDecision(
                UUID.randomUUID(),
                false,
                reason,
                policyName,
                accessContext.getUserId(),
                UUID.randomUUID(), // Resource ID would be extracted from context
                "ExportJob",
                Instant.now(),
                accessContext.getAccessReason(),
                policyName,
                policyVersion,
                metadata
        );
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isPermitted() {
        return allowed;
    }

    public boolean isDenied() {
        return !allowed;
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

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public String toString() {
        return String.format("PolicyDecision[%s: %s - %s (rule: %s)]",
                allowed ? "ALLOW" : "DENY", resourceType, reason, policyRule);
    }
}
