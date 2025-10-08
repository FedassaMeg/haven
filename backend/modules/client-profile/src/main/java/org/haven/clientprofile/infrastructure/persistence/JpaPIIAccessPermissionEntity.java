package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.pii.PIIAccessLevel;
import org.haven.clientprofile.domain.pii.PIIAccessPermission;
import org.haven.clientprofile.domain.pii.PIICategory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pii_access_permissions")
public class JpaPIIAccessPermissionEntity {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "client_id")
    private UUID clientId; // null for global access
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PIICategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private PIIAccessLevel accessLevel;
    
    @Column(name = "granted_by", nullable = false)
    private UUID grantedBy;
    
    @Column(name = "granted_reason", nullable = false)
    private String grantedReason;
    
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt; // null for permanent
    
    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked = false;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "revoked_by")
    private UUID revokedBy;
    
    @Column(name = "revoked_reason")
    private String revokedReason;
    
    // Default constructor for JPA
    public JpaPIIAccessPermissionEntity() {}
    
    public static JpaPIIAccessPermissionEntity fromDomain(PIIAccessPermission permission) {
        JpaPIIAccessPermissionEntity entity = new JpaPIIAccessPermissionEntity();
        entity.userId = permission.getUserId();
        entity.clientId = permission.getClientId();
        entity.category = permission.getCategory();
        entity.accessLevel = permission.getAccessLevel();
        entity.grantedBy = permission.getGrantedBy();
        entity.grantedReason = permission.getGrantedReason();
        entity.grantedAt = permission.getGrantedAt();
        entity.expiresAt = permission.getExpiresAt();
        entity.isRevoked = permission.isRevoked();
        entity.revokedAt = permission.getRevokedAt();
        entity.revokedBy = permission.getRevokedBy();
        entity.revokedReason = permission.getRevokedReason();
        return entity;
    }
    
    public PIIAccessPermission toDomain() {
        PIIAccessPermission permission = new PIIAccessPermission(
            userId, clientId, category, accessLevel, grantedBy, grantedReason, expiresAt
        );
        
        if (isRevoked && revokedBy != null && revokedReason != null) {
            permission.revoke(revokedBy, revokedReason);
        }
        
        return permission;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public PIICategory getCategory() { return category; }
    public void setCategory(PIICategory category) { this.category = category; }
    
    public PIIAccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(PIIAccessLevel accessLevel) { this.accessLevel = accessLevel; }
    
    public UUID getGrantedBy() { return grantedBy; }
    public void setGrantedBy(UUID grantedBy) { this.grantedBy = grantedBy; }
    
    public String getGrantedReason() { return grantedReason; }
    public void setGrantedReason(String grantedReason) { this.grantedReason = grantedReason; }
    
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public boolean isRevoked() { return isRevoked; }
    public void setRevoked(boolean revoked) { isRevoked = revoked; }
    
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    
    public UUID getRevokedBy() { return revokedBy; }
    public void setRevokedBy(UUID revokedBy) { this.revokedBy = revokedBy; }
    
    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }
}