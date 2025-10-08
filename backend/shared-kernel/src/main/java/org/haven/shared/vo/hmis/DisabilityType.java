package org.haven.shared.vo.hmis;

/**
 * HMIS Disability Types - FY2024 Data Standards
 * Covers Physical Disability, Developmental Disability, Chronic Health Condition,
 * HIV/AIDS, Mental Health Disorder, and Substance Use Disorder
 * Aligns with HMIS Data Elements 4.05-4.10
 */
public enum DisabilityType {
    
    // Response values for each disability type
    NO(0, "No"),
    YES(1, "Yes"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    DisabilityType(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }
    
    public int getHmisValue() {
        return hmisValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isKnownResponse() {
        return this != CLIENT_DOESNT_KNOW && 
               this != CLIENT_REFUSED && 
               this != DATA_NOT_COLLECTED;
    }
    
    public boolean isAffirmative() {
        return this == YES;
    }
    
    public boolean isNegative() {
        return this == NO;
    }
}