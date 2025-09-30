package org.haven.shared.vo.hmis;

/**
 * HMIS RRH Move-In Date Classification - FY2024 Data Standards
 * Represents type of move-in date for RRH programs per HMIS Element 3.20
 */
public enum ResidentialMoveInDateType {

    INITIAL_MOVE_IN(1, "Initial move-in to permanent housing"),
    SUBSEQUENT_MOVE_IN(2, "Subsequent move to different permanent housing unit"),
    MOVE_IN_AFTER_TEMPORARY_ABSENCE(3, "Move-in after temporary absence"),
    DATA_NOT_COLLECTED(99, "Data not collected");

    private final int hmisValue;
    private final String description;

    ResidentialMoveInDateType(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }

    public int getHmisValue() {
        return hmisValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isKnownResponse() {
        return this != DATA_NOT_COLLECTED;
    }

    public boolean isInitialMoveIn() {
        return this == INITIAL_MOVE_IN;
    }

    public boolean isSubsequentMoveIn() {
        return this == SUBSEQUENT_MOVE_IN;
    }

    public static ResidentialMoveInDateType fromHmisValue(Integer value) {
        if (value == null) return DATA_NOT_COLLECTED;

        return switch (value) {
            case 1 -> INITIAL_MOVE_IN;
            case 2 -> SUBSEQUENT_MOVE_IN;
            case 3 -> MOVE_IN_AFTER_TEMPORARY_ABSENCE;
            case 99 -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
}