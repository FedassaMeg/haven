package org.haven.clientprofile.domain.pii;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PIIAccessContext {
    private final UUID userId;
    private final List<String> userRoles;
    private final String businessJustification;
    private final UUID caseId;
    private final String sessionId;
    private final String ipAddress;
    private final Instant accessTime;
    
    public PIIAccessContext(UUID userId, List<String> userRoles, String businessJustification,
                           UUID caseId, String sessionId, String ipAddress) {
        this.userId = userId;
        this.userRoles = userRoles;
        this.businessJustification = businessJustification;
        this.caseId = caseId;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.accessTime = Instant.now();
    }
    
    public boolean hasRole(String role) {
        return userRoles.contains(role);
    }
    
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    public PIIAccessLevel getMaxAccessLevel() {
        // Determine maximum access level based on roles
        if (hasRole("ADMINISTRATOR")) {
            return PIIAccessLevel.HIGHLY_CONFIDENTIAL;
        }
        if (hasRole("SUPERVISOR")) {
            return PIIAccessLevel.CONFIDENTIAL;
        }
        if (hasAnyRole("CASE_MANAGER", "SOCIAL_WORKER")) {
            return PIIAccessLevel.RESTRICTED;
        }
        if (hasRole("INTAKE_COORDINATOR")) {
            return PIIAccessLevel.INTERNAL;
        }
        return PIIAccessLevel.PUBLIC;
    }
    
    public boolean requiresJustification(PIICategory category) {
        return category.isHighRisk() || 
               (category == PIICategory.DIRECT_IDENTIFIER && !hasAnyRole("CASE_MANAGER", "SUPERVISOR"));
    }
    
    public boolean hasValidJustification(PIICategory category) {
        if (!requiresJustification(category)) {
            return true;
        }
        return businessJustification != null && !businessJustification.trim().isEmpty();
    }
    
    public boolean hasAccess(PIICategory category, PIIAccessLevel requiredLevel) {
        if (!hasValidJustification(category)) {
            return false;
        }
        
        PIIAccessLevel userMaxLevel = getMaxAccessLevel();
        return userMaxLevel.ordinal() >= requiredLevel.ordinal();
    }
    
    /**
     * Checks if the user is an assigned case worker for the given client
     */
    public boolean isAssignedCaseWorker(UUID clientId) {
        // In a real implementation, this would check against case assignments
        // For now, return true if user is a case manager with a valid case ID
        return hasRole("CASE_MANAGER") && caseId != null;
    }
    
    /**
     * Checks if the user has legal authorization (e.g., court order)
     */
    public boolean hasLegalAuthorization() {
        // Check for specific legal authorization role or justification
        return hasRole("LEGAL_AUTHORITY") || 
               (businessJustification != null && businessJustification.toLowerCase().contains("court order"));
    }
    
    /**
     * Checks if the access is anonymous (no user context)
     */
    public boolean isAnonymous() {
        return userId == null || userRoles == null || userRoles.isEmpty();
    }
    
    // Getters
    public UUID getUserId() { return userId; }
    public List<String> getUserRoles() { return List.copyOf(userRoles); }
    public String getBusinessJustification() { return businessJustification; }
    public UUID getCaseId() { return caseId; }
    public String getSessionId() { return sessionId; }
    public String getIpAddress() { return ipAddress; }
    public Instant getAccessTime() { return accessTime; }
}