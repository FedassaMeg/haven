package org.haven.shared.vo.hmis;

/**
 * HMIS Information Date types for data collection timing
 * Determines when and why income/benefits data was collected
 */
public enum InformationDate {
    
    START_OF_PROJECT("START", "Collected at project start/entry"),
    UPDATE("UPDATE", "Collected due to change in circumstances"),
    ANNUAL_ASSESSMENT("ANNUAL", "Collected at annual assessment"),
    EXIT("EXIT", "Collected at project exit"),
    MINOR_TURNING_18("MINOR18", "Collected when minor client turns 18");
    
    private final String code;
    private final String description;
    
    InformationDate(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isLifecycleEvent() {
        return this == START_OF_PROJECT || this == EXIT;
    }
    
    public boolean isPeriodicAssessment() {
        return this == ANNUAL_ASSESSMENT;
    }
    
    public boolean isChangeTriggered() {
        return this == UPDATE || this == MINOR_TURNING_18;
    }
}