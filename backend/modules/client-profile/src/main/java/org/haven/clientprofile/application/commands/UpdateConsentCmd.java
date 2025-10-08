package org.haven.clientprofile.application.commands;

import java.util.UUID;

/**
 * Command to update consent limitations or recipient information
 */
public record UpdateConsentCmd(
    UUID consentId,
    String newLimitations,
    String newRecipientContact,
    UUID updatedByUserId
) {
    
    public UpdateConsentCmd {
        if (consentId == null) {
            throw new IllegalArgumentException("Consent ID cannot be null");
        }
        if (updatedByUserId == null) {
            throw new IllegalArgumentException("Updated by user ID cannot be null");
        }
        if ((newLimitations == null || newLimitations.trim().isEmpty()) && 
            (newRecipientContact == null || newRecipientContact.trim().isEmpty())) {
            throw new IllegalArgumentException("At least one field must be updated");
        }
    }
}