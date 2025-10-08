package org.haven.shared.rbac;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Role definition synchronized with Keycloak realm roles.
 */
@Entity
@Table(name = "rbac_roles", schema = "haven")
public class RbacRole {

    @Id
    private UUID id;

    @Column(name = "keycloak_role_id", unique = true)
    private String keycloakRoleId;

    @Column(name = "role_name", unique = true, nullable = false, length = 100)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_enum", unique = true, nullable = false)
    private UserRole roleEnum;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_composite")
    private Boolean isComposite = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "requires_mfa")
    private Boolean requiresMfa = false;

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes = 480;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKeycloakRoleId() {
        return keycloakRoleId;
    }

    public void setKeycloakRoleId(String keycloakRoleId) {
        this.keycloakRoleId = keycloakRoleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public UserRole getRoleEnum() {
        return roleEnum;
    }

    public void setRoleEnum(UserRole roleEnum) {
        this.roleEnum = roleEnum;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsComposite() {
        return isComposite;
    }

    public void setIsComposite(Boolean isComposite) {
        this.isComposite = isComposite;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getRequiresMfa() {
        return requiresMfa;
    }

    public void setRequiresMfa(Boolean requiresMfa) {
        this.requiresMfa = requiresMfa;
    }

    public Integer getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
