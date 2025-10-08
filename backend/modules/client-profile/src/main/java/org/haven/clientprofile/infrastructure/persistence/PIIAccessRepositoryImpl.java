package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.pii.PIIAccessPermission;
import org.haven.clientprofile.domain.pii.PIIAccessRepository;
import org.haven.clientprofile.domain.pii.PIICategory;
import org.haven.clientprofile.domain.pii.RolePIIAccessTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PIIAccessRepositoryImpl implements PIIAccessRepository {
    
    @Autowired
    private JpaPIIAccessRepository jpaPIIAccessRepository;
    
    @Autowired
    private JpaRolePIIAccessTemplateRepository roleTemplateRepository;
    
    @Override
    public PIIAccessPermission save(PIIAccessPermission permission) {
        JpaPIIAccessPermissionEntity entity = JpaPIIAccessPermissionEntity.fromDomain(permission);
        JpaPIIAccessPermissionEntity saved = jpaPIIAccessRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    public List<PIIAccessPermission> findActivePermissions(UUID userId, UUID clientId, PIICategory category) {
        return jpaPIIAccessRepository.findActivePermissionEntities(userId, clientId, category, Instant.now()).stream()
            .map(JpaPIIAccessPermissionEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PIIAccessPermission> findActivePermissionsByUser(UUID userId) {
        return jpaPIIAccessRepository.findActivePermissionsByUserEntity(userId, Instant.now()).stream()
            .map(JpaPIIAccessPermissionEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PIIAccessPermission> findExpiringPermissions(int daysWarning) {
        Instant now = Instant.now();
        Instant warningDate = now.plusSeconds(daysWarning * 24 * 60 * 60);
        return jpaPIIAccessRepository.findExpiringPermissionEntities(warningDate, now).stream()
            .map(JpaPIIAccessPermissionEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RolePIIAccessTemplate> findRoleTemplates(String roleName) {
        return roleTemplateRepository.findByRoleName(roleName).stream()
            .map(JpaRolePIIAccessTemplateEntity::toDomain)
            .collect(Collectors.toList());
    }
}