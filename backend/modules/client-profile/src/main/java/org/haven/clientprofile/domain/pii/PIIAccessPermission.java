package org.haven.clientprofile.domain.pii;

import java.time.Instant;
import java.util.UUID;

public class PIIAccessPermission {
    private final UUID userId;
    private final UUID clientId; // null for global access
    private final PIICategory category;
    private final PIIAccessLevel accessLevel;
    private final UUID grantedBy;
    private final String grantedReason;
    private final Instant grantedAt;
    private final Instant expiresAt; // null for permanent
    private boolean isRevoked;
    private Instant revokedAt;
    private UUID revokedBy;
    private String revokedReason;
    
    public PIIAccessPermission(UUID userId, UUID clientId, PIICategory category,
                              PIIAccessLevel accessLevel, UUID grantedBy, String grantedReason) {
        this(userId, clientId, category, accessLevel, grantedBy, grantedReason, null);
    }
    
    public PIIAccessPermission(UUID userId, UUID clientId, PIICategory category,
                              PIIAccessLevel accessLevel, UUID grantedBy, String grantedReason,
                              Instant expiresAt) {
        this.userId = userId;
        this.clientId = clientId;
        this.category = category;
        this.accessLevel = accessLevel;
        this.grantedBy = grantedBy;
        this.grantedReason = grantedReason;
        this.grantedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.isRevoked = false;
    }
    
    public boolean isValid() {
        if (isRevoked) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
    
    public boolean allowsAccess(PIIAccessLevel requestedLevel) {
        return isValid() && accessLevel.allowsAccess(requestedLevel);
    }
    
    public boolean allowsAccessToClient(UUID requestedClientId) {
        return isValid() && (clientId == null || clientId.equals(requestedClientId));
    }
    
    public void revoke(UUID revokedBy, String reason) {
        this.isRevoked = true;
        this.revokedAt = Instant.now();
        this.revokedBy = revokedBy;
        this.revokedReason = reason;
    }
    
    public boolean isExpiringSoon(int daysWarning) {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.isBefore(Instant.now().plusSeconds(daysWarning * 24 * 60 * 60));
    }
    
    public boolean isGlobalAccess() {
        return clientId == null;
    }
    
    public String getAccessSummary() {
        return String.format("%s access to %s for %s", 
                           accessLevel.name(), 
                           category.name(),
                           isGlobalAccess() ? "all clients" : "specific client");
    }
    
    // Getters
    public UUID getUserId() { return userId; }
    public UUID getClientId() { return clientId; }
    public PIICategory getCategory() { return category; }
    public PIIAccessLevel getAccessLevel() { return accessLevel; }
    public UUID getGrantedBy() { return grantedBy; }
    public String getGrantedReason() { return grantedReason; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return isRevoked; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getRevokedBy() { return revokedBy; }
    public String getRevokedReason() { return revokedReason; }
}