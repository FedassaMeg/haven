package org.haven.shared.vo.hmis;

/**
 * VAWA-compliant recipient categories for data sharing
 * Enforces HUD guidelines on who can receive victim service data
 */
public enum VawaRecipientCategory {

    /**
     * Victim service providers with full VAWA compliance
     */
    VICTIM_SERVICE_PROVIDER(true, true, "VSP"),

    /**
     * Legal aid organizations representing victims
     */
    LEGAL_AID(true, false, "LEGAL"),

    /**
     * Law enforcement with valid case requirements
     */
    LAW_ENFORCEMENT(false, false, "LE"),

    /**
     * Healthcare providers with treatment relationship
     */
    HEALTHCARE_PROVIDER(true, false, "HEALTH"),

    /**
     * Government agencies with statutory authority
     */
    GOVERNMENT_AGENCY(false, false, "GOV"),

    /**
     * Research institutions with IRB approval
     */
    RESEARCH_INSTITUTION(false, false, "RESEARCH"),

    /**
     * Continuum of Care lead agencies
     */
    COC_LEAD(true, false, "COC"),

    /**
     * HMIS lead agencies
     */
    HMIS_LEAD(true, false, "HMIS"),

    /**
     * Emergency shelter providers
     */
    EMERGENCY_SHELTER(true, true, "SHELTER"),

    /**
     * Transitional housing providers
     */
    TRANSITIONAL_HOUSING(true, false, "TH"),

    /**
     * Internal agency use only
     */
    INTERNAL_USE(true, true, "INTERNAL"),

    /**
     * Client self-request
     */
    CLIENT_REQUEST(true, true, "CLIENT"),

    /**
     * Unauthorized or unknown recipient
     */
    UNAUTHORIZED(false, false, "UNAUTHORIZED");

    private final boolean authorizedForVictimData;
    private final boolean fullVawaCompliance;
    private final String code;

    VawaRecipientCategory(boolean authorizedForVictimData, boolean fullVawaCompliance, String code) {
        this.authorizedForVictimData = authorizedForVictimData;
        this.fullVawaCompliance = fullVawaCompliance;
        this.code = code;
    }

    public boolean isAuthorizedForVictimData() {
        return authorizedForVictimData;
    }

    public boolean hasFullVawaCompliance() {
        return fullVawaCompliance;
    }

    public String getCode() {
        return code;
    }

    /**
     * Determine category from recipient organization type
     */
    public static VawaRecipientCategory fromOrganizationType(String orgType) {
        if (orgType == null) {
            return UNAUTHORIZED;
        }

        String normalized = orgType.toUpperCase().replaceAll("[^A-Z]", "");

        return switch (normalized) {
            case "VSP", "VICTIMSERVICEPROVIDER" -> VICTIM_SERVICE_PROVIDER;
            case "LEGAL", "LEGALAID" -> LEGAL_AID;
            case "LE", "LAWENFORCEMENT", "POLICE" -> LAW_ENFORCEMENT;
            case "HEALTH", "HEALTHCARE", "MEDICAL" -> HEALTHCARE_PROVIDER;
            case "GOV", "GOVERNMENT" -> GOVERNMENT_AGENCY;
            case "RESEARCH", "UNIVERSITY", "IRB" -> RESEARCH_INSTITUTION;
            case "COC", "COCLEAD", "CONTINUUM" -> COC_LEAD;
            case "HMIS", "HMISLEAD" -> HMIS_LEAD;
            case "SHELTER", "EMERGENCY" -> EMERGENCY_SHELTER;
            case "TH", "TRANSITIONAL" -> TRANSITIONAL_HOUSING;
            case "INTERNAL", "AGENCY" -> INTERNAL_USE;
            case "CLIENT", "SELF" -> CLIENT_REQUEST;
            default -> UNAUTHORIZED;
        };
    }

    /**
     * Get required anonymization level based on category
     */
    public AnonymizationLevel getRequiredAnonymizationLevel() {
        if (!authorizedForVictimData) {
            return AnonymizationLevel.FULL;
        }
        if (fullVawaCompliance) {
            return AnonymizationLevel.MINIMAL;
        }
        return AnonymizationLevel.STANDARD;
    }

    public enum AnonymizationLevel {
        MINIMAL,    // VSPs and fully compliant recipients
        STANDARD,   // Authorized but limited recipients
        FULL        // Unauthorized recipients - maximum redaction
    }
}