package org.haven.shared.vo.hmis;

/**
 * HMIS Income from Any Source - FY2024 Data Standards
 * Overall income status before collecting individual sources
 * Aligns with HMIS Data Elements 4.02.1 Income from Any Source
 */
public enum IncomeFromAnySource {
    
    NO(0, "No"),
    YES(1, "Yes"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    IncomeFromAnySource(int hmisValue, String description) {
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
    
    public boolean hasIncome() {
        return this == YES;
    }
    
    public boolean noIncome() {
        return this == NO;
    }
    
    /**
     * Determines if individual source collection is required
     * Per HMIS standards, individual sources only collected if YES
     */
    public boolean requiresSourceCollection() {
        return this == YES;
    }
}