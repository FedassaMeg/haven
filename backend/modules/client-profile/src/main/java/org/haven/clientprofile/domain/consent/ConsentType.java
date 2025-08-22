package org.haven.clientprofile.domain.consent;

/**
 * Types of consent that can be granted or revoked
 * Based on VAWA confidentiality requirements and case management needs
 */
public enum ConsentType {
    /**
     * General information sharing between service providers
     */
    INFORMATION_SHARING("Information Sharing", "Consent to share general case information"),
    
    /**
     * Participation in HMIS data collection and sharing
     */
    HMIS_PARTICIPATION("HMIS Participation", "Consent to participate in HMIS data collection"),
    
    /**
     * Court testimony and legal proceedings
     */
    COURT_TESTIMONY("Court Testimony", "Consent to provide testimony in court proceedings"),
    
    /**
     * Communication with legal counsel
     */
    LEGAL_COUNSEL_COMMUNICATION("Legal Counsel Communication", "Consent to communicate with attorney"),
    
    /**
     * Medical information sharing with healthcare providers
     */
    MEDICAL_INFORMATION_SHARING("Medical Information Sharing", "Consent to share medical information"),
    
    /**
     * Contact with family members or emergency contacts
     */
    FAMILY_CONTACT("Family Contact", "Consent to contact family members or emergency contacts"),
    
    /**
     * Participation in research studies or program evaluation
     */
    RESEARCH_PARTICIPATION("Research Participation", "Consent to participate in research or evaluation"),
    
    /**
     * Photography or media for program materials
     */
    MEDIA_RELEASE("Media Release", "Consent for photography or media use"),
    
    /**
     * Referrals to other service providers or agencies
     */
    REFERRAL_SHARING("Referral Sharing", "Consent to share information for service referrals"),
    
    /**
     * Contact after case closure for follow-up services
     */
    FOLLOW_UP_CONTACT("Follow-up Contact", "Consent for post-case closure contact");
    
    private final String displayName;
    private final String description;
    
    ConsentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns true if this consent type requires special handling for DV survivors
     */
    public boolean isHighRisk() {
        return this == FAMILY_CONTACT || this == COURT_TESTIMONY || this == LEGAL_COUNSEL_COMMUNICATION;
    }
    
    /**
     * Returns true if this consent type is required for HMIS participation
     */
    public boolean isRequiredForHMIS() {
        return this == HMIS_PARTICIPATION;
    }
    
    /**
     * Returns true if this consent type can be granted by default for safety
     */
    public boolean canBeDefaulted() {
        return this == INFORMATION_SHARING || this == REFERRAL_SHARING;
    }
}