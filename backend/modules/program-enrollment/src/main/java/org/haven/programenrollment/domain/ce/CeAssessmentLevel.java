package org.haven.programenrollment.domain.ce;

/**
 * HUD FY2024 Coordinated Entry assessment level enumeration.
 */
public enum CeAssessmentLevel {
    PRE_SCREEN,
    FULL_ASSESSMENT,
    POST_ASSESSMENT;

    public static CeAssessmentLevel fromString(String value) {
        return value == null ? null : CeAssessmentLevel.valueOf(value.toUpperCase());
    }
}
