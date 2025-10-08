package org.haven.reporting.application.transformers;

import org.haven.shared.vo.hmis.*;

/**
 * Utility class for converting Haven domain enums to HMIS CSV integer values
 * per FY2024 HMIS Data Standards specifications.
 *
 * All conversions follow HUD code lists:
 * - 1 = Yes
 * - 0 = No
 * - 8 = Client doesn't know
 * - 9 = Client refused
 * - 99 = Data not collected
 */
public final class HmisValueConverter {

    private HmisValueConverter() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts HmisFivePointResponse to HMIS Integer value
     * Used for: Income/Benefits responses, Health Insurance coverage
     *
     * @param response HmisFivePointResponse enum value
     * @return HMIS integer value (1, 0, 8, 9, 99)
     */
    public static Integer toInteger(HmisFivePointResponse response) {
        if (response == null) {
            return 99; // DATA_NOT_COLLECTED
        }

        return switch (response) {
            case YES -> 1;
            case NO -> 0;
            case CLIENT_DOESNT_KNOW -> 8;
            case CLIENT_REFUSED -> 9;
            case DATA_NOT_COLLECTED -> 99;
        };
    }

    /**
     * Converts HmisFivePoint to HMIS Integer value
     * Used for: Disability responses, DV responses
     *
     * @param response HmisFivePoint enum value
     * @return HMIS integer value (1, 0, 8, 9, 99)
     */
    public static Integer toInteger(HmisFivePoint response) {
        if (response == null) {
            return 99; // DATA_NOT_COLLECTED
        }

        return switch (response) {
            case YES -> 1;
            case NO -> 0;
            case CLIENT_DOESNT_KNOW -> 8;
            case CLIENT_REFUSED -> 9;
            case DATA_NOT_COLLECTED -> 99;
        };
    }

    /**
     * Converts DomesticViolence enum to HMIS Integer value
     * VAWA-sensitive field
     *
     * @param dv DomesticViolence enum value
     * @return HMIS integer value (1, 0, 8, 9, 99)
     */
    public static Integer toInteger(DomesticViolence dv) {
        if (dv == null) {
            return 99; // DATA_NOT_COLLECTED
        }

        return switch (dv) {
            case YES -> 1;
            case NO -> 0;
            case CLIENT_DOESNT_KNOW -> 8;
            case CLIENT_REFUSED -> 9;
            case DATA_NOT_COLLECTED -> 99;
        };
    }

    /**
     * Converts DisabilityType to HMIS Integer value
     * Uses existing getHmisValue() method on enum
     *
     * @param type DisabilityType enum value
     * @return HMIS integer value (1, 0, 8, 9, 99)
     */
    public static Integer toInteger(DisabilityType type) {
        if (type == null) {
            return 99; // DATA_NOT_COLLECTED
        }

        return type.getHmisValue();
    }

    /**
     * Converts InformationDate enum to DataCollectionStage Integer
     * per HMIS CSV specifications
     *
     * Mapping:
     * - START_OF_PROJECT → 1 (Project Start)
     * - UPDATE → 2 (Update)
     * - EXIT → 3 (Project Exit)
     * - MINOR_TURNING_18 → 4 (Annual Assessment - special case)
     * - ANNUAL_ASSESSMENT → 5 (Annual Assessment)
     *
     * @param informationDate InformationDate enum value
     * @return DataCollectionStage integer (1-5)
     */
    public static Integer toDataCollectionStage(org.haven.shared.vo.hmis.InformationDate informationDate) {
        if (informationDate == null) {
            return 1; // Default to project start
        }

        return switch (informationDate) {
            case START_OF_PROJECT -> 1;
            case UPDATE -> 2;
            case EXIT -> 3;
            case MINOR_TURNING_18 -> 4;
            case ANNUAL_ASSESSMENT -> 5;
        };
    }

    /**
     * Converts DataCollectionStage enum to Integer
     * per HMIS CSV specifications
     *
     * Mapping:
     * - PROJECT_START → 1
     * - UPDATE → 2
     * - PROJECT_EXIT → 3
     *
     * @param stage DataCollectionStage enum value
     * @return DataCollectionStage integer (1-3)
     */
    public static Integer toDataCollectionStage(org.haven.shared.vo.hmis.DataCollectionStage stage) {
        if (stage == null) {
            return 1; // Default to project start
        }

        return switch (stage) {
            case PROJECT_START -> 1;
            case UPDATE -> 2;
            case PROJECT_EXIT -> 3;
        };
    }

    /**
     * Converts HmisFivePointResponse to DisabilityType for benefits mapping
     * Used when mapping non-cash benefits from HmisFivePointResponse to DisabilityType
     *
     * @param response HmisFivePointResponse enum value
     * @return DisabilityType enum value
     */
    public static DisabilityType toDisabilityType(HmisFivePointResponse response) {
        if (response == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }

        return switch (response) {
            case YES -> DisabilityType.YES;
            case NO -> DisabilityType.NO;
            case CLIENT_DOESNT_KNOW -> DisabilityType.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED -> DisabilityType.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED -> DisabilityType.DATA_NOT_COLLECTED;
        };
    }

    /**
     * Inverts health insurance coverage response for NoInsurance field
     *
     * @param coveredByHealthInsurance HmisFivePointResponse value
     * @return Integer value inverted (YES→0, NO→1, etc.)
     */
    public static Integer toNoInsuranceValue(HmisFivePointResponse coveredByHealthInsurance) {
        if (coveredByHealthInsurance == null) {
            return 99;
        }

        return switch (coveredByHealthInsurance) {
            case YES -> 0; // Has insurance = NO to "no insurance"
            case NO -> 1;  // No insurance = YES to "no insurance"
            case CLIENT_DOESNT_KNOW -> 8;
            case CLIENT_REFUSED -> 9;
            case DATA_NOT_COLLECTED -> 99;
        };
    }

    /**
     * Converts boolean insurance type to Integer based on overall coverage status
     *
     * @param hasInsuranceType Whether client has this specific insurance type
     * @param overallCoverage Overall health insurance coverage status
     * @return HMIS integer value (1, 0, 8, 9, 99)
     */
    public static Integer toInsuranceTypeValue(boolean hasInsuranceType, HmisFivePointResponse overallCoverage) {
        if (overallCoverage == null) {
            return 99;
        }

        return switch (overallCoverage) {
            case YES -> hasInsuranceType ? 1 : 0;
            case NO -> 0;
            case CLIENT_DOESNT_KNOW -> 8;
            case CLIENT_REFUSED -> 9;
            case DATA_NOT_COLLECTED -> 99;
        };
    }
}
