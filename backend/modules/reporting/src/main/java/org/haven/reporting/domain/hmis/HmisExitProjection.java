package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.ProjectExitDestination;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HMIS Exit.csv projection
 * Represents the standardized HMIS CSV format for exit data export.
 * Aligned with HMIS 2024 Data Standards CSV schema.
 */
public record HmisExitProjection(
    String exitId,
    String enrollmentId,
    String personalId,
    LocalDate exitDate,
    ProjectExitDestination destination,
    String otherDestination,
    String housingAssessment,
    String subsidy,
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId
) {

    /**
     * Create projection from domain exit for HMIS CSV export
     */
    public static HmisExitProjection fromDomainExit(
            String exitId,
            String enrollmentId,
            String personalId,
            LocalDate exitDate,
            ProjectExitDestination destination,
            String otherDestination,
            LocalDate dateCreated,
            LocalDate dateUpdated,
            String userId,
            String exportId) {
        
        return new HmisExitProjection(
            exitId,
            enrollmentId,
            personalId,
            exitDate,
            destination != null ? destination : ProjectExitDestination.DATA_NOT_COLLECTED,
            otherDestination,
            null, // Housing assessment - would need additional data collection
            null, // Subsidy information - would need additional data collection
            dateCreated,
            dateUpdated,
            userId,
            null, // Not deleted
            exportId
        );
    }

    /**
     * Convert to CSV row format
     */
    public String toCsvRow() {
        return String.join(",",
            quote(exitId),
            quote(enrollmentId),
            quote(personalId),
            quote(formatDate(exitDate)),
            String.valueOf(destination.ordinal() + 1),
            quote(otherDestination),
            quote(housingAssessment),
            quote(subsidy),
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }

    /**
     * Determine if this exit represents a successful outcome
     */
    public boolean isSuccessfulOutcome() {
        return destination.isPermanentDestination() && 
               !destination.isInstitutionalDestination();
    }

    /**
     * Determine if client returned to homelessness
     */
    public boolean isReturnToHomelessness() {
        return destination.isReturnToHomelessness();
    }

    /**
     * Get exit destination category for reporting
     */
    public String getDestinationCategory() {
        if (destination.isPermanentDestination()) {
            return "Permanent";
        } else if (destination.isTemporaryDestination()) {
            return "Temporary";
        } else if (destination.isInstitutionalDestination()) {
            return "Institutional";
        } else if (destination.isReturnToHomelessness()) {
            return "Homeless";
        } else {
            return "Other";
        }
    }

    private String quote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "";
    }
}