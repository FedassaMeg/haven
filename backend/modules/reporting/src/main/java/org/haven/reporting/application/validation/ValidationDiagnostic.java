package org.haven.reporting.application.validation;

import java.time.Instant;
import java.util.Objects;

/**
 * PII-safe validation diagnostic result.
 *
 * Captures validation outcomes with sanitized context suitable for
 * structured logging, monitoring dashboards, and remediation workflows.
 *
 * All diagnostic messages are designed to exclude PII (SSN, DOB, names)
 * while providing sufficient detail for data quality remediation.
 */
public final class ValidationDiagnostic {

    private final String rowContext;      // Anonymized identifier (e.g., "Enrollment row 42")
    private final String fieldName;       // CSV field name
    private final Severity severity;      // ERROR, WARNING, SUCCESS
    private final String errorCode;       // Machine-readable code (e.g., "DATE_NULL")
    private final String message;         // Human-readable message (PII-safe)
    private final Instant timestamp;      // When validation occurred

    private ValidationDiagnostic(
            String rowContext,
            String fieldName,
            Severity severity,
            String errorCode,
            String message) {

        this.rowContext = Objects.requireNonNull(rowContext, "rowContext");
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.errorCode = errorCode;  // Null for SUCCESS
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static ValidationDiagnostic error(
            String rowContext,
            String fieldName,
            String errorCode,
            String message) {
        return new ValidationDiagnostic(rowContext, fieldName, Severity.ERROR, errorCode, message);
    }

    public static ValidationDiagnostic warning(
            String rowContext,
            String fieldName,
            String errorCode,
            String message) {
        return new ValidationDiagnostic(rowContext, fieldName, Severity.WARNING, errorCode, message);
    }

    public static ValidationDiagnostic success(String rowContext, String fieldName) {
        return new ValidationDiagnostic(rowContext, fieldName, Severity.SUCCESS, null, null);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    public boolean isSuccess() {
        return severity == Severity.SUCCESS;
    }

    public String getRowContext() {
        return rowContext;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Formats diagnostic for structured log output.
     * Format: [SEVERITY] rowContext | fieldName | errorCode | message
     */
    public String toLogFormat() {
        if (severity == Severity.SUCCESS) {
            return String.format("[SUCCESS] %s | %s", rowContext, fieldName);
        }

        return String.format("[%s] %s | %s | %s | %s",
                severity, rowContext, fieldName, errorCode, message);
    }

    /**
     * Formats diagnostic for JSON serialization (monitoring dashboards).
     */
    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"rowContext\":\"").append(escapeJson(rowContext)).append("\",");
        json.append("\"fieldName\":\"").append(escapeJson(fieldName)).append("\",");
        json.append("\"severity\":\"").append(severity).append("\",");

        if (errorCode != null) {
            json.append("\"errorCode\":\"").append(escapeJson(errorCode)).append("\",");
        }

        if (message != null) {
            json.append("\"message\":\"").append(escapeJson(message)).append("\",");
        }

        json.append("\"timestamp\":\"").append(timestamp).append("\"");
        json.append("}");

        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return toLogFormat();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationDiagnostic that = (ValidationDiagnostic) o;
        return Objects.equals(rowContext, that.rowContext) &&
                Objects.equals(fieldName, that.fieldName) &&
                severity == that.severity &&
                Objects.equals(errorCode, that.errorCode) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowContext, fieldName, severity, errorCode, message);
    }

    public enum Severity {
        SUCCESS,   // Field passes validation
        WARNING,   // Field has data quality concern but not blocking
        ERROR      // Field fails validation - row should be rejected
    }
}
