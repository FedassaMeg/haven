package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.consent.ConsentType;

import java.util.UUID;

/**
 * Command to grant consent for a specific purpose
 */
public record GrantConsentCmd(
    UUID clientId,
    ConsentType consentType,
    String purpose,
    String recipientOrganization,
    String recipientContact,
    UUID grantedByUserId,
    Integer durationMonths,
    String limitations
) {
    
    public GrantConsentCmd {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        if (consentType == null) {
            throw new IllegalArgumentException("Consent type cannot be null");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Purpose cannot be null or empty");
        }
        if (grantedByUserId == null) {
            throw new IllegalArgumentException("Granted by user ID cannot be null");
        }
        if (durationMonths != null && durationMonths <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // Additional business rule validations
        if (consentType.isHighRisk() && (limitations == null || limitations.trim().isEmpty())) {
            throw new IllegalArgumentException("High-risk consent types require explicit limitations");
        }
        
        if (consentType == ConsentType.HMIS_PARTICIPATION && recipientOrganization == null) {
            throw new IllegalArgumentException("HMIS participation consent requires recipient organization");
        }
        
        if (consentType == ConsentType.COURT_TESTIMONY && recipientContact == null) {
            throw new IllegalArgumentException("Court testimony consent requires specific recipient contact");
        }
    }
}