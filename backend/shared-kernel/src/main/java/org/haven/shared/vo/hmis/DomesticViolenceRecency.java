package org.haven.shared.vo.hmis;

/**
 * HMIS Domestic Violence Recency - FY2024 Data Standards
 * Represents when domestic violence was experienced
 * Aligns with HMIS Data Element 4.11.3
 */
public enum DomesticViolenceRecency {
    
    WITHIN_3_MONTHS(1, "Within the past 3 months"),
    THREE_TO_SIX_MONTHS(2, "3 to 6 months ago"),
    SIX_TO_12_MONTHS(3, "6 to 12 months ago"),
    MORE_THAN_12_MONTHS(4, "More than 12 months ago"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    DomesticViolenceRecency(int hmisValue, String description) {
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
        return this != CLIENT_DOESNT_KNOW && 
               this != CLIENT_REFUSED && 
               this != DATA_NOT_COLLECTED;
    }
    
    /**
     * Check if DV was experienced recently (within 6 months)
     */
    public boolean isRecent() {
        return this == WITHIN_3_MONTHS || this == THREE_TO_SIX_MONTHS;
    }
    
    /**
     * Check if DV was experienced very recently (within 3 months)
     */
    public boolean isVeryRecent() {
        return this == WITHIN_3_MONTHS;
    }
    
    /**
     * Check if DV was experienced in past year
     */
    public boolean isWithinPastYear() {
        return this == WITHIN_3_MONTHS || 
               this == THREE_TO_SIX_MONTHS || 
               this == SIX_TO_12_MONTHS;
    }
    
    /**
     * Map from integer HMIS value to enum
     */
    public static DomesticViolenceRecency fromHmisValue(Integer value) {
        if (value == null) return DATA_NOT_COLLECTED;
        
        return switch (value) {
            case 1 -> WITHIN_3_MONTHS;
            case 2 -> THREE_TO_SIX_MONTHS;
            case 3 -> SIX_TO_12_MONTHS;
            case 4 -> MORE_THAN_12_MONTHS;
            case 8 -> CLIENT_DOESNT_KNOW;
            case 9 -> CLIENT_REFUSED;
            case 99 -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
    
    /**
     * Map from database enum string to domain enum
     */
    public static DomesticViolenceRecency fromDatabaseValue(String dbValue) {
        if (dbValue == null) return DATA_NOT_COLLECTED;
        
        return switch (dbValue.toUpperCase()) {
            case "WITHIN_3_MONTHS" -> WITHIN_3_MONTHS;
            case "THREE_TO_SIX_MONTHS" -> THREE_TO_SIX_MONTHS;
            case "SIX_TO_12_MONTHS" -> SIX_TO_12_MONTHS;
            case "MORE_THAN_12_MONTHS" -> MORE_THAN_12_MONTHS;
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
            case WITHIN_3_MONTHS -> "WITHIN_3_MONTHS";
            case THREE_TO_SIX_MONTHS -> "THREE_TO_SIX_MONTHS";
            case SIX_TO_12_MONTHS -> "SIX_TO_12_MONTHS";
            case MORE_THAN_12_MONTHS -> "MORE_THAN_12_MONTHS";
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