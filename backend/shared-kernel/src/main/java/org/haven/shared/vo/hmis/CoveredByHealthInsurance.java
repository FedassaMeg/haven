package org.haven.shared.vo.hmis;

/**
 * HMIS Covered by Health Insurance - FY2024 Data Standards
 * Five-point response scale for overall health insurance coverage status
 * Aligns with HMIS Data Elements 4.04.1 Covered by Health Insurance
 */
public enum CoveredByHealthInsurance {
    
    NO(0, "No"),
    YES(1, "Yes"), 
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    CoveredByHealthInsurance(int hmisValue, String description) {
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
     * Check if client has health insurance coverage
     */
    public boolean hasCoverage() {
        return this == YES;
    }
    
    /**
     * Check if client has no health insurance coverage
     */
    public boolean noCoverage() {
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
    public static CoveredByHealthInsurance fromHmisValue(Integer value) {
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
     * Check if response requires insurance source collection
     * Per HMIS standards, if YES then individual sources must be collected
     */
    public boolean requiresSourceCollection() {
        return this == YES;
    }
    
    /**
     * Check if response prohibits insurance source collection
     * Per HMIS standards, if NO then individual sources should not be collected
     */
    public boolean prohibitsSourceCollection() {
        return this == NO;
    }
    
    /**
     * Convert to "No Insurance" value for HMIS CSV export
     * Used in HealthAndDV.csv where NoInsurance field is inverse of coverage
     */
    public HmisFivePointResponse toNoInsuranceResponse() {
        return switch (this) {
            case NO -> HmisFivePointResponse.YES; // No coverage = Yes to "no insurance"
            case YES -> HmisFivePointResponse.NO; // Has coverage = No to "no insurance"
            case CLIENT_DOESNT_KNOW -> HmisFivePointResponse.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED -> HmisFivePointResponse.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED -> HmisFivePointResponse.DATA_NOT_COLLECTED;
        };
    }
}