package org.haven.programenrollment.domain.ce;

/**
 * Result of a Coordinated Entry referral based on HUD 2024 data standards.
 */
public enum CeReferralResult {
    SUCCESSFUL_CLIENT_ACCEPTED("1", "Successful referral: client accepted"),
    UNSUCCESSFUL_CLIENT_REJECTED("2", "Unsuccessful referral: client rejected"),
    UNSUCCESSFUL_PROVIDER_REJECTED("3", "Unsuccessful referral: provider rejected"),
    CLIENT_DOESNT_KNOW("8", "Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("9", "Client prefers not to answer"),
    DATA_NOT_COLLECTED("99", "Data not collected");

    private final String hudCode;
    private final String description;

    CeReferralResult(String hudCode, String description) {
        this.hudCode = hudCode;
        this.description = description;
    }

    public String getHudCode() {
        return hudCode;
    }

    public String getDescription() {
        return description;
    }

    public static CeReferralResult fromHudCode(String hudCode) {
        for (CeReferralResult result : values()) {
            if (result.hudCode.equals(hudCode)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Unknown HUD code: " + hudCode);
    }
}