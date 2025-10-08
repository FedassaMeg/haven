package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * HMIS Client.csv projection
 * Represents the standardized HMIS CSV format for client data export.
 * Aligned with HMIS 2024 Data Standards CSV schema.
 */
public record HmisClientProjection(
    HmisPersonalId personalId,
    String firstName,
    String middleName,
    String lastName,
    String nameSuffix,
    Integer nameDataQuality,
    String ssn,
    Integer ssnDataQuality,
    LocalDate dateOfBirth,
    Integer dobDataQuality,
    Set<HmisRace> race,
    Set<HmisGender> gender,
    String otherGender,
    VeteranStatus veteranStatus,
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId
) {

    /**
     * Create projection from domain Client for HMIS CSV export
     */
    public static HmisClientProjection fromDomainClient(
            HmisPersonalId personalId,
            String firstName,
            String middleName,
            String lastName,
            String nameSuffix,
            String ssn,
            LocalDate dateOfBirth,
            Set<HmisRace> race,
            Set<HmisGender> gender,
            VeteranStatus veteranStatus,
            LocalDate dateCreated,
            LocalDate dateUpdated,
            String userId,
            String exportId) {
        
        return new HmisClientProjection(
            personalId,
            firstName,
            middleName,
            lastName,
            nameSuffix,
            determineNameDataQuality(firstName, lastName),
            anonymizeSSN(ssn),
            determineSsnDataQuality(ssn),
            dateOfBirth,
            determineDobDataQuality(dateOfBirth),
            race != null ? race : Set.of(HmisRace.DATA_NOT_COLLECTED),
            gender != null ? gender : Set.of(HmisGender.DATA_NOT_COLLECTED),
            extractOtherGender(gender),
            veteranStatus != null ? veteranStatus : VeteranStatus.DATA_NOT_COLLECTED,
            dateCreated,
            dateUpdated,
            userId,
            null, // Not deleted
            exportId
        );
    }

    private static Integer determineNameDataQuality(String firstName, String lastName) {
        if ((firstName == null || firstName.trim().isEmpty()) && 
            (lastName == null || lastName.trim().isEmpty())) {
            return 9; // Data not collected
        }
        if ((firstName != null && firstName.trim().length() > 0) && 
            (lastName != null && lastName.trim().length() > 0)) {
            return 1; // Full name reported
        }
        return 2; // Partial name reported
    }

    private static String anonymizeSSN(String ssn) {
        // For privacy, only include if complete and valid
        if (ssn != null && ssn.replaceAll("[^0-9]", "").length() == 9) {
            return ssn;
        }
        return null;
    }

    private static Integer determineSsnDataQuality(String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            return 9; // Data not collected
        }
        String digitsOnly = ssn.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 9 && !digitsOnly.equals("000000000")) {
            return 1; // Full SSN reported
        }
        return 2; // Approximate or partial SSN reported
    }

    private static Integer determineDobDataQuality(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 9; // Data not collected
        }
        // Check if it's a placeholder date
        if (dateOfBirth.equals(LocalDate.of(1900, 1, 1))) {
            return 9; // Data not collected
        }
        return 1; // Full date reported
    }

    private static String extractOtherGender(Set<HmisGender> genders) {
        if (genders != null && genders.contains(HmisGender.DIFFERENT_IDENTITY)) {
            return "Other"; // Would need to be populated from free-text field
        }
        return null;
    }

    /**
     * Convert to CSV row format
     */
    public String toCsvRow() {
        return String.join(",",
            quote(personalId.value()),
            quote(firstName),
            quote(middleName),
            quote(lastName),
            quote(nameSuffix),
            String.valueOf(nameDataQuality),
            quote(ssn),
            String.valueOf(ssnDataQuality),
            quote(formatDate(dateOfBirth)),
            String.valueOf(dobDataQuality),
            formatRaceForCsv(),
            formatGenderForCsv(),
            quote(otherGender),
            String.valueOf(veteranStatus.ordinal() + 1), // HMIS uses 1-based indexing
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }

    private String formatRaceForCsv() {
        if (race.isEmpty()) {
            return "9"; // Data not collected
        }
        return race.stream()
                .mapToInt(r -> r.ordinal() + 1) // Convert to HMIS codes
                .sorted()
                .mapToObj(String::valueOf)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ";" + b);
    }

    private String formatGenderForCsv() {
        if (gender.isEmpty()) {
            return "9"; // Data not collected
        }
        return gender.stream()
                .mapToInt(g -> g.ordinal() + 1) // Convert to HMIS codes
                .sorted()
                .mapToObj(String::valueOf)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ";" + b);
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
    
    /**
     * Create a copy with redacted name information for privacy protection
     */
    public HmisClientProjection withRedactedName() {
        return new HmisClientProjection(
            personalId,
            "[REDACTED]", // firstName
            null, // middleName
            "[REDACTED]", // lastName
            null, // nameSuffix
            9, // nameDataQuality - Data not collected
            ssn,
            ssnDataQuality,
            dateOfBirth,
            dobDataQuality,
            race,
            gender,
            otherGender,
            veteranStatus,
            dateCreated,
            dateUpdated,
            userId,
            dateDeleted,
            exportId
        );
    }
    
    /**
     * Create a copy with redacted location-sensitive information
     * This doesn't apply to Client.csv but included for consistency
     */
    public HmisClientProjection withRedactedLocation() {
        // For Client.csv, location data is not typically included
        // Return the same instance as no location redaction needed
        return this;
    }
}