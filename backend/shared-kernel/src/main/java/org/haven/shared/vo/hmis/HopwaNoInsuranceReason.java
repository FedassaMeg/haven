package org.haven.shared.vo.hmis;

/**
 * HMIS HOPWA No Insurance Reason - FY2024 Data Standards
 * Required for HOPWA (Housing Opportunities for Persons With AIDS) programs
 * when client has no health insurance coverage
 * Aligns with HMIS Data Elements 4.04.B HOPWA Services - Reason for No Insurance
 */
public enum HopwaNoInsuranceReason {
    
    APPLIED_PENDING(1, "Applied, pending approval"),
    APPLIED_NOT_ELIGIBLE(2, "Applied, client not eligible"),
    CLIENT_DID_NOT_APPLY(3, "Client did not apply"),
    TYPE_NA(4, "Type not applicable for this client (e.g., other insurance type)"),
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    HopwaNoInsuranceReason(int hmisValue, String description) {
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
     * Check if this indicates an application was submitted
     */
    public boolean isApplicationSubmitted() {
        return this == APPLIED_PENDING || this == APPLIED_NOT_ELIGIBLE;
    }
    
    /**
     * Check if this indicates client action was taken
     */
    public boolean isClientAction() {
        return this == APPLIED_PENDING || 
               this == APPLIED_NOT_ELIGIBLE || 
               this == CLIENT_DID_NOT_APPLY;
    }
    
    /**
     * Check if this indicates system/program limitation
     */
    public boolean isSystemLimitation() {
        return this == TYPE_NA;
    }
    
    /**
     * Map from integer HMIS value to enum
     */
    public static HopwaNoInsuranceReason fromHmisValue(Integer value) {
        if (value == null) return DATA_NOT_COLLECTED;
        
        return switch (value) {
            case 1 -> APPLIED_PENDING;
            case 2 -> APPLIED_NOT_ELIGIBLE;
            case 3 -> CLIENT_DID_NOT_APPLY;
            case 4 -> TYPE_NA;
            case 8 -> CLIENT_DOESNT_KNOW;
            case 9 -> CLIENT_REFUSED;
            case 99 -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
    
    /**
     * Map from database enum string to domain enum
     */
    public static HopwaNoInsuranceReason fromDatabaseValue(String dbValue) {
        if (dbValue == null) return DATA_NOT_COLLECTED;
        
        return switch (dbValue.toLowerCase()) {
            case "applied_pending" -> APPLIED_PENDING;
            case "applied_not_eligible" -> APPLIED_NOT_ELIGIBLE;
            case "client_did_not_apply" -> CLIENT_DID_NOT_APPLY;
            case "type_na" -> TYPE_NA;
            case "client_doesnt_know" -> CLIENT_DOESNT_KNOW;
            case "client_refused" -> CLIENT_REFUSED;
            case "data_not_collected" -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
    
    /**
     * Convert to database enum string
     */
    public String toDatabaseValue() {
        return switch (this) {
            case APPLIED_PENDING -> "APPLIED_PENDING";
            case APPLIED_NOT_ELIGIBLE -> "APPLIED_NOT_ELIGIBLE";
            case CLIENT_DID_NOT_APPLY -> "CLIENT_DID_NOT_APPLY";
            case TYPE_NA -> "TYPE_NA";
            case CLIENT_DOESNT_KNOW -> "CLIENT_DOESNT_KNOW";
            case CLIENT_REFUSED -> "CLIENT_REFUSED";
            case DATA_NOT_COLLECTED -> "DATA_NOT_COLLECTED";
        };
    }
}