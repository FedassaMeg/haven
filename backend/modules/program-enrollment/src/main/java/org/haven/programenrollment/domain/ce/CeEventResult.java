package org.haven.programenrollment.domain.ce;

/**
 * HUD FY2024 result codes for Coordinated Entry events and referrals.
 */
public enum CeEventResult {
    CLIENT_ACCEPTED,
    CLIENT_DECLINED,
    PROVIDER_DECLINED,
    EXPIRED,
    NO_CONTACT,
    OTHER;

    public static CeEventResult fromString(String value) {
        return value == null ? null : CeEventResult.valueOf(value.toUpperCase());
    }
}
