package org.haven.reporting.application.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Centralized CSV validation utilities for HUD HMIS exports.
 *
 * Provides comprehensive validation for:
 * - Date range enforcement with export period boundaries
 * - Nullable field handling per HUD specifications
 * - HUD picklist code validation against 2024 data standards
 * - PII-safe error formatting with sanitized diagnostics
 *
 * All validation failures are reported through structured diagnostic results
 * that exclude PII and support remediation workflows.
 */
public final class CsvValidationUtilities {

    private static final Logger logger = LoggerFactory.getLogger(CsvValidationUtilities.class);

    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // HUD date boundaries
    private static final LocalDate HUD_HMIS_EPOCH = LocalDate.of(1998, 10, 1); // HUD HMIS inception
    private static final int MAX_FUTURE_DAYS = 30; // Allow 30-day future tolerance for data entry lag

    private CsvValidationUtilities() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates a date field against export period and HUD business rules.
     *
     * @param fieldName CSV field name (e.g., "EntryDate")
     * @param value Date value to validate
     * @param exportStartDate Export period start boundary
     * @param exportEndDate Export period end boundary
     * @param rowContext Anonymized row identifier (e.g., "Enrollment row 42")
     * @return Validation result with PII-safe diagnostics
     */
    public static ValidationDiagnostic validateDateInRange(
            String fieldName,
            Object value,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        if (value == null) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "DATE_NULL",
                    String.format("%s is null but required", fieldName)
            );
        }

        LocalDate dateValue = parseDate(value, fieldName, rowContext);
        if (dateValue == null) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "DATE_PARSE_FAILURE",
                    String.format("%s has invalid date format: %s", fieldName, sanitizeValue(value))
            );
        }

        // Check against HMIS epoch
        if (dateValue.isBefore(HUD_HMIS_EPOCH)) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "DATE_BEFORE_HMIS_EPOCH",
                    String.format("%s (%s) precedes HUD HMIS inception date (%s)",
                            fieldName, dateValue, HUD_HMIS_EPOCH)
            );
        }

        // Check against future boundary
        LocalDate maxFutureDate = LocalDate.now().plusDays(MAX_FUTURE_DAYS);
        if (dateValue.isAfter(maxFutureDate)) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "DATE_TOO_FAR_FUTURE",
                    String.format("%s (%s) exceeds allowed future tolerance (%s)",
                            fieldName, dateValue, maxFutureDate)
            );
        }

        // Check against export period boundaries
        if (exportStartDate != null && dateValue.isBefore(exportStartDate)) {
            return ValidationDiagnostic.warning(
                    rowContext,
                    fieldName,
                    "DATE_BEFORE_EXPORT_PERIOD",
                    String.format("%s (%s) precedes export start date (%s)",
                            fieldName, dateValue, exportStartDate)
            );
        }

        if (exportEndDate != null && dateValue.isAfter(exportEndDate)) {
            return ValidationDiagnostic.warning(
                    rowContext,
                    fieldName,
                    "DATE_AFTER_EXPORT_PERIOD",
                    String.format("%s (%s) exceeds export end date (%s)",
                            fieldName, dateValue, exportEndDate)
            );
        }

        return ValidationDiagnostic.success(rowContext, fieldName);
    }

    /**
     * Validates nullable field handling per HUD data quality specifications.
     *
     * @param fieldName CSV field name
     * @param value Field value
     * @param requiredFlag HUD requirement flag: 'R' (Required), 'C' (Conditional), 'O' (Optional)
     * @param rowContext Anonymized row identifier
     * @return Validation result
     */
    public static ValidationDiagnostic validateNullableField(
            String fieldName,
            Object value,
            String requiredFlag,
            String rowContext) {

        boolean isNull = value == null || (value instanceof String && ((String) value).trim().isEmpty());

        if ("R".equals(requiredFlag) && isNull) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "REQUIRED_FIELD_NULL",
                    String.format("%s is required (flag=%s) but null or empty", fieldName, requiredFlag)
            );
        }

        if ("C".equals(requiredFlag) && isNull) {
            return ValidationDiagnostic.warning(
                    rowContext,
                    fieldName,
                    "CONDITIONAL_FIELD_NULL",
                    String.format("%s is conditionally required (flag=%s) but null - verify business rules",
                            fieldName, requiredFlag)
            );
        }

        return ValidationDiagnostic.success(rowContext, fieldName);
    }

    /**
     * Validates HUD picklist code against 2024 data standards.
     *
     * @param fieldName CSV field name (e.g., "RelationshipToHoH")
     * @param value Picklist code value
     * @param validCodes Set of valid codes for this picklist
     * @param picklistName HUD picklist identifier (e.g., "1.27 Relationship to HoH")
     * @param rowContext Anonymized row identifier
     * @return Validation result
     */
    public static ValidationDiagnostic validatePicklistCode(
            String fieldName,
            Object value,
            Set<Integer> validCodes,
            String picklistName,
            String rowContext) {

        if (value == null) {
            // Nullability handled separately via validateNullableField
            return ValidationDiagnostic.success(rowContext, fieldName);
        }

        Integer code = parseInteger(value, fieldName, rowContext);
        if (code == null) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "PICKLIST_PARSE_FAILURE",
                    String.format("%s has non-integer value for picklist %s: %s",
                            fieldName, picklistName, sanitizeValue(value))
            );
        }

        if (!validCodes.contains(code)) {
            return ValidationDiagnostic.error(
                    rowContext,
                    fieldName,
                    "PICKLIST_INVALID_CODE",
                    String.format("%s has invalid code %d for picklist %s (valid: %s)",
                            fieldName, code, picklistName, formatValidCodes(validCodes))
            );
        }

        return ValidationDiagnostic.success(rowContext, fieldName);
    }

    /**
     * Validates date sequencing between two date fields.
     *
     * @param earlierFieldName Name of field that should be earlier
     * @param earlierDate Earlier date value
     * @param laterFieldName Name of field that should be later
     * @param laterDate Later date value
     * @param allowEqual Whether dates can be equal
     * @param rowContext Anonymized row identifier
     * @return Validation result
     */
    public static ValidationDiagnostic validateDateSequence(
            String earlierFieldName,
            LocalDate earlierDate,
            String laterFieldName,
            LocalDate laterDate,
            boolean allowEqual,
            String rowContext) {

        if (earlierDate == null || laterDate == null) {
            // Null dates handled separately
            return ValidationDiagnostic.success(rowContext, earlierFieldName + "→" + laterFieldName);
        }

        if (laterDate.isBefore(earlierDate)) {
            return ValidationDiagnostic.error(
                    rowContext,
                    earlierFieldName + "→" + laterFieldName,
                    "DATE_SEQUENCE_VIOLATION",
                    String.format("%s (%s) must precede %s (%s)",
                            earlierFieldName, earlierDate, laterFieldName, laterDate)
            );
        }

        if (!allowEqual && laterDate.equals(earlierDate)) {
            return ValidationDiagnostic.warning(
                    rowContext,
                    earlierFieldName + "→" + laterFieldName,
                    "DATE_SEQUENCE_EQUAL",
                    String.format("%s (%s) equals %s (%s) - verify if same-day allowed",
                            earlierFieldName, earlierDate, laterFieldName, laterDate)
            );
        }

        return ValidationDiagnostic.success(rowContext, earlierFieldName + "→" + laterFieldName);
    }

    // Helper methods

    private static LocalDate parseDate(Object value, String fieldName, String rowContext) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }

        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }

        if (value instanceof String) {
            try {
                return LocalDate.parse((String) value, ISO_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                logger.debug("Failed to parse date for {} in {}: {}", fieldName, rowContext, value);
                return null;
            }
        }

        return null;
    }

    private static Integer parseInteger(Object value, String fieldName, String rowContext) {
        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse integer for {} in {}: {}", fieldName, rowContext, value);
                return null;
            }
        }

        return null;
    }

    /**
     * Sanitizes value for PII-safe logging.
     * Truncates long strings and masks potential PII patterns.
     */
    private static String sanitizeValue(Object value) {
        if (value == null) {
            return "null";
        }

        String str = value.toString();

        // Mask potential SSN patterns (9 digits)
        str = str.replaceAll("\\d{9}", "[SSN-REDACTED]");

        // Mask potential DOB patterns (dates before 2005)
        str = str.replaceAll("19\\d{2}-\\d{2}-\\d{2}", "[DOB-REDACTED]");
        str = str.replaceAll("20[0-4]\\d-\\d{2}-\\d{2}", "[DOB-REDACTED]");

        // Truncate long values
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }

        return str;
    }

    private static String formatValidCodes(Set<Integer> codes) {
        if (codes.size() > 10) {
            return String.format("%d valid codes", codes.size());
        }

        List<Integer> sorted = new ArrayList<>(codes);
        Collections.sort(sorted);
        return sorted.toString();
    }
}
