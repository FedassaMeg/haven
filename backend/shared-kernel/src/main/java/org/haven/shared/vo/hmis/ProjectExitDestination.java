package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.12 Destination
 * Describes where the client is expected to stay after exiting the project.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum ProjectExitDestination {
    // Permanent destinations
    RENTAL_BY_CLIENT_NO_SUBSIDY("Rental by client, no ongoing housing subsidy"),
    RENTAL_BY_CLIENT_WITH_VASH("Rental by client, with VASH housing subsidy"),
    RENTAL_BY_CLIENT_WITH_OTHER_SUBSIDY("Rental by client, with other ongoing housing subsidy"),
    OWNED_BY_CLIENT_NO_SUBSIDY("Owned by client, no ongoing housing subsidy"),
    OWNED_BY_CLIENT_WITH_SUBSIDY("Owned by client, with ongoing housing subsidy"),
    STAYING_WITH_FAMILY_PERMANENT("Staying or living with family, permanent tenure"),
    STAYING_WITH_FRIENDS_PERMANENT("Staying or living with friends, permanent tenure"),
    PERMANENT_HOUSING_FORMERLY_HOMELESS("Permanent housing (other than RRH) for formerly homeless persons"),
    RAPID_REHOUSING("Rapid re-housing or other similar time-limited subsidy"),
    LONG_TERM_CARE("Long-term care facility or nursing home"),
    MOVED_FROM_ONE_HOPWA_FUNDED_PROJECT("Moved from one HOPWA funded project to HOPWA PH"),
    MOVED_FROM_ONE_RRH_PROJECT("Moved from one RRH project to RRH PH"),
    
    // Temporary destinations
    EMERGENCY_SHELTER("Emergency shelter, including hotel or motel paid for with emergency shelter voucher, or RHY-funded Host Home shelter"),
    TRANSITIONAL_HOUSING("Transitional housing for homeless persons (including homeless youth)"),
    STAYING_WITH_FAMILY_TEMPORARY("Staying or living with family, temporary tenure (e.g., room, apartment or house)"),
    STAYING_WITH_FRIENDS_TEMPORARY("Staying or living with friends, temporary tenure (e.g., room, apartment or house)"),
    SAFE_HAVEN("Safe Haven"),
    HOTEL_MOTEL_NO_VOUCHER("Hotel or motel paid for without emergency shelter voucher"),
    TRANSITIONAL_HOUSING_FOR_YOUTH("Transitional housing for homeless youth"),
    HOST_HOME_NON_CRISIS("Host Home (non-crisis)"),
    
    // Institutional settings
    PSYCHIATRIC_HOSPITAL("Psychiatric hospital or other psychiatric facility"),
    SUBSTANCE_ABUSE_TREATMENT("Substance abuse treatment facility or detox center"),
    HOSPITAL("Hospital or other residential non-psychiatric medical facility"),
    JAIL_PRISON("Jail, prison or juvenile detention facility"),
    CONNECTIVE_CARE("Connective Care"),
    FOSTER_CARE_HOME("Foster care home or foster care group home"),
    
    // Streets/unsheltered
    PLACE_NOT_MEANT_FOR_HABITATION("Place not meant for habitation (e.g., a vehicle, an abandoned building, bus/train/subway station/airport or anywhere outside)"),
    
    // Other
    RESIDENTIAL_PROJECT_HALFWAY_HOUSE("Residential project or halfway house with no homeless criteria"),
    OTHER("Other"),
    DECEASED("Deceased"),
    
    // Data quality
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    ProjectExitDestination(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Permanent housing destinations
     */
    public boolean isPermanentDestination() {
        return this == RENTAL_BY_CLIENT_NO_SUBSIDY ||
               this == RENTAL_BY_CLIENT_WITH_VASH ||
               this == RENTAL_BY_CLIENT_WITH_OTHER_SUBSIDY ||
               this == OWNED_BY_CLIENT_NO_SUBSIDY ||
               this == OWNED_BY_CLIENT_WITH_SUBSIDY ||
               this == STAYING_WITH_FAMILY_PERMANENT ||
               this == STAYING_WITH_FRIENDS_PERMANENT ||
               this == PERMANENT_HOUSING_FORMERLY_HOMELESS ||
               this == RAPID_REHOUSING ||
               this == LONG_TERM_CARE ||
               this == MOVED_FROM_ONE_HOPWA_FUNDED_PROJECT ||
               this == MOVED_FROM_ONE_RRH_PROJECT;
    }

    /**
     * Temporary housing destinations
     */
    public boolean isTemporaryDestination() {
        return this == EMERGENCY_SHELTER ||
               this == TRANSITIONAL_HOUSING ||
               this == STAYING_WITH_FAMILY_TEMPORARY ||
               this == STAYING_WITH_FRIENDS_TEMPORARY ||
               this == SAFE_HAVEN ||
               this == HOTEL_MOTEL_NO_VOUCHER ||
               this == TRANSITIONAL_HOUSING_FOR_YOUTH ||
               this == HOST_HOME_NON_CRISIS;
    }

    /**
     * Institutional destinations
     */
    public boolean isInstitutionalDestination() {
        return this == PSYCHIATRIC_HOSPITAL ||
               this == SUBSTANCE_ABUSE_TREATMENT ||
               this == HOSPITAL ||
               this == JAIL_PRISON ||
               this == CONNECTIVE_CARE ||
               this == FOSTER_CARE_HOME;
    }

    /**
     * Returns to homelessness
     */
    public boolean isReturnToHomelessness() {
        return this == PLACE_NOT_MEANT_FOR_HABITATION ||
               this == EMERGENCY_SHELTER ||
               this == SAFE_HAVEN ||
               this == TRANSITIONAL_HOUSING;
    }

    /**
     * Positive housing outcomes for reporting
     */
    public boolean isPositiveHousingOutcome() {
        return isPermanentDestination() && 
               this != LONG_TERM_CARE; // Excluding institutional permanent housing
    }
}