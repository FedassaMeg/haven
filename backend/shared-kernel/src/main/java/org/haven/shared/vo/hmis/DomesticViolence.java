package org.haven.shared.vo.hmis;

/**
 * HMIS Domestic Violence - FY2024 Data Standards
 * HMIS Universal Data Element for Domestic Violence History
 * Aligns with HMIS Data Elements 4.11 Domestic Violence
 */
public enum DomesticViolence {
    
    NO(0, "No"),
    YES(1, "Yes"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),  
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    DomesticViolence(int hmisValue, String description) {
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
    
    public boolean hasHistory() {
        return this == YES;
    }
    
    public boolean noHistory() {
        return this == NO;
    }
    
    /**
     * Indicates this is sensitive data requiring special handling
     */
    public boolean requiresConfidentialHandling() {
        return this == YES;
    }
}