package org.haven.programenrollment.domain.ce;

/**
 * Coordinated Entry event types aligning with HUD CSV specification 4.21.
 */
public enum CeEventType {
    REFERRAL_TO_PREVENTION,
    REFERRAL_TO_STREET_OUTREACH,
    REFERRAL_TO_NAVIGATION,
    REFERRAL_TO_PH,
    REFERRAL_TO_RRH,
    REFERRAL_TO_ES,
    EVENT_SAFETY_PLANNING,
    EVENT_DIVERSION,
    EVENT_OTHER;

    public boolean isReferral() {
        return switch (this) {
            case REFERRAL_TO_PREVENTION,
                 REFERRAL_TO_STREET_OUTREACH,
                 REFERRAL_TO_NAVIGATION,
                 REFERRAL_TO_PH,
                 REFERRAL_TO_RRH,
                 REFERRAL_TO_ES -> true;
            default -> false;
        };
    }

    public static CeEventType fromString(String value) {
        return value == null ? null : CeEventType.valueOf(value.toUpperCase());
    }
}
