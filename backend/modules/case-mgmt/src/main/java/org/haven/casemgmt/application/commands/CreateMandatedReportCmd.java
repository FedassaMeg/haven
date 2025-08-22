package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.mandatedreport.ReportType;

import java.time.Instant;
import java.util.UUID;

/**
 * Command to create a new mandated report
 */
public record CreateMandatedReportCmd(
    UUID caseId,
    UUID clientId,
    ReportType reportType,
    String incidentDescription,
    Instant incidentDateTime,
    UUID createdByUserId,
    String legalJustification
) {
    
    public CreateMandatedReportCmd {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        if (reportType == null) {
            throw new IllegalArgumentException("Report type cannot be null");
        }
        if (incidentDescription == null || incidentDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Incident description cannot be null or empty");
        }
        if (incidentDateTime == null) {
            throw new IllegalArgumentException("Incident date/time cannot be null");
        }
        if (createdByUserId == null) {
            throw new IllegalArgumentException("Created by user ID cannot be null");
        }
        if (legalJustification == null || legalJustification.trim().isEmpty()) {
            throw new IllegalArgumentException("Legal justification cannot be null or empty");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // Incident cannot be in the future
        if (incidentDateTime.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Incident date/time cannot be in the future");
        }
        
        // Incident cannot be too far in the past (more than 5 years)
        Instant fiveYearsAgo = Instant.now().minusSeconds(5 * 365 * 24 * 3600);
        if (incidentDateTime.isBefore(fiveYearsAgo)) {
            throw new IllegalArgumentException("Incident date/time cannot be more than 5 years ago");
        }
        
        // Description must be detailed enough
        if (incidentDescription.length() < 50) {
            throw new IllegalArgumentException("Incident description must be at least 50 characters");
        }
        
        // Legal justification must be detailed enough
        if (legalJustification.length() < 20) {
            throw new IllegalArgumentException("Legal justification must be at least 20 characters");
        }
    }
}