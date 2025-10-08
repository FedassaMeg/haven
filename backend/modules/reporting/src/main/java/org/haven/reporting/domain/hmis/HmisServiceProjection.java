package org.haven.reporting.domain.hmis;

import java.time.LocalDate;

/**
 * HMIS Service Projection for CSV export
 * Maps ServiceEpisode data to HMIS-compliant service records
 */
public record HmisServiceProjection(
    String serviceId,
    String personalId, // Links to client via enrollment
    String enrollmentId,
    LocalDate serviceDate,
    String serviceType,
    String serviceSubType,
    Double serviceHours,
    String fundingSource,
    Double serviceAmount,
    String serviceOutcome,
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    String exportId
) {

    /**
     * Create from ServiceEpisode domain object
     */
    public static HmisServiceProjection fromServiceEpisode(
            String serviceId,
            String enrollmentId,
            LocalDate serviceDate,
            String serviceType,
            String serviceSubType,
            Double serviceHours,
            String fundingSource,
            Double serviceAmount,
            String serviceOutcome,
            LocalDate dateCreated,
            LocalDate dateUpdated,
            String userId,
            String exportId) {

        return new HmisServiceProjection(
            serviceId,
            null, // PersonalId derived from enrollment lookup
            enrollmentId,
            serviceDate,
            serviceType,
            serviceSubType,
            serviceHours,
            fundingSource,
            serviceAmount,
            serviceOutcome,
            dateCreated,
            dateUpdated,
            userId,
            exportId
        );
    }

    /**
     * Set personal ID after enrollment lookup
     */
    public HmisServiceProjection withPersonalId(String personalId) {
        return new HmisServiceProjection(
            serviceId,
            personalId,
            enrollmentId,
            serviceDate,
            serviceType,
            serviceSubType,
            serviceHours,
            fundingSource,
            serviceAmount,
            serviceOutcome,
            dateCreated,
            dateUpdated,
            userId,
            exportId
        );
    }

    /**
     * Validate HMIS service data requirements
     */
    public boolean isHmisValid() {
        return serviceId != null &&
               personalId != null &&
               enrollmentId != null &&
               serviceDate != null &&
               serviceType != null &&
               dateCreated != null &&
               userId != null &&
               exportId != null;
    }

    /**
     * Check if service meets documentation standards
     */
    public boolean meetsDocumentationStandards() {
        return isHmisValid() &&
               (serviceOutcome != null && !serviceOutcome.trim().isEmpty()) &&
               serviceHours != null &&
               serviceHours > 0;
    }

    /**
     * Convert to CSV row format for HMIS export
     */
    public String[] toCsvRow() {
        return new String[] {
            serviceId,
            personalId,
            enrollmentId,
            serviceDate != null ? serviceDate.toString() : "",
            serviceType != null ? serviceType : "",
            serviceSubType != null ? serviceSubType : "",
            serviceHours != null ? serviceHours.toString() : "",
            fundingSource != null ? fundingSource : "",
            serviceAmount != null ? serviceAmount.toString() : "",
            serviceOutcome != null ? serviceOutcome : "",
            dateCreated != null ? dateCreated.toString() : "",
            dateUpdated != null ? dateUpdated.toString() : "",
            userId != null ? userId : "",
            exportId != null ? exportId : ""
        };
    }

    /**
     * Get CSV headers for HMIS service export
     */
    public static String[] getCsvHeaders() {
        return new String[] {
            "ServiceID",
            "PersonalID",
            "EnrollmentID",
            "ServiceDate",
            "ServiceType",
            "ServiceSubType",
            "ServiceHours",
            "FundingSource",
            "ServiceAmount",
            "ServiceOutcome",
            "DateCreated",
            "DateUpdated",
            "UserID",
            "ExportID"
        };
    }
}