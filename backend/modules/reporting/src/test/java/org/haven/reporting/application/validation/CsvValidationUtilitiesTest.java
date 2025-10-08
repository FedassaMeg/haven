package org.haven.reporting.application.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CSV validation utilities with edge-case fixtures.
 *
 * Tests cover:
 * - Invalid picklist codes
 * - Null value handling
 * - Out-of-range dates
 * - Anonymized error diagnostics (no PII in logs)
 */
class CsvValidationUtilitiesTest {

    @Test
    @DisplayName("validateDateInRange: null date with required field should return error")
    void testNullDateRequired() {
        LocalDate exportStart = LocalDate.of(2024, 1, 1);
        LocalDate exportEnd = LocalDate.of(2024, 12, 31);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "EntryDate",
                null,
                exportStart,
                exportEnd,
                "Enrollment row 1"
        );

        assertTrue(result.isError());
        assertEquals("DATE_NULL", result.getErrorCode());
        assertFalse(result.getMessage().contains("SSN"));
        assertFalse(result.getMessage().contains("DOB"));
    }

    @Test
    @DisplayName("validateDateInRange: date before HMIS epoch should return error")
    void testDateBeforeHmisEpoch() {
        LocalDate tooEarly = LocalDate.of(1997, 1, 1);
        LocalDate exportStart = LocalDate.of(2024, 1, 1);
        LocalDate exportEnd = LocalDate.of(2024, 12, 31);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "EntryDate",
                tooEarly,
                exportStart,
                exportEnd,
                "Enrollment row 1"
        );

        assertTrue(result.isError());
        assertEquals("DATE_BEFORE_HMIS_EPOCH", result.getErrorCode());
        assertTrue(result.getMessage().contains("1998-10-01"));
    }

    @Test
    @DisplayName("validateDateInRange: date too far in future should return error")
    void testDateTooFarFuture() {
        LocalDate tooFuture = LocalDate.now().plusDays(60);
        LocalDate exportStart = LocalDate.of(2024, 1, 1);
        LocalDate exportEnd = LocalDate.of(2024, 12, 31);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "ExitDate",
                tooFuture,
                exportStart,
                exportEnd,
                "Exit row 1"
        );

        assertTrue(result.isError());
        assertEquals("DATE_TOO_FAR_FUTURE", result.getErrorCode());
    }

    @Test
    @DisplayName("validateDateInRange: date before export period should return warning")
    void testDateBeforeExportPeriod() {
        LocalDate beforePeriod = LocalDate.of(2023, 6, 1);
        LocalDate exportStart = LocalDate.of(2024, 1, 1);
        LocalDate exportEnd = LocalDate.of(2024, 12, 31);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "EntryDate",
                beforePeriod,
                exportStart,
                exportEnd,
                "Enrollment row 1"
        );

        assertTrue(result.isWarning());
        assertEquals("DATE_BEFORE_EXPORT_PERIOD", result.getErrorCode());
    }

    @Test
    @DisplayName("validateDateInRange: valid date within export period should succeed")
    void testValidDateInPeriod() {
        LocalDate validDate = LocalDate.of(2024, 6, 15);
        LocalDate exportStart = LocalDate.of(2024, 1, 1);
        LocalDate exportEnd = LocalDate.of(2024, 12, 31);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "EntryDate",
                validDate,
                exportStart,
                exportEnd,
                "Enrollment row 1"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("validateDateInRange: invalid date format string should return error")
    void testInvalidDateFormat() {
        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "EntryDate",
                "not-a-date",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "Enrollment row 1"
        );

        assertTrue(result.isError());
        assertEquals("DATE_PARSE_FAILURE", result.getErrorCode());
    }

    @Test
    @DisplayName("validateNullableField: required field null should return error")
    void testRequiredFieldNull() {
        ValidationDiagnostic result = CsvValidationUtilities.validateNullableField(
                "RelationshipToHoH",
                null,
                "R",
                "Enrollment row 1"
        );

        assertTrue(result.isError());
        assertEquals("REQUIRED_FIELD_NULL", result.getErrorCode());
        assertTrue(result.getMessage().contains("RelationshipToHoH"));
    }

    @Test
    @DisplayName("validateNullableField: required field empty string should return error")
    void testRequiredFieldEmptyString() {
        ValidationDiagnostic result = CsvValidationUtilities.validateNullableField(
                "FirstName",
                "   ",
                "R",
                "Client row 1"
        );

        assertTrue(result.isError());
        assertEquals("REQUIRED_FIELD_NULL", result.getErrorCode());
    }

    @Test
    @DisplayName("validateNullableField: conditional field null should return warning")
    void testConditionalFieldNull() {
        ValidationDiagnostic result = CsvValidationUtilities.validateNullableField(
                "MoveInDate",
                null,
                "C",
                "Enrollment row 1"
        );

        assertTrue(result.isWarning());
        assertEquals("CONDITIONAL_FIELD_NULL", result.getErrorCode());
    }

    @Test
    @DisplayName("validateNullableField: optional field null should succeed")
    void testOptionalFieldNull() {
        ValidationDiagnostic result = CsvValidationUtilities.validateNullableField(
                "MiddleName",
                null,
                "O",
                "Client row 1"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("validatePicklistCode: invalid code should return error")
    void testInvalidPicklistCode() {
        ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
                "RelationshipToHoH",
                999,
                HudPicklistCodes.RELATIONSHIP_TO_HOH,
                "1.27 Relationship to HoH",
                "Enrollment row 1"
        );

        assertTrue(result.isError());
        assertEquals("PICKLIST_INVALID_CODE", result.getErrorCode());
        assertTrue(result.getMessage().contains("999"));
        assertTrue(result.getMessage().contains("1.27 Relationship to HoH"));
    }

    @Test
    @DisplayName("validatePicklistCode: valid code should succeed")
    void testValidPicklistCode() {
        ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
                "RelationshipToHoH",
                1,
                HudPicklistCodes.RELATIONSHIP_TO_HOH,
                "1.27 Relationship to HoH",
                "Enrollment row 1"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("validatePicklistCode: non-integer value should return error")
    void testPicklistNonIntegerValue() {
        ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
                "Destination",
                "invalid",
                HudPicklistCodes.DESTINATION,
                "3.12 Destination",
                "Exit row 1"
        );

        assertTrue(result.isError());
        assertEquals("PICKLIST_PARSE_FAILURE", result.getErrorCode());
    }

    @Test
    @DisplayName("validatePicklistCode: null value should succeed (handled separately)")
    void testPicklistNullValue() {
        ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
                "DisablingCondition",
                null,
                HudPicklistCodes.DISABLING_CONDITION,
                "1.7 Disabling Condition",
                "Enrollment row 1"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("validateDateSequence: exit before entry should return error")
    void testExitBeforeEntry() {
        LocalDate entryDate = LocalDate.of(2024, 6, 15);
        LocalDate exitDate = LocalDate.of(2024, 6, 10);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateSequence(
                "EntryDate",
                entryDate,
                "ExitDate",
                exitDate,
                false,
                "Exit row 1"
        );

        assertTrue(result.isError());
        assertEquals("DATE_SEQUENCE_VIOLATION", result.getErrorCode());
    }

    @Test
    @DisplayName("validateDateSequence: same-day entry and exit with allowEqual=false should warn")
    void testSameDayEntryExitNotAllowed() {
        LocalDate date = LocalDate.of(2024, 6, 15);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateSequence(
                "EntryDate",
                date,
                "ExitDate",
                date,
                false,
                "Exit row 1"
        );

        assertTrue(result.isWarning());
        assertEquals("DATE_SEQUENCE_EQUAL", result.getErrorCode());
    }

    @Test
    @DisplayName("validateDateSequence: valid sequence should succeed")
    void testValidDateSequence() {
        LocalDate entryDate = LocalDate.of(2024, 6, 1);
        LocalDate exitDate = LocalDate.of(2024, 6, 30);

        ValidationDiagnostic result = CsvValidationUtilities.validateDateSequence(
                "EntryDate",
                entryDate,
                "ExitDate",
                exitDate,
                true,
                "Exit row 1"
        );

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("PII sanitization: SSN pattern should be redacted in diagnostics")
    void testPiiSanitizationSsn() {
        ValidationDiagnostic result = CsvValidationUtilities.validateDateInRange(
                "EntryDate",
                "123456789",  // Looks like SSN
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "Enrollment row 1"
        );

        assertTrue(result.isError());
        assertFalse(result.getMessage().contains("123456789"));
        assertTrue(result.getMessage().contains("[SSN-REDACTED]"));
    }

    @Test
    @DisplayName("PII sanitization: DOB pattern should be redacted in diagnostics")
    void testPiiSanitizationDob() {
        ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
                "Destination",
                "1985-03-15",  // Looks like DOB
                HudPicklistCodes.DESTINATION,
                "3.12 Destination",
                "Exit row 1"
        );

        assertTrue(result.isError());
        assertFalse(result.getMessage().contains("1985-03-15"));
        assertTrue(result.getMessage().contains("[DOB-REDACTED]"));
    }

    @Test
    @DisplayName("Edge case: very large picklist (Destination) should format code count")
    void testLargePicklistFormatting() {
        ValidationDiagnostic result = CsvValidationUtilities.validatePicklistCode(
                "Destination",
                9999,
                HudPicklistCodes.DESTINATION,
                "3.12 Destination",
                "Exit row 1"
        );

        assertTrue(result.isError());
        // Should show "N valid codes" instead of listing all
        assertTrue(result.getMessage().contains("valid codes") || result.getMessage().contains("9999"));
    }

    @Test
    @DisplayName("HudPicklistCodes: boundary values for RelationshipToHoH")
    void testRelationshipToHohBoundaries() {
        assertTrue(HudPicklistCodes.isValidCode("RELATIONSHIP_TO_HOH", 1));
        assertTrue(HudPicklistCodes.isValidCode("RELATIONSHIP_TO_HOH", 5));
        assertFalse(HudPicklistCodes.isValidCode("RELATIONSHIP_TO_HOH", 0));
        assertFalse(HudPicklistCodes.isValidCode("RELATIONSHIP_TO_HOH", 6));
    }

    @Test
    @DisplayName("HudPicklistCodes: five-point response includes all standard values")
    void testFivePointResponse() {
        Set<Integer> codes = HudPicklistCodes.FIVE_POINT_RESPONSE;
        assertTrue(codes.contains(0));  // No
        assertTrue(codes.contains(1));  // Yes
        assertTrue(codes.contains(8));  // Client doesn't know
        assertTrue(codes.contains(9));  // Client refused
        assertTrue(codes.contains(99)); // Data not collected
        assertEquals(5, codes.size());
    }

    @Test
    @DisplayName("HudPicklistCodes: destination includes all HUD 2024 codes")
    void testDestinationCompleteness() {
        Set<Integer> codes = HudPicklistCodes.DESTINATION;
        assertTrue(codes.contains(1));   // Emergency shelter
        assertTrue(codes.contains(24));  // Deceased
        assertTrue(codes.contains(99));  // Data not collected
        assertTrue(codes.contains(435)); // VASH
        assertTrue(codes.contains(436)); // Deceased (alternate)
        assertTrue(codes.size() > 50);   // Should have many destination codes
    }
}
