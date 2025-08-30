package org.haven.shared.vo.hmis;

/**
 * HMIS Current Living Situation - FY2024 Data Standards
 * Aligns with HMIS Data Elements 4.12 Current Living Situation
 * Used for Current Living Situation assessments during enrollment
 */
public enum CurrentLivingSituation {
    
    // Homeless situations
    PLACE_NOT_MEANT_FOR_HABITATION(1, "Place not meant for habitation"),
    EMERGENCY_SHELTER(2, "Emergency shelter"),
    SAFE_HAVEN(3, "Safe Haven"),
    
    // Transitional and supportive housing
    TRANSITIONAL_HOUSING_FOR_HOMELESS(4, "Transitional housing for homeless persons"),
    HOST_HOME_FAMILY(5, "Host Home (family)"),
    HOST_HOME_NON_FAMILY(6, "Host Home (non-family)"),
    
    // Institutional settings
    HOSPITAL_OR_OTHER_RESIDENTIAL_NON_PSYCHIATRIC(7, "Hospital or other residential non-psychiatric medical facility"),
    JAIL_PRISON_JUVENILE_DETENTION(8, "Jail, prison or juvenile detention facility, or correctional facility"),
    SUBSTANCE_ABUSE_TREATMENT_FACILITY(9, "Substance abuse treatment facility or detox center"),
    PSYCHIATRIC_HOSPITAL_OR_OTHER_PSYCHIATRIC_FACILITY(10, "Psychiatric hospital or other psychiatric facility"),
    
    // Permanent housing
    PERMANENT_HOUSING_FOR_FORMERLY_HOMELESS(11, "Permanent housing for formerly homeless persons"),
    OWNED_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY(12, "Owned by client, no ongoing housing subsidy"),
    OWNED_BY_CLIENT_WITH_ONGOING_HOUSING_SUBSIDY(13, "Owned by client, with ongoing housing subsidy"),
    RENTAL_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY(14, "Rental by client, no ongoing housing subsidy"),
    RENTAL_BY_CLIENT_WITH_VASH_HOUSING_SUBSIDY(15, "Rental by client, with VASH housing subsidy"),
    RENTAL_BY_CLIENT_WITH_OTHER_ONGOING_HOUSING_SUBSIDY(16, "Rental by client, with other ongoing housing subsidy"),
    STAYING_OR_LIVING_WITH_FAMILY_TEMPORARY_TENURE(17, "Staying or living with family, temporary tenure"),
    STAYING_OR_LIVING_WITH_FRIENDS_TEMPORARY_TENURE(18, "Staying or living with friends, temporary tenure"),
    STAYING_OR_LIVING_WITH_FAMILY_PERMANENT_TENURE(19, "Staying or living with family, permanent tenure"),
    STAYING_OR_LIVING_WITH_FRIENDS_PERMANENT_TENURE(20, "Staying or living with friends, permanent tenure"),
    
    // Other situations
    FOSTER_CARE_HOME_OR_FOSTER_CARE_GROUP_HOME(21, "Foster care home or foster care group home"),
    OTHER(22, "Other"),
    
    // Standard responses
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    CurrentLivingSituation(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }
    
    public int getHmisValue() {
        return hmisValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isKnownSituation() {
        return this != CLIENT_DOESNT_KNOW && 
               this != CLIENT_REFUSED && 
               this != DATA_NOT_COLLECTED;
    }
    
    public boolean isLiterallyHomeless() {
        return this == PLACE_NOT_MEANT_FOR_HABITATION ||
               this == EMERGENCY_SHELTER ||
               this == SAFE_HAVEN;
    }
    
    public boolean isTemporarilyHomeless() {
        return this == TRANSITIONAL_HOUSING_FOR_HOMELESS ||
               this == HOST_HOME_FAMILY ||
               this == HOST_HOME_NON_FAMILY ||
               this == STAYING_OR_LIVING_WITH_FAMILY_TEMPORARY_TENURE ||
               this == STAYING_OR_LIVING_WITH_FRIENDS_TEMPORARY_TENURE;
    }
    
    public boolean isInstitutional() {
        return this == HOSPITAL_OR_OTHER_RESIDENTIAL_NON_PSYCHIATRIC ||
               this == JAIL_PRISON_JUVENILE_DETENTION ||
               this == SUBSTANCE_ABUSE_TREATMENT_FACILITY ||
               this == PSYCHIATRIC_HOSPITAL_OR_OTHER_PSYCHIATRIC_FACILITY;
    }
    
    public boolean isPermanentHousing() {
        return this == PERMANENT_HOUSING_FOR_FORMERLY_HOMELESS ||
               this == OWNED_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY ||
               this == OWNED_BY_CLIENT_WITH_ONGOING_HOUSING_SUBSIDY ||
               this == RENTAL_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY ||
               this == RENTAL_BY_CLIENT_WITH_VASH_HOUSING_SUBSIDY ||
               this == RENTAL_BY_CLIENT_WITH_OTHER_ONGOING_HOUSING_SUBSIDY ||
               this == STAYING_OR_LIVING_WITH_FAMILY_PERMANENT_TENURE ||
               this == STAYING_OR_LIVING_WITH_FRIENDS_PERMANENT_TENURE;
    }
    
    public boolean hasHousingSubsidy() {
        return this == OWNED_BY_CLIENT_WITH_ONGOING_HOUSING_SUBSIDY ||
               this == RENTAL_BY_CLIENT_WITH_VASH_HOUSING_SUBSIDY ||
               this == RENTAL_BY_CLIENT_WITH_OTHER_ONGOING_HOUSING_SUBSIDY;
    }
}