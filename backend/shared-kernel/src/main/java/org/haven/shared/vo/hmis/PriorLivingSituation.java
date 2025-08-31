package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.917 Prior Living Situation
 * Identifies the type of living situation immediately prior to project start.
 * Used to determine chronic homelessness criteria.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum PriorLivingSituation {
    // Homeless situations
    EMERGENCY_SHELTER("Emergency shelter, including hotel or motel paid for with emergency shelter voucher, or RHY-funded Host Home shelter"),
    SAFE_HAVEN("Safe Haven"),
    TRANSITIONAL_HOUSING("Transitional housing for homeless persons (including homeless youth)"),
    PLACE_NOT_MEANT_FOR_HABITATION("Place not meant for habitation (e.g., a vehicle, an abandoned building, bus/train/subway station/airport or anywhere outside)"),
    
    // Institutional situations
    PSYCHIATRIC_HOSPITAL("Psychiatric hospital or other psychiatric facility"),
    SUBSTANCE_ABUSE_TREATMENT("Substance abuse treatment facility or detox center"),
    HOSPITAL("Hospital or other residential non-psychiatric medical facility"),
    JAIL_PRISON("Jail, prison or juvenile detention facility"),
    FOSTER_CARE_HOME("Foster care home or foster care group home"),
    LONG_TERM_CARE("Long-term care facility or nursing home"),
    
    // Temporary and permanent housing situations
    DOUBLED_UP("Staying or living with family, temporary tenure (e.g., room, apartment or house)"),
    DOUBLED_UP_FRIENDS("Staying or living with friends, temporary tenure (e.g., room, apartment or house)"),
    HOTEL_MOTEL_NO_VOUCHER("Hotel or motel paid for without emergency shelter voucher"),
    
    // Permanent housing situations
    RENTAL_HOUSING("Rental by client, no ongoing housing subsidy"),
    RENTAL_WITH_SUBSIDY("Rental by client, with VASH housing subsidy"),
    RENTAL_WITH_OTHER_SUBSIDY("Rental by client, with other ongoing housing subsidy"),
    OWNED_BY_CLIENT("Owned by client, no ongoing housing subsidy"),
    OWNED_WITH_SUBSIDY("Owned by client, with ongoing housing subsidy"),
    PERMANENT_HOUSING("Permanent housing (other than RRH) for formerly homeless persons"),
    RAPID_REHOUSING("Rapid re-housing or other similar time-limited subsidy"),
    
    // Other
    RESIDENTIAL_PROJECT("Residential project or halfway house with no homeless criteria"),
    OTHER("Other"),
    
    // Data quality
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    PriorLivingSituation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Situations that count as literally homeless according to HUD definition
     */
    public boolean isLiterallyHomeless() {
        return this == EMERGENCY_SHELTER ||
               this == SAFE_HAVEN ||
               this == TRANSITIONAL_HOUSING ||
               this == PLACE_NOT_MEANT_FOR_HABITATION;
    }

    /**
     * Institutional settings
     */
    public boolean isInstitutional() {
        return this == PSYCHIATRIC_HOSPITAL ||
               this == SUBSTANCE_ABUSE_TREATMENT ||
               this == HOSPITAL ||
               this == JAIL_PRISON ||
               this == FOSTER_CARE_HOME ||
               this == LONG_TERM_CARE;
    }

    /**
     * Temporary housing situations
     */
    public boolean isTemporaryHousing() {
        return this == DOUBLED_UP ||
               this == DOUBLED_UP_FRIENDS ||
               this == HOTEL_MOTEL_NO_VOUCHER;
    }

    /**
     * Permanent housing situations
     */
    public boolean isPermanentHousing() {
        return this == RENTAL_HOUSING ||
               this == RENTAL_WITH_SUBSIDY ||
               this == RENTAL_WITH_OTHER_SUBSIDY ||
               this == OWNED_BY_CLIENT ||
               this == OWNED_WITH_SUBSIDY ||
               this == PERMANENT_HOUSING ||
               this == RAPID_REHOUSING;
    }
    
    /**
     * Situations that indicate risk of homelessness
     */
    public boolean isAtRiskOfHomelessness() {
        return this == DOUBLED_UP ||
               this == DOUBLED_UP_FRIENDS ||
               this == HOTEL_MOTEL_NO_VOUCHER;
    }
    
    /**
     * Situations that indicate stable housing
     */
    public boolean isStableHousing() {
        return isPermanentHousing();
    }
}