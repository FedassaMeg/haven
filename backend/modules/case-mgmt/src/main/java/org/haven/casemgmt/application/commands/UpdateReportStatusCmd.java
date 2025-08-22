package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.mandatedreport.ReportStatus;

import java.util.UUID;

/**
 * Command to update mandated report status
 */
public record UpdateReportStatusCmd(
    UUID reportId,
    ReportStatus newStatus,
    String statusReason,
    UUID updatedByUserId
) {
    
    public UpdateReportStatusCmd {
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (statusReason == null || statusReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Status reason cannot be null or empty");
        }
        if (updatedByUserId == null) {
            throw new IllegalArgumentException("Updated by user ID cannot be null");
        }
    }
    
    /**
     * Validate command for business rules
     */
    public void validate() {
        // Status reason must be detailed enough for final statuses
        if (newStatus.isFinal() && statusReason.length() < 20) {
            throw new IllegalArgumentException("Final status updates require detailed reason (min 20 characters)");
        }
    }
}