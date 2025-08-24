package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HMIS Enrollment.csv projection
 * Represents the standardized HMIS CSV format for enrollment data export.
 * Aligned with HMIS 2024 Data Standards CSV schema.
 */
public record HmisEnrollmentProjection(
    String enrollmentId,
    HmisPersonalId personalId,
    String projectId,
    LocalDate entryDate,
    String householdId,
    RelationshipToHeadOfHousehold relationshipToHoH,
    PriorLivingSituation priorLivingSituation,
    LengthOfStay lengthOfStayPriorToDiEntry,
    String entryFromStreetESSH,
    Integer monthsHomelessPastThreeYears,
    Integer timesHomelessPastThreeYears,
    DisablingCondition disablingCondition,
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId
) {

    /**
     * Create projection from domain enrollment for HMIS CSV export
     */
    public static HmisEnrollmentProjection fromDomainEnrollment(
            String enrollmentId,
            HmisPersonalId personalId,
            String projectId,
            LocalDate entryDate,
            String householdId,
            RelationshipToHeadOfHousehold relationshipToHoH,
            PriorLivingSituation priorLivingSituation,
            LengthOfStay lengthOfStay,
            DisablingCondition disablingCondition,
            LocalDate dateCreated,
            LocalDate dateUpdated,
            String userId,
            String exportId) {
        
        return new HmisEnrollmentProjection(
            enrollmentId,
            personalId,
            projectId,
            entryDate,
            householdId,
            relationshipToHoH != null ? relationshipToHoH : RelationshipToHeadOfHousehold.DATA_NOT_COLLECTED,
            priorLivingSituation != null ? priorLivingSituation : PriorLivingSituation.DATA_NOT_COLLECTED,
            lengthOfStay != null ? lengthOfStay : LengthOfStay.DATA_NOT_COLLECTED,
            null, // Entry from street/ES/SH calculated from prior living situation
            null, // Months homeless - would need additional data collection
            null, // Times homeless - would need additional data collection
            disablingCondition != null ? disablingCondition : DisablingCondition.DATA_NOT_COLLECTED,
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
            quote(enrollmentId),
            quote(personalId.value()),
            quote(projectId),
            quote(formatDate(entryDate)),
            quote(householdId),
            String.valueOf(relationshipToHoH.ordinal() + 1),
            String.valueOf(priorLivingSituation.ordinal() + 1),
            String.valueOf(lengthOfStayPriorToDiEntry.ordinal() + 1),
            quote(entryFromStreetESSH),
            formatInteger(monthsHomelessPastThreeYears),
            formatInteger(timesHomelessPastThreeYears),
            String.valueOf(disablingCondition.ordinal() + 1),
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }

    /**
     * Determine if this enrollment represents chronic homelessness
     * Based on HMIS chronic homelessness criteria
     */
    public boolean isChronicallyHomeless() {
        if (disablingCondition != DisablingCondition.YES) {
            return false; // Must have disabling condition
        }

        // Check if coming from literally homeless situation for 12+ months
        if (priorLivingSituation.isLiterallyHomeless() && 
            lengthOfStayPriorToDiEntry.isLongTerm()) {
            return true;
        }

        // Additional logic would check for 4+ episodes totaling 12+ months
        // This would require additional data elements not captured in basic projection
        return false;
    }

    /**
     * Determine if this is a family household
     */
    public boolean isFamilyHousehold() {
        return relationshipToHoH.isFamilyMember() || 
               relationshipToHoH.isHeadOfHousehold();
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

    private String formatInteger(Integer value) {
        return value != null ? value.toString() : "";
    }
}