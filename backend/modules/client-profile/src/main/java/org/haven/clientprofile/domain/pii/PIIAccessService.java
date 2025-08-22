package org.haven.clientprofile.domain.pii;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PIIAccessService {
    
    private final PIIAccessRepository accessRepository;
    private final PIIAuditService auditService;
    
    public PIIAccessService(PIIAccessRepository accessRepository, PIIAuditService auditService) {
        this.accessRepository = accessRepository;
        this.auditService = auditService;
    }
    
    /**
     * Check if user has access to specific PII category for a client
     */
    public PIIAccessDecision checkAccess(PIIAccessContext context, UUID clientId, PIICategory category) {
        return checkAccess(context, clientId, category, category.getDefaultAccessLevel());
    }
    
    /**
     * Check if user has access to specific PII category at requested level
     */
    public PIIAccessDecision checkAccess(PIIAccessContext context, UUID clientId, 
                                       PIICategory category, PIIAccessLevel requestedLevel) {
        
        // Check if user has valid justification for high-risk categories
        if (!context.hasValidJustification(category)) {
            return PIIAccessDecision.denied("Missing business justification for " + category.name());
        }
        
        // Get user's maximum role-based access level
        PIIAccessLevel maxRoleAccess = context.getMaxAccessLevel();
        
        // Check if role allows the requested access level
        if (!maxRoleAccess.allowsAccess(requestedLevel)) {
            return PIIAccessDecision.denied(
                String.format("Role-based access level %s insufficient for requested level %s", 
                            maxRoleAccess.name(), requestedLevel.name()));
        }
        
        // Check for specific user permissions
        List<PIIAccessPermission> userPermissions = accessRepository.findActivePermissions(
            context.getUserId(), clientId, category);
        
        // Check if any permission allows the requested access
        Optional<PIIAccessPermission> allowingPermission = userPermissions.stream()
            .filter(p -> p.allowsAccess(requestedLevel) && p.allowsAccessToClient(clientId))
            .findFirst();
        
        if (allowingPermission.isEmpty()) {
            return PIIAccessDecision.denied("No explicit permission for requested access level");
        }
        
        // Log the access attempt
        auditService.logAccess(context, clientId, category, requestedLevel, true);
        
        return PIIAccessDecision.allowed(allowingPermission.get());
    }
    
    /**
     * Grant PII access permission to a user
     */
    public PIIAccessPermission grantAccess(UUID userId, UUID clientId, PIICategory category,
                                         PIIAccessLevel accessLevel, UUID grantedBy, 
                                         String reason, Instant expiresAt) {
        
        // Revoke any existing permissions for same user/client/category
        List<PIIAccessPermission> existing = accessRepository.findActivePermissions(userId, clientId, category);
        existing.forEach(p -> {
            p.revoke(grantedBy, "Replaced by new permission");
            accessRepository.save(p);
        });
        
        // Create new permission
        PIIAccessPermission permission = new PIIAccessPermission(
            userId, clientId, category, accessLevel, grantedBy, reason, expiresAt);
        
        return accessRepository.save(permission);
    }
    
    /**
     * Revoke PII access permission
     */
    public void revokeAccess(UUID userId, UUID clientId, PIICategory category, 
                           UUID revokedBy, String reason) {
        
        List<PIIAccessPermission> permissions = accessRepository.findActivePermissions(userId, clientId, category);
        permissions.forEach(p -> {
            p.revoke(revokedBy, reason);
            accessRepository.save(p);
        });
    }
    
    /**
     * Get all active permissions for a user
     */
    public List<PIIAccessPermission> getUserPermissions(UUID userId) {
        return accessRepository.findActivePermissionsByUser(userId);
    }
    
    /**
     * Get permissions expiring soon
     */
    public List<PIIAccessPermission> getExpiringPermissions(int daysWarning) {
        return accessRepository.findExpiringPermissions(daysWarning);
    }
    
    /**
     * Bulk grant role-based permissions based on templates
     */
    public void grantRoleBasedAccess(UUID userId, String roleName, UUID grantedBy) {
        List<RolePIIAccessTemplate> templates = accessRepository.findRoleTemplates(roleName);
        
        for (RolePIIAccessTemplate template : templates) {
            Instant expiresAt = template.getAutoExpiresDays() != null ?
                Instant.now().plusSeconds(template.getAutoExpiresDays() * 24 * 60 * 60) : null;
                
            grantAccess(userId, null, template.getCategory(), template.getMaxAccessLevel(),
                       grantedBy, "Role-based access: " + roleName, expiresAt);
        }
    }
    
    /**
     * Get redacted client data based on user's access level
     */
    public <T> T applyRedaction(T data, PIIAccessContext context, UUID clientId) {
        // Implementation moved to PIIRedactionService for better separation of concerns
        throw new UnsupportedOperationException("Use PIIRedactionService.applyRedaction() instead");
    }
}