package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.04 Race
 * Represents the client's self-identification with racial categories.
 * Aligned with HMIS 2024 Data Standards and OMB 2024 standards.
 */
public enum HmisRace {
    AMERICAN_INDIAN_ALASKA_NATIVE("American Indian, Alaska Native, or Indigenous"),
    ASIAN("Asian or Asian American"),
    BLACK_AFRICAN_AMERICAN("Black, African American, or African"),
    HISPANIC_LATINO("Hispanic or Latino"),
    MIDDLE_EASTERN_NORTH_AFRICAN("Middle Eastern or North African"),
    NATIVE_HAWAIIAN_PACIFIC_ISLANDER("Native Hawaiian or Pacific Islander"),
    WHITE("White"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    HmisRace(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * HMIS allows multiple race selections, so this should be used with a Set or List
     * when storing client race information.
     */
    public boolean isKnownRace() {
        return this != CLIENT_DOESNT_KNOW && 
               this != CLIENT_PREFERS_NOT_TO_ANSWER && 
               this != DATA_NOT_COLLECTED;
    }
}