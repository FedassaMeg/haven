package org.haven.shared.vo.hmis;

/**
 * HMIS Intake-Specific Data Collection Stages - FY2024 Data Standards
 * Represents enhanced data collection stages for intake and assessment workflows
 */
public enum IntakeDataCollectionStage {

    INITIAL_INTAKE(1, "Initial intake assessment"),
    COMPREHENSIVE_ASSESSMENT(2, "Comprehensive assessment"),
    PSDE_COLLECTION(3, "Program-specific data elements collection"),
    FOLLOW_UP_ASSESSMENT(4, "Follow-up assessment"),
    UPDATE_DUE_TO_CHANGE(5, "Update due to change in circumstances"),
    ANNUAL_ASSESSMENT(6, "Annual assessment"),
    EXIT_ASSESSMENT(7, "Exit assessment"),
    CORRECTION(8, "Data correction"),
    DATA_NOT_COLLECTED(99, "Data not collected");

    private final int hmisValue;
    private final String description;

    IntakeDataCollectionStage(int hmisValue, String description) {
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

    public boolean isIntakeStage() {
        return this == INITIAL_INTAKE || this == COMPREHENSIVE_ASSESSMENT;
    }

    public boolean isPsdeCollectionStage() {
        return this == PSDE_COLLECTION;
    }

    public boolean isAssessmentStage() {
        return this == COMPREHENSIVE_ASSESSMENT ||
               this == FOLLOW_UP_ASSESSMENT ||
               this == ANNUAL_ASSESSMENT;
    }

    public boolean requiresPsdeData() {
        return this == COMPREHENSIVE_ASSESSMENT ||
               this == PSDE_COLLECTION ||
               this == ANNUAL_ASSESSMENT;
    }

    public static IntakeDataCollectionStage fromHmisValue(Integer value) {
        if (value == null) return DATA_NOT_COLLECTED;

        return switch (value) {
            case 1 -> INITIAL_INTAKE;
            case 2 -> COMPREHENSIVE_ASSESSMENT;
            case 3 -> PSDE_COLLECTION;
            case 4 -> FOLLOW_UP_ASSESSMENT;
            case 5 -> UPDATE_DUE_TO_CHANGE;
            case 6 -> ANNUAL_ASSESSMENT;
            case 7 -> EXIT_ASSESSMENT;
            case 8 -> CORRECTION;
            case 99 -> DATA_NOT_COLLECTED;
            default -> DATA_NOT_COLLECTED;
        };
    }
}