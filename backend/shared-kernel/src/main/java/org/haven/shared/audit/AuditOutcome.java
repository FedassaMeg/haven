package org.haven.shared.audit;

/**
 * Outcome of a privileged action attempt.
 *
 * Used for both success and failure logging to support:
 * - Intrusion detection (multiple denied attempts)
 * - Compliance reporting (who accessed what data)
 * - Forensic investigation (timeline reconstruction)
 */
public enum AuditOutcome {
    SUCCESS("Action completed successfully"),
    DENIED_INSUFFICIENT_PERMISSION("Denied - insufficient permission"),
    DENIED_CONSENT_REQUIRED("Denied - consent required"),
    DENIED_POLICY_VIOLATION("Denied - security policy violation"),
    DENIED_VAWA_PROTECTED("Denied - VAWA protected data"),
    DENIED_VSP_RESTRICTED("Denied - Violence Survivor Protection restriction"),
    DENIED_INVALID_JUSTIFICATION("Denied - invalid or missing justification"),
    DENIED_RATE_LIMITED("Denied - rate limit exceeded"),
    ERROR_SYSTEM_FAILURE("Error - system failure"),
    ERROR_INVALID_REQUEST("Error - invalid request"),
    PARTIAL_SUCCESS("Partially successful - some data redacted");

    private final String description;

    AuditOutcome(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccess() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }

    public boolean isDenial() {
        return name().startsWith("DENIED_");
    }

    public boolean isError() {
        return name().startsWith("ERROR_");
    }
}
