package org.haven.reporting.domain;

/**
 * Consent scopes required for unhashed export authorization
 * Based on VAWA, HIPAA, and HUD confidentiality requirements
 */
public enum ExportConsentScope {
    /**
     * HUD reporting consent - required for all HMIS exports
     */
    HUD_REPORTING,

    /**
     * PII disclosure consent - required for unhashed exports
     */
    PII_DISCLOSURE,

    /**
     * VAWA override - required for DV-related data in unhashed form
     */
    VAWA_OVERRIDE,

    /**
     * Legal/subpoena - administrative override for legal compliance
     */
    LEGAL_SUBPOENA,

    /**
     * Research IRB approval - for approved research projects
     */
    RESEARCH_IRB,

    /**
     * Coordinated Entry partner sharing
     */
    CE_PARTNER_SHARING;

    /**
     * @return true if this scope requires VAWA clearance
     */
    public boolean requiresVawaClearance() {
        return this == VAWA_OVERRIDE;
    }

    /**
     * @return true if this scope requires legal review
     */
    public boolean requiresLegalReview() {
        return this == LEGAL_SUBPOENA || this == RESEARCH_IRB;
    }

    /**
     * @return true if this scope is sufficient for unhashed PII
     */
    public boolean authorizesUnhashedPII() {
        return this == PII_DISCLOSURE ||
               this == VAWA_OVERRIDE ||
               this == LEGAL_SUBPOENA ||
               this == RESEARCH_IRB;
    }

    /**
     * @return human-readable description of this consent scope
     */
    public String getDescription() {
        return switch (this) {
            case HUD_REPORTING -> "HUD Reporting Consent";
            case PII_DISCLOSURE -> "PII Disclosure Consent";
            case VAWA_OVERRIDE -> "VAWA Override Authorization";
            case LEGAL_SUBPOENA -> "Legal/Subpoena Requirement";
            case RESEARCH_IRB -> "Research IRB Approval";
            case CE_PARTNER_SHARING -> "Coordinated Entry Partner Sharing";
        };
    }
}
