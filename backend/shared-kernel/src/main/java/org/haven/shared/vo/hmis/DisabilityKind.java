package org.haven.shared.vo.hmis;

/**
 * HMIS Disability Kinds - FY2024 Data Standards
 * Represents the different types of disabilities tracked in HMIS
 * Maps to UDE 3.08-3.13 disability types
 */
public enum DisabilityKind {
    
    PHYSICAL("PHYSICAL", "Physical Disability", "3.08"),
    DEVELOPMENTAL("DEVELOPMENTAL", "Developmental Disability", "3.09"),
    CHRONIC_HEALTH_CONDITION("CHRONIC_HEALTH_CONDITION", "Chronic Health Condition", "3.10"),
    HIV_AIDS("HIV_AIDS", "HIV/AIDS", "3.11"),
    MENTAL_HEALTH("MENTAL_HEALTH", "Mental Health Disorder", "3.12"),
    SUBSTANCE_USE("SUBSTANCE_USE", "Substance Use Disorder", "3.13");
    
    private final String databaseValue;
    private final String displayName;
    private final String udeNumber;
    
    DisabilityKind(String databaseValue, String displayName, String udeNumber) {
        this.databaseValue = databaseValue;
        this.displayName = displayName;
        this.udeNumber = udeNumber;
    }
    
    public String getDatabaseValue() {
        return databaseValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getUdeNumber() {
        return udeNumber;
    }
    
    /**
     * Check if this is a behavioral health disability
     */
    public boolean isBehavioralHealth() {
        return this == MENTAL_HEALTH || this == SUBSTANCE_USE;
    }
    
    /**
     * Check if this is a medical disability
     */
    public boolean isMedical() {
        return this == CHRONIC_HEALTH_CONDITION || this == HIV_AIDS;
    }
    
    /**
     * Map from database enum string to domain enum
     */
    public static DisabilityKind fromDatabaseValue(String dbValue) {
        if (dbValue == null) return null;
        
        return switch (dbValue.toUpperCase()) {
            case "PHYSICAL" -> PHYSICAL;
            case "DEVELOPMENTAL" -> DEVELOPMENTAL;
            case "CHRONIC_HEALTH_CONDITION" -> CHRONIC_HEALTH_CONDITION;
            case "HIV_AIDS" -> HIV_AIDS;
            case "MENTAL_HEALTH" -> MENTAL_HEALTH;
            case "SUBSTANCE_USE" -> SUBSTANCE_USE;
            default -> null;
        };
    }
    
    /**
     * Convert to database enum string
     */
    public String toDatabaseValue() {
        return databaseValue;
    }
}