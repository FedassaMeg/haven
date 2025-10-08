package org.haven.shared.rbac;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RbacRoleRepository extends JpaRepository<RbacRole, UUID> {

    Optional<RbacRole> findByRoleName(String roleName);

    Optional<RbacRole> findByRoleEnum(UserRole roleEnum);

    Optional<RbacRole> findByKeycloakRoleId(String keycloakRoleId);

    List<RbacRole> findByIsActiveTrue();

    @Query("SELECT r FROM RbacRole r WHERE r.keycloakRoleId IS NOT NULL")
    List<RbacRole> findByKeycloakRoleIdNotNull();

    List<RbacRole> findByIsCompositeTrue();
}
