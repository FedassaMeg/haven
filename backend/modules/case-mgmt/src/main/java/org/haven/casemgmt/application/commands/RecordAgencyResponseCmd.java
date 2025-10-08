package org.haven.casemgmt.application.commands;

import java.util.UUID;

/**
 * Command to record agency response or investigation outcome
 */
public record RecordAgencyResponseCmd(
    UUID reportId,
    String response,
    String investigationOutcome,
    UUID recordedByUserId
) {
    
    public RecordAgencyResponseCmd {
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("Response cannot be null or empty");
        }
        if (recordedByUserId == null) {
            throw new IllegalArgumentException("Recorded by user ID cannot be null");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // Response must be detailed enough
        if (response.length() < 20) {
            throw new IllegalArgumentException("Agency response must be at least 20 characters");
        }
        
        // Investigation outcome, if provided, must be detailed
        if (investigationOutcome != null && 
            !investigationOutcome.trim().isEmpty() && 
            investigationOutcome.length() < 10) {
            throw new IllegalArgumentException("Investigation outcome must be at least 10 characters");
        }
    }
}