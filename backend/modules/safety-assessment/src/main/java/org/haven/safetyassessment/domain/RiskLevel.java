package org.haven.safetyassessment.domain;

/**
 * Standardized risk levels for DV safety assessments
 */
public enum RiskLevel {
    /**
     * Imminent danger - immediate safety intervention required
     * Triggers automatic safety protocols
     */
    EXTREME(5, "Imminent Danger", "Immediate safety intervention required"),
    
    /**
     * High risk - elevated safety concerns requiring close monitoring
     */
    HIGH(4, "High Risk", "Elevated safety concerns requiring close monitoring"),
    
    /**
     * Moderate risk - standard safety planning appropriate
     */
    MODERATE(3, "Moderate Risk", "Standard safety planning appropriate"),
    
    /**
     * Low risk - basic safety awareness and planning
     */
    LOW(2, "Low Risk", "Basic safety awareness and planning"),
    
    /**
     * Minimal risk - routine safety check-ins
     */
    MINIMAL(1, "Minimal Risk", "Routine safety check-ins");
    
    private final int numericValue;
    private final String displayName;
    private final String description;
    
    RiskLevel(int numericValue, String displayName, String description) {
        this.numericValue = numericValue;
        this.displayName = displayName;
        this.description = description;
    }
    
    public int getNumericValue() { return numericValue; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    public boolean requiresImmediateAction() {
        return this == EXTREME;
    }
    
    public boolean requiresEnhancedMonitoring() {
        return this == EXTREME || this == HIGH;
    }
    
    public static RiskLevel fromNumericValue(int value) {
        for (RiskLevel level : values()) {
            if (level.numericValue == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid risk level value: " + value);
    }
}