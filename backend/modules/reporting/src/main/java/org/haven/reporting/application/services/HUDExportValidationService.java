package org.haven.reporting.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates HUD HMIS exports against HUD Data Quality Framework.
 *
 * Implements checks for:
 * - Universal Data Element completeness and validity
 * - Referential integrity across CSV files
 * - Date logic and sequencing
 * - Code list conformance
 * - SSN/DOB validation
 */
@Service
public class HUDExportValidationService {

    private static final Logger logger = LoggerFactory.getLogger(HUDExportValidationService.class);

    // HUD Data Quality thresholds
    private static final double REQUIRED_COMPLETENESS_THRESHOLD = 0.95; // 95%
    private static final int MAX_AGE_YEARS = 120;
    private static final int MIN_AGE_YEARS = 0;

    // SSN validation
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Set<String> INVALID_SSNS = Set.of(
            "000000000", "111111111", "222222222", "333333333",
            "444444444", "555555555", "666666666", "777777777",
            "888888888", "999999999", "123456789"
    );

    /**
     * Validate complete export package
     */
    public ValidationResult validateExport(Map<String, List<Map<String, Object>>> sections) {
        logger.info("Starting HUD export validation");

        ValidationResult result = new ValidationResult();

        // Extract sections
        List<Map<String, Object>> clients = sections.getOrDefault("Client", Collections.emptyList());
        List<Map<String, Object>> enrollments = sections.getOrDefault("Enrollment", Collections.emptyList());
        List<Map<String, Object>> exits = sections.getOrDefault("Exit", Collections.emptyList());
        List<Map<String, Object>> projects = sections.getOrDefault("Project", Collections.emptyList());
        List<Map<String, Object>> services = sections.getOrDefault("Services", Collections.emptyList());

        // 1. Universal Data Element validation
        validateClients(clients, result);

        // 2. Enrollment validation
        validateEnrollments(enrollments, result);

        // 3. Exit validation
        validateExits(exits, result);

        // 4. Referential integrity
        validateReferentialIntegrity(clients, enrollments, exits, projects, services, result);

        // 5. Date sequencing
        validateDateSequencing(enrollments, exits, result);

        // 6. Completeness rates
        validateCompletenessRates(sections, result);

        logger.info("Validation complete: {} errors, {} warnings",
                result.getErrorCount(), result.getWarningCount());

        return result;
    }

    /**
     * Validate Client.csv records
     */
    private void validateClients(List<Map<String, Object>> clients, ValidationResult result) {
        logger.info("Validating {} client records", clients.size());

        for (int i = 0; i < clients.size(); i++) {
            Map<String, Object> client = clients.get(i);
            String personalId = getString(client, "PersonalID");
            String context = "Client row " + (i + 1) + " (PersonalID=" + personalId + ")";

            // SSN validation
            String ssn = getString(client, "SSN");
            Integer ssnDataQuality = getInteger(client, "SSNDataQuality");

            if (ssn != null && !ssn.isEmpty()) {
                if (!SSN_PATTERN.matcher(ssn).matches()) {
                    result.addError(context, "SSN must be 9 digits: " + ssn);
                } else if (INVALID_SSNS.contains(ssn)) {
                    result.addError(context, "SSN contains invalid pattern: " + ssn);
                }
            } else if (ssnDataQuality == null || ssnDataQuality == 99) {
                result.addWarning(context, "SSN missing or unknown (DataQuality=" + ssnDataQuality + ")");
            }

            // DOB validation
            LocalDate dob = getDate(client, "DOB");
            Integer dobDataQuality = getInteger(client, "DOBDataQuality");

            if (dob != null) {
                int age = Period.between(dob, LocalDate.now()).getYears();
                if (age < MIN_AGE_YEARS || age > MAX_AGE_YEARS) {
                    result.addError(context, "DOB yields invalid age: " + age + " years (DOB=" + dob + ")");
                }
            } else if (dobDataQuality == null || dobDataQuality == 99) {
                result.addWarning(context, "DOB missing or unknown (DataQuality=" + dobDataQuality + ")");
            }

            // Name validation
            String firstName = getString(client, "FirstName");
            String lastName = getString(client, "LastName");
            Integer nameDataQuality = getInteger(client, "NameDataQuality");

            if ((firstName == null || firstName.isEmpty()) &&
                (lastName == null || lastName.isEmpty()) &&
                (nameDataQuality == null || nameDataQuality == 99)) {
                result.addError(context, "Client name completely missing");
            }

            // Race/Ethnicity - at least one required
            Boolean amIndAKNative = getBoolean(client, "AmIndAKNative");
            Boolean asian = getBoolean(client, "Asian");
            Boolean blackAfAmerican = getBoolean(client, "BlackAfAmerican");
            Boolean nativeHIPacific = getBoolean(client, "NativeHIPacific");
            Boolean white = getBoolean(client, "White");
            Boolean raceNone = getBoolean(client, "RaceNone");

            boolean hasRace = (amIndAKNative != null && amIndAKNative) ||
                    (asian != null && asian) ||
                    (blackAfAmerican != null && blackAfAmerican) ||
                    (nativeHIPacific != null && nativeHIPacific) ||
                    (white != null && white) ||
                    (raceNone != null && raceNone);

            if (!hasRace) {
                result.addError(context, "At least one race category required");
            }

            // Gender - at least one required
            Boolean woman = getBoolean(client, "Woman");
            Boolean man = getBoolean(client, "Man");
            Boolean nonBinary = getBoolean(client, "NonBinary");
            Boolean genderNone = getBoolean(client, "GenderNone");

            boolean hasGender = (woman != null && woman) ||
                    (man != null && man) ||
                    (nonBinary != null && nonBinary) ||
                    (genderNone != null && genderNone);

            if (!hasGender) {
                result.addError(context, "At least one gender category required");
            }
        }
    }

    /**
     * Validate Enrollment.csv records
     */
    private void validateEnrollments(List<Map<String, Object>> enrollments, ValidationResult result) {
        logger.info("Validating {} enrollment records", enrollments.size());

        for (int i = 0; i < enrollments.size(); i++) {
            Map<String, Object> enrollment = enrollments.get(i);
            String enrollmentId = getString(enrollment, "EnrollmentID");
            String context = "Enrollment row " + (i + 1) + " (EnrollmentID=" + enrollmentId + ")";

            // Required fields
            if (getString(enrollment, "PersonalID") == null) {
                result.addError(context, "PersonalID is required");
            }
            if (getString(enrollment, "ProjectID") == null) {
                result.addError(context, "ProjectID is required");
            }
            if (getDate(enrollment, "EntryDate") == null) {
                result.addError(context, "EntryDate is required");
            }
            if (getString(enrollment, "HouseholdID") == null) {
                result.addError(context, "HouseholdID is required");
            }

            // RelationshipToHoH validation
            Integer relationshipToHoH = getInteger(enrollment, "RelationshipToHoH");
            if (relationshipToHoH == null) {
                result.addError(context, "RelationshipToHoH is required");
            } else if (!isValidCodeListValue(relationshipToHoH, 1, 5)) {
                result.addError(context, "RelationshipToHoH invalid value: " + relationshipToHoH);
            }

            // DisablingCondition
            Integer disablingCondition = getInteger(enrollment, "DisablingCondition");
            if (disablingCondition != null && !isValidCodeListValue(disablingCondition, 0, 1, 8, 9, 99)) {
                result.addError(context, "DisablingCondition invalid value: " + disablingCondition);
            }

            // LivingSituation (at entry)
            Integer livingSituation = getInteger(enrollment, "LivingSituation");
            if (livingSituation == null) {
                result.addError(context, "LivingSituation is required");
            }
        }
    }

    /**
     * Validate Exit.csv records
     */
    private void validateExits(List<Map<String, Object>> exits, ValidationResult result) {
        logger.info("Validating {} exit records", exits.size());

        for (int i = 0; i < exits.size(); i++) {
            Map<String, Object> exit = exits.get(i);
            String exitId = getString(exit, "ExitID");
            String context = "Exit row " + (i + 1) + " (ExitID=" + exitId + ")";

            // Required fields
            if (getString(exit, "EnrollmentID") == null) {
                result.addError(context, "EnrollmentID is required");
            }
            if (getDate(exit, "ExitDate") == null) {
                result.addError(context, "ExitDate is required");
            }

            // Destination validation
            Integer destination = getInteger(exit, "Destination");
            if (destination == null) {
                result.addError(context, "Destination is required");
            } else if (!isValidDestination(destination)) {
                result.addError(context, "Destination invalid value: " + destination);
            }
        }
    }

    /**
     * Validate referential integrity across files
     */
    private void validateReferentialIntegrity(
            List<Map<String, Object>> clients,
            List<Map<String, Object>> enrollments,
            List<Map<String, Object>> exits,
            List<Map<String, Object>> projects,
            List<Map<String, Object>> services,
            ValidationResult result) {

        logger.info("Validating referential integrity");

        // Build reference sets
        Set<String> personalIds = buildIdSet(clients, "PersonalID");
        Set<String> enrollmentIds = buildIdSet(enrollments, "EnrollmentID");
        Set<String> projectIds = buildIdSet(projects, "ProjectID");

        // Validate Enrollment → Client references
        for (Map<String, Object> enrollment : enrollments) {
            String personalId = getString(enrollment, "PersonalID");
            if (personalId != null && !personalIds.contains(personalId)) {
                result.addError("Enrollment EnrollmentID=" + getString(enrollment, "EnrollmentID"),
                        "PersonalID not found in Client.csv: " + personalId);
            }
        }

        // Validate Enrollment → Project references
        for (Map<String, Object> enrollment : enrollments) {
            String projectId = getString(enrollment, "ProjectID");
            if (projectId != null && !projectIds.contains(projectId)) {
                result.addError("Enrollment EnrollmentID=" + getString(enrollment, "EnrollmentID"),
                        "ProjectID not found in Project.csv: " + projectId);
            }
        }

        // Validate Exit → Enrollment references
        for (Map<String, Object> exit : exits) {
            String enrollmentId = getString(exit, "EnrollmentID");
            if (enrollmentId != null && !enrollmentIds.contains(enrollmentId)) {
                result.addError("Exit ExitID=" + getString(exit, "ExitID"),
                        "EnrollmentID not found in Enrollment.csv: " + enrollmentId);
            }
        }

        // Validate Services → Enrollment references
        for (Map<String, Object> service : services) {
            String enrollmentId = getString(service, "EnrollmentID");
            if (enrollmentId != null && !enrollmentIds.contains(enrollmentId)) {
                result.addError("Service ServicesID=" + getString(service, "ServicesID"),
                        "EnrollmentID not found in Enrollment.csv: " + enrollmentId);
            }
        }
    }

    /**
     * Validate date sequencing (entry before exit, etc.)
     */
    private void validateDateSequencing(
            List<Map<String, Object>> enrollments,
            List<Map<String, Object>> exits,
            ValidationResult result) {

        logger.info("Validating date sequencing");

        // Build enrollment date map
        Map<String, LocalDate> enrollmentDates = new HashMap<>();
        for (Map<String, Object> enrollment : enrollments) {
            String enrollmentId = getString(enrollment, "EnrollmentID");
            LocalDate entryDate = getDate(enrollment, "EntryDate");
            if (enrollmentId != null && entryDate != null) {
                enrollmentDates.put(enrollmentId, entryDate);
            }
        }

        // Validate exit dates
        for (Map<String, Object> exit : exits) {
            String enrollmentId = getString(exit, "EnrollmentID");
            LocalDate exitDate = getDate(exit, "ExitDate");

            if (enrollmentId != null && exitDate != null) {
                LocalDate entryDate = enrollmentDates.get(enrollmentId);
                if (entryDate != null && exitDate.isBefore(entryDate)) {
                    result.addError("Exit ExitID=" + getString(exit, "ExitID"),
                            "ExitDate (" + exitDate + ") before EntryDate (" + entryDate + ")");
                }
            }
        }

        // Validate move-in dates
        for (Map<String, Object> enrollment : enrollments) {
            LocalDate entryDate = getDate(enrollment, "EntryDate");
            LocalDate moveInDate = getDate(enrollment, "MoveInDate");

            if (entryDate != null && moveInDate != null && moveInDate.isBefore(entryDate)) {
                result.addError("Enrollment EnrollmentID=" + getString(enrollment, "EnrollmentID"),
                        "MoveInDate (" + moveInDate + ") before EntryDate (" + entryDate + ")");
            }
        }
    }

    /**
     * Validate completeness rates for required data elements
     */
    private void validateCompletenessRates(
            Map<String, List<Map<String, Object>>> sections,
            ValidationResult result) {

        logger.info("Validating completeness rates");

        List<Map<String, Object>> clients = sections.getOrDefault("Client", Collections.emptyList());
        List<Map<String, Object>> enrollments = sections.getOrDefault("Enrollment", Collections.emptyList());

        if (!clients.isEmpty()) {
            // Universal DEs: Name, SSN, DOB, Race, Gender
            double ssnComplete = calculateCompleteness(clients, "SSN");
            double dobComplete = calculateCompleteness(clients, "DOB");
            double nameComplete = calculateCompletenessEither(clients, "FirstName", "LastName");

            if (ssnComplete < REQUIRED_COMPLETENESS_THRESHOLD) {
                result.addWarning("Client.csv", String.format(
                        "SSN completeness %.1f%% below threshold %.1f%%",
                        ssnComplete * 100, REQUIRED_COMPLETENESS_THRESHOLD * 100));
            }

            if (dobComplete < REQUIRED_COMPLETENESS_THRESHOLD) {
                result.addWarning("Client.csv", String.format(
                        "DOB completeness %.1f%% below threshold %.1f%%",
                        dobComplete * 100, REQUIRED_COMPLETENESS_THRESHOLD * 100));
            }

            if (nameComplete < REQUIRED_COMPLETENESS_THRESHOLD) {
                result.addWarning("Client.csv", String.format(
                        "Name completeness %.1f%% below threshold %.1f%%",
                        nameComplete * 100, REQUIRED_COMPLETENESS_THRESHOLD * 100));
            }
        }

        if (!enrollments.isEmpty()) {
            double relationshipComplete = calculateCompleteness(enrollments, "RelationshipToHoH");
            double disablingComplete = calculateCompleteness(enrollments, "DisablingCondition");

            if (relationshipComplete < REQUIRED_COMPLETENESS_THRESHOLD) {
                result.addWarning("Enrollment.csv", String.format(
                        "RelationshipToHoH completeness %.1f%% below threshold %.1f%%",
                        relationshipComplete * 100, REQUIRED_COMPLETENESS_THRESHOLD * 100));
            }

            if (disablingComplete < REQUIRED_COMPLETENESS_THRESHOLD) {
                result.addWarning("Enrollment.csv", String.format(
                        "DisablingCondition completeness %.1f%% below threshold %.1f%%",
                        disablingComplete * 100, REQUIRED_COMPLETENESS_THRESHOLD * 100));
            }
        }
    }

    // Helper methods

    private Set<String> buildIdSet(List<Map<String, Object>> records, String idField) {
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> record : records) {
            String id = getString(record, idField);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private double calculateCompleteness(List<Map<String, Object>> records, String field) {
        if (records.isEmpty()) return 1.0;

        long nonNullCount = records.stream()
                .filter(r -> r.get(field) != null && !r.get(field).toString().isEmpty())
                .count();

        return (double) nonNullCount / records.size();
    }

    private double calculateCompletenessEither(List<Map<String, Object>> records, String field1, String field2) {
        if (records.isEmpty()) return 1.0;

        long nonNullCount = records.stream()
                .filter(r -> {
                    Object v1 = r.get(field1);
                    Object v2 = r.get(field2);
                    return (v1 != null && !v1.toString().isEmpty()) ||
                            (v2 != null && !v2.toString().isEmpty());
                })
                .count();

        return (double) nonNullCount / records.size();
    }

    private boolean isValidCodeListValue(Integer value, Integer... validValues) {
        return Arrays.asList(validValues).contains(value);
    }

    private boolean isValidDestination(Integer destination) {
        // HUD destination codes (partial list - full list would include all valid codes)
        Set<Integer> validDestinations = Set.of(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
                99, 101, 116, 118, 204, 205, 206, 207, 215, 225, 302, 312, 313, 314,
                327, 329, 332, 410, 411, 421, 422, 423, 426, 435, 436
        );
        return validDestinations.contains(destination);
    }

    private String getString(Map<String, Object> record, String field) {
        Object value = record.get(field);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() == 1;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDate getDate(Map<String, Object> record, String field) {
        Object value = record.get(field);
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final List<ValidationIssue> errors = new ArrayList<>();
        private final List<ValidationIssue> warnings = new ArrayList<>();

        public void addError(String context, String message) {
            errors.add(new ValidationIssue(context, message, IssueType.ERROR));
            logger.error("Validation error [{}]: {}", context, message);
        }

        public void addWarning(String context, String message) {
            warnings.add(new ValidationIssue(context, message, IssueType.WARNING));
            logger.warn("Validation warning [{}]: {}", context, message);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getWarningCount() {
            return warnings.size();
        }

        public List<ValidationIssue> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<ValidationIssue> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public List<ValidationIssue> getAllIssues() {
            List<ValidationIssue> all = new ArrayList<>(errors);
            all.addAll(warnings);
            return all;
        }
    }

    public record ValidationIssue(String context, String message, IssueType type) {
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", type, context, message);
        }
    }

    public enum IssueType {
        ERROR, WARNING
    }
}
