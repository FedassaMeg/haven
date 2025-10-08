package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.pii.PIIAccessLevel;
import org.haven.clientprofile.domain.pii.PIICategory;
import org.haven.clientprofile.domain.pii.RolePIIAccessTemplate;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "role_pii_access_templates")
public class JpaRolePIIAccessTemplateEntity {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "role_name", nullable = false)
    private String roleName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PIICategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "max_access_level", nullable = false)
    private PIIAccessLevel maxAccessLevel;
    
    @Column(name = "requires_justification", nullable = false)
    private boolean requiresJustification;
    
    @Column(name = "auto_expires_days")
    private Integer autoExpiresDays;
    
    // Default constructor for JPA
    public JpaRolePIIAccessTemplateEntity() {}
    
    public static JpaRolePIIAccessTemplateEntity fromDomain(RolePIIAccessTemplate template) {
        JpaRolePIIAccessTemplateEntity entity = new JpaRolePIIAccessTemplateEntity();
        entity.roleName = template.getRoleName();
        entity.category = template.getCategory();
        entity.maxAccessLevel = template.getMaxAccessLevel();
        entity.requiresJustification = template.isRequiresJustification();
        entity.autoExpiresDays = template.getAutoExpiresDays();
        return entity;
    }
    
    public RolePIIAccessTemplate toDomain() {
        return new RolePIIAccessTemplate(
            roleName, category, maxAccessLevel, requiresJustification, autoExpiresDays
        );
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    
    public PIICategory getCategory() { return category; }
    public void setCategory(PIICategory category) { this.category = category; }
    
    public PIIAccessLevel getMaxAccessLevel() { return maxAccessLevel; }
    public void setMaxAccessLevel(PIIAccessLevel maxAccessLevel) { this.maxAccessLevel = maxAccessLevel; }
    
    public boolean isRequiresJustification() { return requiresJustification; }
    public void setRequiresJustification(boolean requiresJustification) { this.requiresJustification = requiresJustification; }
    
    public Integer getAutoExpiresDays() { return autoExpiresDays; }
    public void setAutoExpiresDays(Integer autoExpiresDays) { this.autoExpiresDays = autoExpiresDays; }
}