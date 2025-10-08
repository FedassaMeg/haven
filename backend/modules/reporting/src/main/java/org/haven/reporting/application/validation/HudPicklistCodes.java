package org.haven.reporting.application.validation;

import java.util.Set;

/**
 * HUD 2024 HMIS Data Standards picklist code definitions.
 *
 * Provides comprehensive code validation for all HUD picklists
 * referenced in CSV exports. Codes are organized by HUD specification
 * section numbers (e.g., "1.7 Disabling Condition").
 *
 * Source: HUD HMIS Data Standards 2024 v1.0 (effective October 1, 2024)
 */
public final class HudPicklistCodes {

    private HudPicklistCodes() {
        // Utility class
    }

    // 1.4 Name Data Quality
    public static final Set<Integer> NAME_DATA_QUALITY = Set.of(
            1,  // Full name reported
            2,  // Partial, street name, or code name reported
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 1.5 SSN Data Quality
    public static final Set<Integer> SSN_DATA_QUALITY = Set.of(
            1,  // Full SSN reported
            2,  // Approximate or partial SSN reported
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 1.6 DOB Data Quality
    public static final Set<Integer> DOB_DATA_QUALITY = Set.of(
            1,  // Full DOB reported
            2,  // Approximate or partial DOB reported
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 1.7 Disabling Condition
    public static final Set<Integer> DISABLING_CONDITION = Set.of(
            0,  // No
            1,  // Yes
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 1.8 Race (0/1 values for each race category)
    public static final Set<Integer> RACE_CATEGORY = Set.of(0, 1);

    // 1.9 Ethnicity
    public static final Set<Integer> ETHNICITY = Set.of(
            0,  // Non-Hispanic/Non-Latin(a)(o)(x)
            1,  // Hispanic/Latin(a)(o)(x)
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 1.10 Gender (0/1 values for each gender category)
    public static final Set<Integer> GENDER_CATEGORY = Set.of(0, 1);

    // 1.27 Relationship to Head of Household
    public static final Set<Integer> RELATIONSHIP_TO_HOH = Set.of(
            1,  // Self (head of household)
            2,  // Head of household's child
            3,  // Head of household's spouse or partner
            4,  // Head of household's other relation member
            5   // Other: non-relation member
    );

    // 3.12 Destination
    public static final Set<Integer> DESTINATION = Set.of(
            // Emergency shelter / Temporary housing
            1,   // Emergency shelter, including hotel or motel paid by organization
            2,   // Transitional housing
            3,   // Permanent housing for formerly homeless persons
            4,   // Psychiatric hospital or other psychiatric facility
            5,   // Substance abuse treatment facility or detox center
            6,   // Hospital or other residential non-psychiatric medical facility
            7,   // Jail, prison or juvenile detention facility
            8,   // Client doesn't know
            9,   // Client refused
            10,  // Rental by client, no ongoing housing subsidy
            11,  // Owned by client, no ongoing housing subsidy
            12,  // Staying or living with family, temporary tenure
            13,  // Staying or living with friends, temporary tenure
            14,  // Hotel or motel paid for without emergency shelter voucher
            15,  // Foster care home or foster care group home
            16,  // Place not meant for habitation
            17,  // Other
            18,  // Safe Haven
            19,  // Rental by client, with VASH housing subsidy
            20,  // Rental by client, with other ongoing housing subsidy
            21,  // Owned by client, with ongoing housing subsidy
            22,  // Staying or living with family, permanent tenure
            23,  // Staying or living with friends, permanent tenure
            24,  // Deceased
            25,  // Long-term care facility or nursing home
            26,  // Moved from one HOPWA funded project to HOPWA PH
            27,  // Moved from one HOPWA funded project to HOPWA TH
            28,  // Rental by client, with GPD TIP housing subsidy
            29,  // Residential project or halfway house with no homeless criteria
            30,  // No exit interview completed
            31,  // Rental by client, with RRH or equivalent subsidy
            32,  // Host Home (non-crisis)
            33,  // Host Home (crisis)
            34,  // Rental by client, with HCV voucher
            35,  // Rental by client, in a public housing unit
            36,  // Rental by client, with other ongoing housing subsidy
            99,  // Data not collected
            101, // Emergency shelter
            116, // Place not meant for habitation
            118, // Safe haven
            204, // Psychiatric facility
            205, // Substance abuse treatment facility
            206, // Hospital (non-psychiatric)
            207, // Jail/prison/juvenile detention
            215, // Foster care home
            225, // Long-term care facility
            302, // Transitional housing for homeless persons
            312, // Staying or living with family, temporary tenure
            313, // Staying or living with friends, temporary tenure
            314, // Hotel or motel paid for without emergency shelter voucher
            327, // Moved from one HOPWA funded project to HOPWA TH
            329, // Residential project or halfway house
            332, // Host home (non-crisis)
            410, // Rental by client, no housing subsidy
            411, // Owned by client, no housing subsidy
            421, // Rental by client, with other ongoing housing subsidy
            422, // Owned by client, with ongoing housing subsidy
            423, // Staying or living with family, permanent tenure
            426, // Moved from one HOPWA funded project to HOPWA PH
            435, // Rental by client, with VASH subsidy
            436  // Deceased
    );

    // 3.15 Project Type (for validation context)
    public static final Set<Integer> PROJECT_TYPE = Set.of(
            0,   // Emergency Shelter - Entry/Exit
            1,   // Emergency Shelter - Night-by-Night
            2,   // Transitional Housing
            3,   // PH - Permanent Supportive Housing
            4,   // Street Outreach
            6,   // Services Only
            7,   // Other
            8,   // Safe Haven
            9,   // PH - Housing Only
            10,  // PH - Housing with Services
            11,  // Day Shelter
            12,  // Homelessness Prevention
            13,  // PH - Rapid Re-Housing
            14,  // Coordinated Entry
            15   // PH - Co-occurring
    );

    // 3.917 Living Situation
    public static final Set<Integer> LIVING_SITUATION = Set.of(
            1,   // Emergency shelter
            2,   // Transitional housing for homeless persons
            3,   // Permanent housing for formerly homeless persons
            4,   // Psychiatric facility
            5,   // Substance abuse treatment facility
            6,   // Hospital (non-psychiatric)
            7,   // Jail/prison/juvenile detention
            8,   // Client doesn't know
            9,   // Client refused
            10,  // Rental by client, no housing subsidy
            11,  // Owned by client, no housing subsidy
            12,  // Staying or living with family, temporary tenure
            13,  // Staying or living with friends, temporary tenure
            14,  // Hotel or motel paid for without emergency shelter voucher
            15,  // Foster care home
            16,  // Place not meant for habitation
            17,  // Other
            18,  // Safe haven
            19,  // Rental by client, with VASH housing subsidy
            20,  // Rental by client, with other ongoing housing subsidy
            21,  // Owned by client, with ongoing housing subsidy
            22,  // Staying or living with family, permanent tenure
            23,  // Staying or living with friends, permanent tenure
            24,  // Residential project or halfway house
            25,  // Long-term care facility
            26,  // Moved from one HOPWA funded project to HOPWA PH
            27,  // Moved from one HOPWA funded project to HOPWA TH
            28,  // Rental by client, with GPD TIP housing subsidy
            29,  // Residential project with no homeless criteria
            30,  // Host Home (non-crisis)
            31,  // Host Home (crisis)
            32,  // Rental by client, with RRH or equivalent subsidy
            33,  // Rental by client, with HCV voucher
            34,  // Rental by client, in a public housing unit
            35,  // Rental by client, other ongoing housing subsidy
            36,  // Deceased
            37,  // Other location
            99   // Data not collected
    );

    // 4.02 Income and Sources - Five Point Response
    public static final Set<Integer> FIVE_POINT_RESPONSE = Set.of(
            0,  // No
            1,  // Yes
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 4.03 Non-Cash Benefits - Five Point Response (same as above)
    // 4.04 Health Insurance - Five Point Response (same as above)

    // 4.05 Data Collection Stage
    public static final Set<Integer> DATA_COLLECTION_STAGE = Set.of(
            1,  // Project start
            2,  // Update
            3,  // Project exit
            5   // Annual assessment
    );

    // 4.10 Domestic Violence
    public static final Set<Integer> DOMESTIC_VIOLENCE = Set.of(
            0,  // No
            1,  // Yes
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 4.10.2 When DV Occurred
    public static final Set<Integer> WHEN_DV_OCCURRED = Set.of(
            1,  // Within the past three months
            2,  // Three to six months ago
            3,  // Six months to one year ago
            4,  // One year ago or more
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 4.11 Health Status
    public static final Set<Integer> HEALTH_STATUS = Set.of(
            1,  // Excellent
            2,  // Very good
            3,  // Good
            4,  // Fair
            5,  // Poor
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // 4.12 Disabilities
    public static final Set<Integer> DISABILITY_TYPE = Set.of(
            5,  // Physical disability
            6,  // Developmental disability
            7,  // Chronic health condition
            8,  // HIV/AIDS
            9,  // Mental health disorder
            10  // Substance use disorder
    );

    public static final Set<Integer> DISABILITY_RESPONSE = Set.of(
            0,  // No
            1,  // Yes
            8,  // Client doesn't know
            9,  // Client refused
            99  // Data not collected
    );

    // Record Type for Services
    public static final Set<Integer> RECORD_TYPE = Set.of(
            12,  // Contact
            141, // PATH service
            142, // RHY service
            143, // HOPWA service
            144, // SSVF service
            151, // HOPWA financial assistance
            152, // SSVF financial assistance
            161, // Path referral
            200  // Bed night
    );

    /**
     * Validates if a code is valid for a specific picklist.
     *
     * @param picklistName Name of the picklist (e.g., "RELATIONSHIP_TO_HOH")
     * @param code Code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(String picklistName, Integer code) {
        if (code == null) {
            return false;
        }

        return switch (picklistName) {
            case "NAME_DATA_QUALITY" -> NAME_DATA_QUALITY.contains(code);
            case "SSN_DATA_QUALITY" -> SSN_DATA_QUALITY.contains(code);
            case "DOB_DATA_QUALITY" -> DOB_DATA_QUALITY.contains(code);
            case "DISABLING_CONDITION" -> DISABLING_CONDITION.contains(code);
            case "RACE_CATEGORY" -> RACE_CATEGORY.contains(code);
            case "ETHNICITY" -> ETHNICITY.contains(code);
            case "GENDER_CATEGORY" -> GENDER_CATEGORY.contains(code);
            case "RELATIONSHIP_TO_HOH" -> RELATIONSHIP_TO_HOH.contains(code);
            case "DESTINATION" -> DESTINATION.contains(code);
            case "PROJECT_TYPE" -> PROJECT_TYPE.contains(code);
            case "LIVING_SITUATION" -> LIVING_SITUATION.contains(code);
            case "FIVE_POINT_RESPONSE" -> FIVE_POINT_RESPONSE.contains(code);
            case "DATA_COLLECTION_STAGE" -> DATA_COLLECTION_STAGE.contains(code);
            case "DOMESTIC_VIOLENCE" -> DOMESTIC_VIOLENCE.contains(code);
            case "WHEN_DV_OCCURRED" -> WHEN_DV_OCCURRED.contains(code);
            case "HEALTH_STATUS" -> HEALTH_STATUS.contains(code);
            case "DISABILITY_TYPE" -> DISABILITY_TYPE.contains(code);
            case "DISABILITY_RESPONSE" -> DISABILITY_RESPONSE.contains(code);
            case "RECORD_TYPE" -> RECORD_TYPE.contains(code);
            default -> false;
        };
    }

    /**
     * Gets the valid codes for a specific picklist.
     *
     * @param picklistName Name of the picklist
     * @return Set of valid codes, or empty set if picklist not found
     */
    public static Set<Integer> getValidCodes(String picklistName) {
        return switch (picklistName) {
            case "NAME_DATA_QUALITY" -> NAME_DATA_QUALITY;
            case "SSN_DATA_QUALITY" -> SSN_DATA_QUALITY;
            case "DOB_DATA_QUALITY" -> DOB_DATA_QUALITY;
            case "DISABLING_CONDITION" -> DISABLING_CONDITION;
            case "RACE_CATEGORY" -> RACE_CATEGORY;
            case "ETHNICITY" -> ETHNICITY;
            case "GENDER_CATEGORY" -> GENDER_CATEGORY;
            case "RELATIONSHIP_TO_HOH" -> RELATIONSHIP_TO_HOH;
            case "DESTINATION" -> DESTINATION;
            case "PROJECT_TYPE" -> PROJECT_TYPE;
            case "LIVING_SITUATION" -> LIVING_SITUATION;
            case "FIVE_POINT_RESPONSE" -> FIVE_POINT_RESPONSE;
            case "DATA_COLLECTION_STAGE" -> DATA_COLLECTION_STAGE;
            case "DOMESTIC_VIOLENCE" -> DOMESTIC_VIOLENCE;
            case "WHEN_DV_OCCURRED" -> WHEN_DV_OCCURRED;
            case "HEALTH_STATUS" -> HEALTH_STATUS;
            case "DISABILITY_TYPE" -> DISABILITY_TYPE;
            case "DISABILITY_RESPONSE" -> DISABILITY_RESPONSE;
            case "RECORD_TYPE" -> RECORD_TYPE;
            default -> Set.of();
        };
    }
}
