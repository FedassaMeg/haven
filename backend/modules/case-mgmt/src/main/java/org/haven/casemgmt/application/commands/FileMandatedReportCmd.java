package org.haven.casemgmt.application.commands;

import java.util.UUID;

/**
 * Command to file a mandated report with the appropriate agency
 */
public record FileMandatedReportCmd(
    UUID reportId,
    String agencyContactInfo,
    UUID filedByUserId
) {
    
    public FileMandatedReportCmd {
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        if (agencyContactInfo == null || agencyContactInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("Agency contact info cannot be null or empty");
        }
        if (filedByUserId == null) {
            throw new IllegalArgumentException("Filed by user ID cannot be null");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // Agency contact info should include at least phone number or email
        String contact = agencyContactInfo.toLowerCase();
        boolean hasPhone = contact.matches(".*\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}.*");
        boolean hasEmail = contact.contains("@") && contact.contains(".");
        
        if (!hasPhone && !hasEmail) {
            throw new IllegalArgumentException("Agency contact info must include phone number or email");
        }
    }
}