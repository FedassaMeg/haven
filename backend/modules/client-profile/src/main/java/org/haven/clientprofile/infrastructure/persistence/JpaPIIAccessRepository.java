package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.pii.PIIAccessPermission;
import org.haven.clientprofile.domain.pii.PIIAccessRepository;
import org.haven.clientprofile.domain.pii.PIICategory;
import org.haven.clientprofile.domain.pii.RolePIIAccessTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface JpaPIIAccessRepository extends JpaRepository<JpaPIIAccessPermissionEntity, UUID> {
    
    @Query("SELECT p FROM JpaPIIAccessPermissionEntity p WHERE " +
           "p.userId = :userId AND " +
           "(p.clientId = :clientId OR p.clientId IS NULL) AND " +
           "p.category = :category AND " +
           "p.isRevoked = false AND " +
           "(p.expiresAt IS NULL OR p.expiresAt > :now)")
    List<JpaPIIAccessPermissionEntity> findActivePermissionEntities(
        @Param("userId") UUID userId, 
        @Param("clientId") UUID clientId, 
        @Param("category") PIICategory category,
        @Param("now") Instant now
    );
    
    @Query("SELECT p FROM JpaPIIAccessPermissionEntity p WHERE " +
           "p.userId = :userId AND " +
           "p.isRevoked = false AND " +
           "(p.expiresAt IS NULL OR p.expiresAt > :now)")
    List<JpaPIIAccessPermissionEntity> findActivePermissionsByUserEntity(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );
    
    @Query("SELECT p FROM JpaPIIAccessPermissionEntity p WHERE " +
           "p.isRevoked = false AND " +
           "p.expiresAt IS NOT NULL AND " +
           "p.expiresAt <= :warningDate AND " +
           "p.expiresAt > :now")
    List<JpaPIIAccessPermissionEntity> findExpiringPermissionEntities(
        @Param("warningDate") Instant warningDate,
        @Param("now") Instant now
    );
}