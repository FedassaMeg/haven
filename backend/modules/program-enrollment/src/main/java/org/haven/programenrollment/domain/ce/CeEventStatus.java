package org.haven.programenrollment.domain.ce;

/**
 * Lifecycle status for Coordinated Entry events/referrals.
 */
public enum CeEventStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CLOSED;

    public static CeEventStatus fromString(String value) {
        return value == null ? null : CeEventStatus.valueOf(value.toUpperCase());
    }
}
