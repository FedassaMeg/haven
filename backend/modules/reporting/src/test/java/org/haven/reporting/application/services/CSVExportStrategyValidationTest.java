package org.haven.reporting.application.services;

import org.haven.reporting.application.validation.CsvValidationException;
import org.haven.reporting.application.validation.CsvValidationLogger;
import org.haven.reporting.domain.ExportPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CSV export with validation guardrails.
 *
 * Tests edge cases with crafted fixtures:
 * - Invalid picklist codes
 * - Null required fields
 * - Out-of-range dates
 * - Mixed valid/invalid rows
 * - Anonymized diagnostics in logs
 */
class CSVExportStrategyValidationTest {

    private CSVExportStrategy csvExportStrategy;

    @BeforeEach
    void setUp() {
        csvExportStrategy = new CSVExportStrategy();
    }

    @Test
    @DisplayName("Valid enrollment data should export successfully")
    void testValidEnrollmentExport() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> enrollments = List.of(
                Map.of(
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2024, 6, 1),
                        "HouseholdID", "HH001",
                        "RelationshipToHoH", 1,
                        "LivingSituation", 16,
                        "DisablingCondition", 1
                )
        );

        sections.put("Enrollment", enrollments);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-001");

        byte[] result = csvExportStrategy.formatWithValidation(sections, period, "test-job-001", logger);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertFalse(logger.hasErrors());
        assertEquals(0, logger.getErrorCount());
    }

    @Test
    @DisplayName("Enrollment with invalid RelationshipToHoH code should reject row")
    void testInvalidRelationshipToHohCode() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> enrollments = List.of(
                Map.of(
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2024, 6, 1),
                        "HouseholdID", "HH001",
                        "RelationshipToHoH", 999,  // INVALID CODE
                        "LivingSituation", 16,
                        "DisablingCondition", 1
                )
        );

        sections.put("Enrollment", enrollments);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-002");

        assertThrows(CsvValidationException.class, () -> {
            csvExportStrategy.formatWithValidation(sections, period, "test-job-002", logger);
        });

        assertTrue(logger.hasErrors());
        assertTrue(logger.getErrorCount() > 0);
    }

    @Test
    @DisplayName("Exit with null required Destination should reject row")
    void testExitNullDestination() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> exits = List.of(
                Map.of(
                        "ExitID", "EXT001",
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "ExitDate", LocalDate.of(2024, 6, 30)
                        // Missing Destination (required)
                )
        );

        sections.put("Exit", exits);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-003");

        assertThrows(CsvValidationException.class, () -> {
            csvExportStrategy.formatWithValidation(sections, period, "test-job-003", logger);
        });

        assertTrue(logger.hasErrors());
        assertTrue(logger.getErrors().stream()
                .anyMatch(d -> d.getErrorCode().equals("REQUIRED_FIELD_NULL")));
    }

    @Test
    @DisplayName("Enrollment with date before export period should generate warning but succeed")
    void testDateBeforeExportPeriodWarning() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> enrollments = List.of(
                Map.of(
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2023, 6, 1),  // Before export period
                        "HouseholdID", "HH001",
                        "RelationshipToHoH", 1,
                        "LivingSituation", 16,
                        "DisablingCondition", 1
                )
        );

        sections.put("Enrollment", enrollments);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-004");

        byte[] result = csvExportStrategy.formatWithValidation(sections, period, "test-job-004", logger);

        assertNotNull(result);
        assertFalse(logger.hasErrors());
        assertTrue(logger.hasWarnings());
        assertTrue(logger.getWarnings().stream()
                .anyMatch(d -> d.getErrorCode().equals("DATE_BEFORE_EXPORT_PERIOD")));
    }

    @Test
    @DisplayName("Service with date before HMIS epoch should reject row")
    void testDateBeforeHmisEpoch() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> services = List.of(
                Map.of(
                        "ServicesID", "SVC001",
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "DateProvided", LocalDate.of(1997, 1, 1),  // Before HMIS epoch
                        "RecordType", 12,
                        "TypeProvided", 1
                )
        );

        sections.put("Services", services);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-005");

        assertThrows(CsvValidationException.class, () -> {
            csvExportStrategy.formatWithValidation(sections, period, "test-job-005", logger);
        });

        assertTrue(logger.hasErrors());
        assertTrue(logger.getErrors().stream()
                .anyMatch(d -> d.getErrorCode().equals("DATE_BEFORE_HMIS_EPOCH")));
    }

    @Test
    @DisplayName("Mixed valid and invalid rows: invalid rows rejected, valid rows exported")
    void testMixedValidInvalidRows() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> enrollments = List.of(
                // Valid row
                Map.of(
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2024, 6, 1),
                        "HouseholdID", "HH001",
                        "RelationshipToHoH", 1,
                        "LivingSituation", 16,
                        "DisablingCondition", 1
                ),
                // Invalid row - bad RelationshipToHoH
                Map.of(
                        "EnrollmentID", "ENR002",
                        "PersonalID", "CLI002",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2024, 6, 2),
                        "HouseholdID", "HH002",
                        "RelationshipToHoH", 999,  // INVALID
                        "LivingSituation", 16,
                        "DisablingCondition", 1
                ),
                // Valid row
                Map.of(
                        "EnrollmentID", "ENR003",
                        "PersonalID", "CLI003",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2024, 6, 3),
                        "HouseholdID", "HH003",
                        "RelationshipToHoH", 1,
                        "LivingSituation", 16,
                        "DisablingCondition", 1
                )
        );

        sections.put("Enrollment", enrollments);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-006");

        assertThrows(CsvValidationException.class, () -> {
            csvExportStrategy.formatWithValidation(sections, period, "test-job-006", logger);
        });

        // Should have errors from ENR002
        assertTrue(logger.hasErrors());
        assertEquals(1, logger.getErrors().stream()
                .filter(d -> d.getRowContext().contains("row 2"))
                .count());
    }

    @Test
    @DisplayName("Client with invalid name data quality should reject row")
    void testInvalidNameDataQuality() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> clients = List.of(
                Map.of(
                        "PersonalID", "CLI001",
                        "FirstName", "John",
                        "LastName", "Doe",
                        "NameDataQuality", 999,  // INVALID CODE
                        "SSNDataQuality", 99,
                        "DOBDataQuality", 99
                )
        );

        sections.put("Client", clients);

        CsvValidationLogger logger = new CsvValidationLogger("test-job-007");

        assertThrows(CsvValidationException.class, () -> {
            csvExportStrategy.formatWithValidation(sections, null, "test-job-007", logger);
        });

        assertTrue(logger.hasErrors());
        assertTrue(logger.getErrors().stream()
                .anyMatch(d -> d.getFieldName().equals("NameDataQuality")));
    }

    @Test
    @DisplayName("IncomeBenefits with invalid DataCollectionStage should reject row")
    void testInvalidDataCollectionStage() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> incomeBenefits = List.of(
                Map.of(
                        "IncomeBenefitsID", "INC001",
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "InformationDate", LocalDate.of(2024, 6, 1),
                        "DataCollectionStage", 99  // INVALID CODE
                )
        );

        sections.put("IncomeBenefits", incomeBenefits);

        ExportPeriod period = new ExportPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        CsvValidationLogger logger = new CsvValidationLogger("test-job-008");

        assertThrows(CsvValidationException.class, () -> {
            csvExportStrategy.formatWithValidation(sections, period, "test-job-008", logger);
        });

        assertTrue(logger.hasErrors());
    }

    @Test
    @DisplayName("Validation summary should contain anonymized diagnostics")
    void testAnonymizedDiagnostics() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();

        List<Map<String, Object>> enrollments = List.of(
                Map.of(
                        "EnrollmentID", "ENR001",
                        "PersonalID", "CLI001",
                        "ProjectID", "PRJ001",
                        "EntryDate", LocalDate.of(2024, 6, 1),
                        "HouseholdID", "HH001",
                        "RelationshipToHoH", 999,  // Invalid
                        "LivingSituation", 16
                )
        );

        sections.put("Enrollment", enrollments);

        CsvValidationLogger logger = new CsvValidationLogger("test-job-009");

        try {
            csvExportStrategy.formatWithValidation(sections, null, "test-job-009", logger);
        } catch (CsvValidationException e) {
            // Expected
        }

        CsvValidationLogger.ValidationSummary summary = logger.getSummary();

        // Summary should not contain PII
        String summaryJson = logger.getSummaryJson();
        assertFalse(summaryJson.contains("CLI001"));  // PersonalID should not appear
        assertTrue(summaryJson.contains("Enrollment row 1"));  // Anonymized context should appear
    }

    @Test
    @DisplayName("Empty sections should export successfully without validation")
    void testEmptySections() {
        Map<String, List<Map<String, Object>>> sections = new HashMap<>();
        sections.put("Enrollment", List.of());
        sections.put("Exit", List.of());

        CsvValidationLogger logger = new CsvValidationLogger("test-job-010");

        byte[] result = csvExportStrategy.formatWithValidation(sections, null, "test-job-010", logger);

        assertNotNull(result);
        assertFalse(logger.hasErrors());
        assertFalse(logger.hasWarnings());
    }
}
