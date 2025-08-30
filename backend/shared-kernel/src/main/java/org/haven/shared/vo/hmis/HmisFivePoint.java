package org.haven.shared.vo.hmis;

/**
 * HMIS Five-Point Response Scale
 * Standard response options for non-boolean HMIS data elements
 * Used across multiple data elements like disabilities, DV, etc.
 */
public enum HmisFivePoint {
    
    YES(1, "Yes"),
    NO(0, "No"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    HmisFivePoint(int hmisValue, String description) {
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
     * Check if this indicates affirmative response
     */
    public boolean isYes() {
        return this == YES;
    }
    
    /**
     * Check if this indicates negative response
     */
    public boolean isNo() {
        return this == NO;
    }
    
    /**
     * Check if response was not collected or refused
     */
    public boolean isNotCollected() {
        return this == CLIENT_DOESNT_KNOW || 
               this == CLIENT_REFUSED || 
               this == DATA_NOT_COLLECTED;
    }
    
    /**
     * Map from integer HMIS value to enum
     */
    public static HmisFivePoint fromHmisValue(Integer value) {
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
     * Map from database enum string to domain enum
     */
    public static HmisFivePoint fromDatabaseValue(String dbValue) {
        if (dbValue == null) return DATA_NOT_COLLECTED;
        
        return switch (dbValue.toUpperCase()) {
            case "YES" -> YES;
            case "NO" -> NO;
            case "CLIENT_DOESNT_KNOW" -> CLIENT_DOESNT_KNOW;
            case "CLIENT_REFUSED" -> CLIENT_REFUSED;
            case "DATA_NOT_COLLECTED" -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
    
    /**
     * Convert to database enum string
     */
    public String toDatabaseValue() {
        return switch (this) {
            case YES -> "YES";
            case NO -> "NO";
            case CLIENT_DOESNT_KNOW -> "CLIENT_DOESNT_KNOW";
            case CLIENT_REFUSED -> "CLIENT_REFUSED";
            case DATA_NOT_COLLECTED -> "DATA_NOT_COLLECTED";
        };
    }
    
    /**
     * Convert to CSV export value
     */
    public String toCsvValue() {
        return String.valueOf(hmisValue);
    }
}