package org.haven.reporting.application.validation;

/**
 * Exception thrown when CSV validation fails with critical errors.
 *
 * Contains validation summary with PII-safe diagnostics for
 * error reporting and remediation.
 */
public class CsvValidationException extends RuntimeException {

    private final CsvValidationLogger.ValidationSummary validationSummary;

    public CsvValidationException(String message, CsvValidationLogger.ValidationSummary validationSummary) {
        super(message);
        this.validationSummary = validationSummary;
    }

    public CsvValidationLogger.ValidationSummary getValidationSummary() {
        return validationSummary;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " - Error rate: " +
                String.format("%.1f%%", validationSummary.errorRate() * 100);
    }
}
