package org.haven.shared.vo.hmis;

/**
 * HMIS Five-Point Response Scale - FY2024 Data Standards
 * Standard response pattern used for "from any source" questions across HMIS data elements
 * Used for Income from Any Source, Benefits from Any Source, etc.
 */
public enum HmisFivePointResponse {
    
    NO(0, "No"),
    YES(1, "Yes"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    HmisFivePointResponse(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }
    
    public int getHmisValue() {
        return hmisValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this is a known/valid response (not DK/Refused/DNC)
     */
    public boolean isKnownResponse() {
        return this == YES || this == NO;
    }
    
    /**
     * Check if this indicates presence/affirmative response
     */
    public boolean isAffirmative() {
        return this == YES;
    }
    
    /**
     * Check if this indicates absence/negative response
     */
    public boolean isNegative() {
        return this == NO;
    }
    
    /**
     * Check if this is an unknown/missing response
     */
    public boolean isUnknownOrMissing() {
        return this == CLIENT_DOESNT_KNOW || this == CLIENT_REFUSED || this == DATA_NOT_COLLECTED;
    }
    
    /**
     * Map from integer HMIS value to enum
     */
    public static HmisFivePointResponse fromHmisValue(Integer value) {
        if (value == null) return DATA_NOT_COLLECTED;
        
        return switch (value) {
            case 0 -> NO;
            case 1 -> YES;
            case 8 -> CLIENT_DOESNT_KNOW;
            case 9 -> CLIENT_REFUSED;
            case 99 -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
    
    /**
     * Check if response requires detail collection
     * For "from any source" questions, if YES then individual sources must be collected
     */
    public boolean requiresDetailCollection() {
        return this == YES;
    }
    
    /**
     * Check if response prohibits detail collection  
     * For "from any source" questions, if NO then individual sources should not be collected
     */
    public boolean prohibitsDetailCollection() {
        return this == NO;
    }
}