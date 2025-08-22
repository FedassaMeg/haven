package org.haven.clientprofile.domain.pii;

public class RolePIIAccessTemplate {
    private final String roleName;
    private final PIICategory category;
    private final PIIAccessLevel maxAccessLevel;
    private final boolean requiresJustification;
    private final Integer autoExpiresDays;
    
    public RolePIIAccessTemplate(String roleName, PIICategory category, PIIAccessLevel maxAccessLevel,
                                boolean requiresJustification, Integer autoExpiresDays) {
        this.roleName = roleName;
        this.category = category;
        this.maxAccessLevel = maxAccessLevel;
        this.requiresJustification = requiresJustification;
        this.autoExpiresDays = autoExpiresDays;
    }
    
    // Getters
    public String getRoleName() { return roleName; }
    public PIICategory getCategory() { return category; }
    public PIIAccessLevel getMaxAccessLevel() { return maxAccessLevel; }
    public boolean isRequiresJustification() { return requiresJustification; }
    public Integer getAutoExpiresDays() { return autoExpiresDays; }
}