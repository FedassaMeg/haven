package org.haven.programenrollment.domain.ce;

/**
 * HUD FY2024 prioritization status outcomes for CE assessments.
 */
public enum CePrioritizationStatus {
    PRIORITIZED,
    ACTIVE_NO_OPENING,
    NOT_PRIORITIZED,
    NO_LONGER_PRIORITY,
    OTHER;

    public static CePrioritizationStatus fromString(String value) {
        return value == null ? null : CePrioritizationStatus.valueOf(value.toUpperCase());
    }
}
