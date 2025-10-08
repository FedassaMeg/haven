package org.haven.clientprofile.application.commands;

import java.util.UUID;

/**
 * Command to revoke previously granted consent
 */
public record RevokeConsentCmd(
    UUID consentId,
    UUID revokedByUserId,
    String reason
) {
    
    public RevokeConsentCmd {
        if (consentId == null) {
            throw new IllegalArgumentException("Consent ID cannot be null");
        }
        if (revokedByUserId == null) {
            throw new IllegalArgumentException("Revoked by user ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Revocation reason cannot be null or empty");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // Additional business rule validations
        if (reason.length() < 10) {
            throw new IllegalArgumentException("Revocation reason must be at least 10 characters");
        }
    }
}