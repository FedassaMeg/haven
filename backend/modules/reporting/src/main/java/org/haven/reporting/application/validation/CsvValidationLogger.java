package org.haven.reporting.application.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Structured diagnostic log channel for CSV validation results.
 *
 * Aggregates validation diagnostics across export jobs and provides
 * PII-safe summaries for monitoring, alerting, and remediation workflows.
 *
 * Thread-safe for concurrent validation operations.
 */
public class CsvValidationLogger {

    private static final Logger logger = LoggerFactory.getLogger(CsvValidationLogger.class);

    private final String exportJobId;
    private final Map<String, AtomicInteger> errorCodeCounts = new ConcurrentHashMap<>();
    private final List<ValidationDiagnostic> errorDiagnostics = Collections.synchronizedList(new ArrayList<>());
    private final List<ValidationDiagnostic> warningDiagnostics = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger totalValidations = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger warningCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    public CsvValidationLogger(String exportJobId) {
        this.exportJobId = Objects.requireNonNull(exportJobId, "exportJobId");
        logger.info("Initialized CSV validation logger for export job: {}", exportJobId);
    }

    /**
     * Logs a validation diagnostic result.
     *
     * @param diagnostic Validation result
     */
    public void log(ValidationDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");

        totalValidations.incrementAndGet();

        switch (diagnostic.getSeverity()) {
            case SUCCESS -> {
                successCount.incrementAndGet();
                logger.debug("Validation success: {}", diagnostic.toLogFormat());
            }
            case WARNING -> {
                warningCount.incrementAndGet();
                warningDiagnostics.add(diagnostic);
                logger.warn("Validation warning: {}", diagnostic.toLogFormat());
            }
            case ERROR -> {
                errorCount.incrementAndGet();
                errorDiagnostics.add(diagnostic);

                // Track error code frequency
                String errorCode = diagnostic.getErrorCode();
                errorCodeCounts.computeIfAbsent(errorCode, k -> new AtomicInteger(0)).incrementAndGet();

                logger.error("Validation error: {}", diagnostic.toLogFormat());
            }
        }
    }

    /**
     * Logs multiple diagnostics in batch.
     *
     * @param diagnostics Collection of validation results
     */
    public void logBatch(Collection<ValidationDiagnostic> diagnostics) {
        diagnostics.forEach(this::log);
    }

    /**
     * Checks if validation has any errors.
     *
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return errorCount.get() > 0;
    }

    /**
     * Checks if validation has any warnings.
     *
     * @return true if warnings exist
     */
    public boolean hasWarnings() {
        return warningCount.get() > 0;
    }

    /**
     * Gets total error count.
     */
    public int getErrorCount() {
        return errorCount.get();
    }

    /**
     * Gets total warning count.
     */
    public int getWarningCount() {
        return warningCount.get();
    }

    /**
     * Gets all error diagnostics.
     */
    public List<ValidationDiagnostic> getErrors() {
        return new ArrayList<>(errorDiagnostics);
    }

    /**
     * Gets all warning diagnostics.
     */
    public List<ValidationDiagnostic> getWarnings() {
        return new ArrayList<>(warningDiagnostics);
    }

    /**
     * Generates PII-safe validation summary for export job completion.
     *
     * @return Summary report suitable for logging and monitoring
     */
    public ValidationSummary getSummary() {
        return new ValidationSummary(
                exportJobId,
                totalValidations.get(),
                successCount.get(),
                warningCount.get(),
                errorCount.get(),
                new HashMap<>(errorCodeCounts.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().get()
                        ))),
                getTopErrors(10),
                getTopWarnings(10)
        );
    }

    /**
     * Logs final validation summary.
     */
    public void logSummary() {
        ValidationSummary summary = getSummary();

        logger.info("CSV Validation Summary for Export Job: {}", exportJobId);
        logger.info("  Total Validations: {}", summary.totalValidations());
        logger.info("  Success: {} ({:.1f}%)",
                summary.successCount(),
                summary.successRate() * 100);
        logger.info("  Warnings: {} ({:.1f}%)",
                summary.warningCount(),
                summary.warningRate() * 100);
        logger.info("  Errors: {} ({:.1f}%)",
                summary.errorCount(),
                summary.errorRate() * 100);

        if (!summary.errorCodeFrequency().isEmpty()) {
            logger.info("  Top Error Codes:");
            summary.errorCodeFrequency().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> logger.info("    {} - {} occurrences",
                            entry.getKey(), entry.getValue()));
        }

        if (summary.hasErrors()) {
            logger.error("CSV validation FAILED for export job {} with {} errors",
                    exportJobId, summary.errorCount());
        } else if (summary.hasWarnings()) {
            logger.warn("CSV validation PASSED with {} warnings for export job {}",
                    summary.warningCount(), exportJobId);
        } else {
            logger.info("CSV validation PASSED for export job {}", exportJobId);
        }
    }

    /**
     * Exports validation summary as JSON for monitoring dashboards.
     *
     * @return JSON-formatted summary
     */
    public String getSummaryJson() {
        ValidationSummary summary = getSummary();

        StringBuilder json = new StringBuilder("{");
        json.append("\"exportJobId\":\"").append(escapeJson(exportJobId)).append("\",");
        json.append("\"totalValidations\":").append(summary.totalValidations()).append(",");
        json.append("\"successCount\":").append(summary.successCount()).append(",");
        json.append("\"warningCount\":").append(summary.warningCount()).append(",");
        json.append("\"errorCount\":").append(summary.errorCount()).append(",");
        json.append("\"successRate\":").append(summary.successRate()).append(",");
        json.append("\"warningRate\":").append(summary.warningRate()).append(",");
        json.append("\"errorRate\":").append(summary.errorRate()).append(",");
        json.append("\"passed\":").append(!summary.hasErrors()).append(",");

        // Error code frequency
        json.append("\"errorCodeFrequency\":{");
        String errorCodes = summary.errorCodeFrequency().entrySet().stream()
                .map(e -> "\"" + escapeJson(e.getKey()) + "\":" + e.getValue())
                .collect(Collectors.joining(","));
        json.append(errorCodes);
        json.append("},");

        // Top errors (PII-safe)
        json.append("\"topErrors\":[");
        String topErrors = summary.topErrors().stream()
                .map(ValidationDiagnostic::toJson)
                .collect(Collectors.joining(","));
        json.append(topErrors);
        json.append("],");

        // Top warnings (PII-safe)
        json.append("\"topWarnings\":[");
        String topWarnings = summary.topWarnings().stream()
                .map(ValidationDiagnostic::toJson)
                .collect(Collectors.joining(","));
        json.append(topWarnings);
        json.append("]");

        json.append("}");

        return json.toString();
    }

    private List<ValidationDiagnostic> getTopErrors(int limit) {
        return errorDiagnostics.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> getTopWarnings(int limit) {
        return warningDiagnostics.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Validation summary record for export job.
     */
    public record ValidationSummary(
            String exportJobId,
            int totalValidations,
            int successCount,
            int warningCount,
            int errorCount,
            Map<String, Integer> errorCodeFrequency,
            List<ValidationDiagnostic> topErrors,
            List<ValidationDiagnostic> topWarnings
    ) {
        public double successRate() {
            return totalValidations > 0 ? (double) successCount / totalValidations : 0.0;
        }

        public double warningRate() {
            return totalValidations > 0 ? (double) warningCount / totalValidations : 0.0;
        }

        public double errorRate() {
            return totalValidations > 0 ? (double) errorCount / totalValidations : 0.0;
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasWarnings() {
            return warningCount > 0;
        }
    }
}
