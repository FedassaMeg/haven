package org.haven.programenrollment.domain.ce;

/**
 * HUD FY2024 Coordinated Entry assessment type codes.
 */
public enum CeAssessmentType {
    CRISIS_NEEDS,
    HOUSING_NEEDS,
    PREVENTION,
    DIVERSION_PROBLEM_SOLVING,
    TRANSFER,
    YOUTH,
    FAMILY,
    OTHER;

    public static CeAssessmentType fromString(String value) {
        return value == null ? null : CeAssessmentType.valueOf(value.toUpperCase());
    }
}
