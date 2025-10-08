package org.haven.shared.vo.hmis;

/**
 * HMIS Data Collection Stage - FY2024 Data Standards
 * Represents the three main stages of HMIS data collection
 * Used across multiple data elements for lifecycle tracking
 */
public enum DataCollectionStage {
    
    PROJECT_START("PROJECT_START", "Project Start", "Data collected at project entry"),
    UPDATE("UPDATE", "Update", "Data collected due to change in circumstances"),
    PROJECT_EXIT("PROJECT_EXIT", "Project Exit", "Data collected at project exit");
    
    private final String databaseValue;
    private final String displayName;
    private final String description;
    
    DataCollectionStage(String databaseValue, String displayName, String description) {
        this.databaseValue = databaseValue;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDatabaseValue() {
        return databaseValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this stage occurs at enrollment start
     */
    public boolean isProjectStart() {
        return this == PROJECT_START;
    }
    
    /**
     * Check if this stage occurs during enrollment
     */
    public boolean isUpdate() {
        return this == UPDATE;
    }
    
    /**
     * Check if this stage occurs at enrollment exit
     */
    public boolean isProjectExit() {
        return this == PROJECT_EXIT;
    }
    
    /**
     * Check if this stage requires baseline/initial data collection
     */
    public boolean isBaselineStage() {
        return this == PROJECT_START;
    }
    
    /**
     * Check if this stage represents a change in circumstances
     */
    public boolean isChangeStage() {
        return this == UPDATE;
    }
    
    /**
     * Check if this stage represents final data collection
     */
    public boolean isFinalStage() {
        return this == PROJECT_EXIT;
    }
    
    /**
     * Map from database enum string to domain enum
     */
    public static DataCollectionStage fromDatabaseValue(String dbValue) {
        if (dbValue == null) {
            return PROJECT_START; // Default fallback
        }
        
        return switch (dbValue.toUpperCase()) {
            case "PROJECT_START" -> PROJECT_START;
            case "UPDATE" -> UPDATE;
            case "PROJECT_EXIT" -> PROJECT_EXIT;
            default -> PROJECT_START; // Default fallback
        };
    }
    
    /**
     * Convert to database enum string
     */
    public String toDatabaseValue() {
        return databaseValue;
    }
    
    /**
     * Get all stages in chronological order
     */
    public static DataCollectionStage[] getChronologicalOrder() {
        return new DataCollectionStage[]{PROJECT_START, UPDATE, PROJECT_EXIT};
    }
    
    /**
     * Get the next logical stage in the data collection lifecycle
     */
    public DataCollectionStage getNextStage() {
        return switch (this) {
            case PROJECT_START -> UPDATE;
            case UPDATE -> PROJECT_EXIT;
            case PROJECT_EXIT -> null; // No stage after exit
        };
    }
    
    /**
     * Get the previous stage in the data collection lifecycle
     */
    public DataCollectionStage getPreviousStage() {
        return switch (this) {
            case PROJECT_START -> null; // No stage before start
            case UPDATE -> PROJECT_START;
            case PROJECT_EXIT -> UPDATE;
        };
    }
}