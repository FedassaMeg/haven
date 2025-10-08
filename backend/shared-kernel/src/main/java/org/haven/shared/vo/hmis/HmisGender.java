package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.06 Gender
 * Represents the client's self-identification of gender.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum HmisGender {
    WOMAN("Woman (including trans woman)"),
    MAN("Man (including trans man)"),
    NON_BINARY("Non-binary"),
    CULTURALLY_SPECIFIC("Culturally specific identity"),
    TRANSGENDER("Transgender"),
    QUESTIONING("Questioning"),
    DIFFERENT_IDENTITY("Different identity"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    HmisGender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static HmisGender fromLegacyGender(String legacyGender) {
        if (legacyGender == null) return DATA_NOT_COLLECTED;
        
        return switch (legacyGender.toUpperCase()) {
            case "MALE" -> MAN;
            case "FEMALE" -> WOMAN;
            case "OTHER" -> NON_BINARY;
            case "UNKNOWN" -> CLIENT_DOESNT_KNOW;
            default -> DATA_NOT_COLLECTED;
        };
    }
}