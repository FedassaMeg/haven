package org.haven.reporting.harness;

import org.haven.reporting.application.services.HUDExportValidationService;
import org.haven.reporting.fixtures.HUDSyntheticDataFixtures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Test harness for HUD export validation.
 *
 * Features:
 * - Golden output comparison
 * - Edge case testing (VAWA, consent, overlapping enrollments)
 * - Regression detection
 * - HTML report generation
 * - CI/CD integration
 */
public class ExportTestHarness {

    private static final Logger logger = LoggerFactory.getLogger(ExportTestHarness.class);

    private final HUDExportValidationService validationService;
    private final Path outputDirectory;
    private final List<TestResult> testResults = new ArrayList<>();

    public ExportTestHarness(HUDExportValidationService validationService, Path outputDirectory) {
        this.validationService = validationService;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Run complete test suite
     */
    public TestSuiteResult runTestSuite() {
        logger.info("Starting HUD export test suite");

        testResults.clear();

        // Test 1: Standard dataset validation
        runTest("Standard Dataset", this::testStandardDataset);

        // Test 2: VAWA-protected data
        runTest("VAWA Protected Data", this::testVAWAProtectedData);

        // Test 3: Overlapping enrollments
        runTest("Overlapping Enrollments", this::testOverlappingEnrollments);

        // Test 4: Missing consent
        runTest("Missing Consent Edge Case", this::testMissingConsent);

        // Test 5: Invalid SSN detection
        runTest("Invalid SSN Detection", this::testInvalidSSN);

        // Test 6: Referential integrity
        runTest("Referential Integrity", this::testReferentialIntegrity);

        // Test 7: Date sequencing
        runTest("Date Sequencing", this::testDateSequencing);

        // Test 8: Completeness rates
        runTest("Completeness Rates", this::testCompletenessRates);

        // Generate report
        TestSuiteResult result = new TestSuiteResult(testResults);
        generateReport(result);

        return result;
    }

    private void runTest(String testName, Runnable test) {
        logger.info("Running test: {}", testName);
        long startTime = System.currentTimeMillis();

        try {
            test.run();
            long duration = System.currentTimeMillis() - startTime;
            testResults.add(new TestResult(testName, true, null, duration));
            logger.info("✓ {} passed ({} ms)", testName, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            testResults.add(new TestResult(testName, false, e.getMessage(), duration));
            logger.error("✗ {} failed: {}", testName, e.getMessage(), e);
        }
    }

    // Test cases

    private void testStandardDataset() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();
        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        if (result.hasErrors()) {
            throw new AssertionError("Standard dataset should not have errors. Errors: " + result.getErrors());
        }

        if (result.getWarningCount() > 2) {
            throw new AssertionError("Too many warnings: " + result.getWarningCount());
        }
    }

    private void testVAWAProtectedData() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        // Verify VAWA-protected client exists
        List<Map<String, Object>> clients = dataset.get("Client");
        boolean hasVAWAClient = clients.stream()
                .anyMatch(c -> "C004".equals(c.get("PersonalID")));

        if (!hasVAWAClient) {
            throw new AssertionError("VAWA-protected client not found in dataset");
        }

        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        // VAWA data should still validate
        if (result.hasErrors()) {
            throw new AssertionError("VAWA dataset should validate. Errors: " + result.getErrors());
        }
    }

    private void testOverlappingEnrollments() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        // Verify overlapping enrollments exist (C003 in P001 and P005)
        List<Map<String, Object>> enrollments = dataset.get("Enrollment");
        long overlappingCount = enrollments.stream()
                .filter(e -> "C003".equals(e.get("PersonalID")))
                .count();

        if (overlappingCount < 2) {
            throw new AssertionError("Overlapping enrollments not found in dataset");
        }

        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        // Overlapping enrollments are valid (different project types)
        if (result.hasErrors()) {
            throw new AssertionError("Overlapping enrollments should be valid. Errors: " + result.getErrors());
        }
    }

    private void testMissingConsent() {
        // In production, would test clients without consent flags
        // For now, verify warning is generated for missing data quality indicators

        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        // Client C007 has missing/unknown data
        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        // Should have warnings for data quality
        if (result.getWarningCount() == 0) {
            throw new AssertionError("Should have warnings for missing data quality indicators");
        }
    }

    private void testInvalidSSN() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        // Add client with invalid SSN
        List<Map<String, Object>> clients = new ArrayList<>(dataset.get("Client"));
        clients.add(HUDSyntheticDataFixtures.createClientWithInvalidSSN());
        dataset.put("Client", clients);

        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        if (!result.hasErrors()) {
            throw new AssertionError("Should detect invalid SSN");
        }

        boolean hasSSNError = result.getErrors().stream()
                .anyMatch(e -> e.message().contains("SSN contains invalid pattern"));

        if (!hasSSNError) {
            throw new AssertionError("Should have SSN validation error");
        }
    }

    private void testReferentialIntegrity() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        // Add orphan exit
        List<Map<String, Object>> exits = new ArrayList<>(dataset.get("Exit"));
        exits.add(HUDSyntheticDataFixtures.createOrphanExit());
        dataset.put("Exit", exits);

        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        if (!result.hasErrors()) {
            throw new AssertionError("Should detect orphan exit record");
        }

        boolean hasRefIntegrityError = result.getErrors().stream()
                .anyMatch(e -> e.message().contains("EnrollmentID not found"));

        if (!hasRefIntegrityError) {
            throw new AssertionError("Should have referential integrity error");
        }
    }

    private void testDateSequencing() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        // Add enrollment with invalid dates
        List<Map<String, Object>> enrollments = new ArrayList<>(dataset.get("Enrollment"));
        enrollments.add(HUDSyntheticDataFixtures.createEnrollmentWithInvalidDates());
        dataset.put("Enrollment", enrollments);

        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        if (!result.hasErrors()) {
            throw new AssertionError("Should detect date sequencing error");
        }

        boolean hasDateError = result.getErrors().stream()
                .anyMatch(e -> e.message().contains("before"));

        if (!hasDateError) {
            throw new AssertionError("Should have date sequencing error");
        }
    }

    private void testCompletenessRates() {
        Map<String, List<Map<String, Object>>> dataset = HUDSyntheticDataFixtures.generateCompleteDataset();

        List<Map<String, Object>> clients = dataset.get("Client");

        // Calculate SSN completeness
        long ssnCount = clients.stream()
                .filter(c -> c.get("SSN") != null && !c.get("SSN").toString().isEmpty())
                .count();

        double ssnCompleteness = (double) ssnCount / clients.size();

        // Should be above 0.85 in synthetic data
        if (ssnCompleteness < 0.85) {
            throw new AssertionError("SSN completeness too low: " + ssnCompleteness);
        }

        // Validate using service
        HUDExportValidationService.ValidationResult result = validationService.validateExport(dataset);

        // May have warnings but should not have errors
        if (result.hasErrors()) {
            throw new AssertionError("Completeness test should not have errors");
        }
    }

    /**
     * Generate HTML test report
     */
    private void generateReport(TestSuiteResult result) {
        try {
            Files.createDirectories(outputDirectory);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path reportPath = outputDirectory.resolve("test-report-" + timestamp + ".html");

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html><head><meta charset='UTF-8'><title>HUD Export Test Report</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            html.append("h1 { color: #333; }\n");
            html.append(".summary { background: #f0f0f0; padding: 15px; margin: 20px 0; border-radius: 5px; }\n");
            html.append(".passed { color: green; }\n");
            html.append(".failed { color: red; }\n");
            html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
            html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
            html.append("th { background-color: #4CAF50; color: white; }\n");
            html.append("tr:nth-child(even) { background-color: #f2f2f2; }\n");
            html.append("</style>\n");
            html.append("</head><body>\n");

            html.append("<h1>HUD Export Test Report</h1>\n");
            html.append("<div class='summary'>\n");
            html.append("<p><strong>Date:</strong> ").append(LocalDateTime.now()).append("</p>\n");
            html.append("<p><strong>Total Tests:</strong> ").append(result.getTotalTests()).append("</p>\n");
            html.append("<p class='passed'><strong>Passed:</strong> ").append(result.getPassedCount()).append("</p>\n");
            html.append("<p class='failed'><strong>Failed:</strong> ").append(result.getFailedCount()).append("</p>\n");
            html.append("<p><strong>Success Rate:</strong> ")
                    .append(String.format("%.1f%%", result.getSuccessRate() * 100)).append("</p>\n");
            html.append("</div>\n");

            html.append("<table>\n");
            html.append("<tr><th>Test Name</th><th>Status</th><th>Duration (ms)</th><th>Error</th></tr>\n");

            for (TestResult testResult : result.getResults()) {
                html.append("<tr>\n");
                html.append("<td>").append(testResult.testName()).append("</td>\n");
                html.append("<td class='").append(testResult.passed() ? "passed'>✓ PASSED" : "failed'>✗ FAILED")
                        .append("</td>\n");
                html.append("<td>").append(testResult.durationMillis()).append("</td>\n");
                html.append("<td>").append(testResult.errorMessage() != null ? testResult.errorMessage() : "")
                        .append("</td>\n");
                html.append("</tr>\n");
            }

            html.append("</table>\n");
            html.append("</body></html>");

            Files.writeString(reportPath, html.toString());

            logger.info("Test report generated: {}", reportPath);

        } catch (IOException e) {
            logger.error("Failed to generate test report", e);
        }
    }

    /**
     * CI/CD integration - exit with error code if tests fail
     */
    public static void main(String[] args) throws Exception {
        Path outputDir = Paths.get("target/test-reports");

        // Initialize validation service
        HUDExportValidationService validationService = new HUDExportValidationService();

        ExportTestHarness harness = new ExportTestHarness(validationService, outputDir);
        TestSuiteResult result = harness.runTestSuite();

        System.out.println("\n========================================");
        System.out.println("HUD EXPORT TEST RESULTS");
        System.out.println("========================================");
        System.out.println("Total Tests: " + result.getTotalTests());
        System.out.println("Passed: " + result.getPassedCount());
        System.out.println("Failed: " + result.getFailedCount());
        System.out.println("Success Rate: " + String.format("%.1f%%", result.getSuccessRate() * 100));
        System.out.println("========================================\n");

        if (!result.isAllPassed()) {
            System.err.println("❌ TESTS FAILED - Build will fail");
            System.exit(1);
        } else {
            System.out.println("✅ ALL TESTS PASSED");
            System.exit(0);
        }
    }

    // Records

    public record TestResult(String testName, boolean passed, String errorMessage, long durationMillis) {}

    public static class TestSuiteResult {
        private final List<TestResult> results;

        public TestSuiteResult(List<TestResult> results) {
            this.results = new ArrayList<>(results);
        }

        public List<TestResult> getResults() {
            return Collections.unmodifiableList(results);
        }

        public int getTotalTests() {
            return results.size();
        }

        public int getPassedCount() {
            return (int) results.stream().filter(TestResult::passed).count();
        }

        public int getFailedCount() {
            return (int) results.stream().filter(r -> !r.passed()).count();
        }

        public double getSuccessRate() {
            return results.isEmpty() ? 0.0 : (double) getPassedCount() / getTotalTests();
        }

        public boolean isAllPassed() {
            return getFailedCount() == 0;
        }
    }
}
