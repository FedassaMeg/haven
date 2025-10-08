package org.haven.clientprofile.domain.pii;

import java.util.List;
import java.util.UUID;

public interface PIIAccessRepository {
    
    PIIAccessPermission save(PIIAccessPermission permission);
    
    List<PIIAccessPermission> findActivePermissions(UUID userId, UUID clientId, PIICategory category);
    
    List<PIIAccessPermission> findActivePermissionsByUser(UUID userId);
    
    List<PIIAccessPermission> findExpiringPermissions(int daysWarning);
    
    List<RolePIIAccessTemplate> findRoleTemplates(String roleName);
}