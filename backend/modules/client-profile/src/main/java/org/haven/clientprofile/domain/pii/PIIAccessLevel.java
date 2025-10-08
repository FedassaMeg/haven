package org.haven.clientprofile.domain.pii;

public enum PIIAccessLevel {
    PUBLIC("No PII restrictions", 0),
    INTERNAL("Organization staff only", 1),
    RESTRICTED("Case team + supervisors only", 2),
    CONFIDENTIAL("Designated staff only", 3),
    HIGHLY_CONFIDENTIAL("Minimal access, specific authorization", 4);
    
    private final String description;
    private final int level;
    
    PIIAccessLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    
    public boolean isMoreRestrictiveThan(PIIAccessLevel other) {
        return this.level > other.level;
    }
    
    public boolean isLessRestrictiveThan(PIIAccessLevel other) {
        return this.level < other.level;
    }
    
    public boolean allowsAccess(PIIAccessLevel requestedLevel) {
        return this.level >= requestedLevel.level;
    }
}