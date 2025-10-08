package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.05 Ethnicity
 * Represents the client's self-identification with ethnic categories.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum HmisEthnicity {
    NON_HISPANIC_LATINO("Non-Hispanic/Non-Latino"),
    HISPANIC_LATINO("Hispanic/Latino"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    HmisEthnicity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this is a known ethnicity value
     */
    public boolean isKnownEthnicity() {
        return this != CLIENT_DOESNT_KNOW && 
               this != CLIENT_PREFERS_NOT_TO_ANSWER && 
               this != DATA_NOT_COLLECTED;
    }
    
    /**
     * Returns a privacy-safe representation based on precision level
     */
    public String getRedactedValue(EthnicityPrecision precision) {
        return switch (precision) {
            case FULL -> this.description;
            case CATEGORY_ONLY -> this.isKnownEthnicity() ? "Known" : "Unknown/Not Collected";
            case REDACTED -> "[REDACTED]";
            case HIDDEN -> null;
        };
    }
    
    /**
     * Precision levels for ethnicity data
     */
    public enum EthnicityPrecision {
        FULL,           // Complete ethnicity information
        CATEGORY_ONLY,  // Only whether ethnicity is known
        REDACTED,       // Shows field exists but value is redacted
        HIDDEN          // Field is completely hidden
    }
}