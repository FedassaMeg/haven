package org.haven.shared.vo.hmis;

/**
 * HMIS Length of Stay in Previous Living Situation
 * Used with Prior Living Situation to help determine chronic homelessness.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum LengthOfStay {
    ONE_NIGHT_OR_LESS("One night or less"),
    TWO_TO_SIX_NIGHTS("Two to six nights"),
    ONE_WEEK_TO_LESS_THAN_ONE_MONTH("One week or more, but less than one month"),
    ONE_MONTH_TO_LESS_THAN_THREE_MONTHS("One month or more, but less than three months"),
    THREE_MONTHS_TO_LESS_THAN_ONE_YEAR("Three months or more, but less than one year"),
    ONE_YEAR_OR_LONGER("One year or longer"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    LengthOfStay(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns true if the length of stay indicates long-term homelessness
     * (used in chronic homelessness determination)
     */
    public boolean isLongTerm() {
        return this == ONE_YEAR_OR_LONGER;
    }

    /**
     * Returns true if the length of stay is substantial (3+ months)
     * which may be relevant for certain eligibility criteria
     */
    public boolean isSubstantial() {
        return this == THREE_MONTHS_TO_LESS_THAN_ONE_YEAR ||
               this == ONE_YEAR_OR_LONGER;
    }

    /**
     * Returns true if this is a known, reportable value
     */
    public boolean isKnownLength() {
        return this != CLIENT_DOESNT_KNOW &&
               this != CLIENT_PREFERS_NOT_TO_ANSWER &&
               this != DATA_NOT_COLLECTED;
    }

    /**
     * Get the minimum number of days represented by this length of stay
     * Used for calculations and reporting
     */
    public Integer getMinimumDays() {
        return switch (this) {
            case ONE_NIGHT_OR_LESS -> 0;
            case TWO_TO_SIX_NIGHTS -> 2;
            case ONE_WEEK_TO_LESS_THAN_ONE_MONTH -> 7;
            case ONE_MONTH_TO_LESS_THAN_THREE_MONTHS -> 30;
            case THREE_MONTHS_TO_LESS_THAN_ONE_YEAR -> 90;
            case ONE_YEAR_OR_LONGER -> 365;
            default -> null; // Unknown values
        };
    }

    /**
     * Get the maximum number of days represented by this length of stay
     * Used for calculations and reporting
     */
    public Integer getMaximumDays() {
        return switch (this) {
            case ONE_NIGHT_OR_LESS -> 1;
            case TWO_TO_SIX_NIGHTS -> 6;
            case ONE_WEEK_TO_LESS_THAN_ONE_MONTH -> 29;
            case ONE_MONTH_TO_LESS_THAN_THREE_MONTHS -> 89;
            case THREE_MONTHS_TO_LESS_THAN_ONE_YEAR -> 364;
            case ONE_YEAR_OR_LONGER -> null; // Open-ended
            default -> null; // Unknown values
        };
    }
}