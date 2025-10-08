package org.haven.clientprofile.application.commands;

import java.time.Instant;
import java.util.UUID;

/**
 * Command to extend consent expiration date
 */
public record ExtendConsentCmd(
    UUID consentId,
    Instant newExpirationDate,
    UUID extendedByUserId
) {
    
    public ExtendConsentCmd {
        if (consentId == null) {
            throw new IllegalArgumentException("Consent ID cannot be null");
        }
        if (newExpirationDate == null) {
            throw new IllegalArgumentException("New expiration date cannot be null");
        }
        if (extendedByUserId == null) {
            throw new IllegalArgumentException("Extended by user ID cannot be null");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        if (newExpirationDate.isBefore(Instant.now())) {
            throw new IllegalArgumentException("New expiration date cannot be in the past");
        }
        
        // Maximum extension is 2 years from now for safety
        Instant maxExtension = Instant.now().plusSeconds(2 * 365 * 24 * 3600);
        if (newExpirationDate.isAfter(maxExtension)) {
            throw new IllegalArgumentException("Cannot extend consent more than 2 years from now");
        }
    }
}