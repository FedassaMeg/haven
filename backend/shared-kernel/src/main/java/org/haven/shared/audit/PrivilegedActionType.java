package org.haven.shared.audit;

/**
 * Enumeration of privileged actions requiring enhanced audit logging.
 *
 * Privileged actions are security-sensitive operations that require:
 * - Mandatory audit trail (non-repudiation)
 * - SIEM routing for security monitoring
 * - Detailed context capture (who, what, when, why, how)
 * - Success AND failure logging
 *
 * Compliance requirements: VAWA, SOX, GDPR, HIPAA
 */
public enum PrivilegedActionType {

    // Domestic Violence Note Operations
    DV_NOTE_READ("Read restricted/DV note", "CRITICAL", true),
    DV_NOTE_WRITE("Create/update restricted/DV note", "CRITICAL", true),
    DV_NOTE_DELETE("Delete restricted/DV note", "CRITICAL", true),
    DV_NOTE_SEAL("Seal restricted/DV note", "CRITICAL", true),
    DV_NOTE_UNSEAL("Unseal restricted/DV note", "CRITICAL", true),
    DV_NOTE_ACCESS_LIST_MODIFIED("Modify note access control list", "HIGH", true),

    // Export Operations
    EXPORT_INITIATED("Export job initiated", "HIGH", true),
    EXPORT_COMPLETED("Export job completed", "HIGH", true),
    EXPORT_FAILED("Export job failed", "MEDIUM", true),
    EXPORT_DOWNLOADED("Export file downloaded", "CRITICAL", true),
    EXPORT_PURGED("Export file purged", "MEDIUM", true),

    // Consent Ledger Operations
    CONSENT_OVERRIDE_ATTEMPTED("Consent override attempted", "CRITICAL", true),
    CONSENT_OVERRIDE_GRANTED("Consent override granted", "CRITICAL", true),
    CONSENT_OVERRIDE_DENIED("Consent override denied", "CRITICAL", true),
    CONSENT_LEDGER_ENTRY_CREATED("Consent ledger entry created", "HIGH", false),
    CONSENT_LEDGER_ENTRY_MODIFIED("Consent ledger entry modified", "CRITICAL", true),

    // Ledger Adjustment Operations
    LEDGER_ADJUSTMENT_CREATED("Ledger adjustment created", "HIGH", true),
    LEDGER_ADJUSTMENT_APPROVED("Ledger adjustment approved", "HIGH", true),
    LEDGER_ADJUSTMENT_REJECTED("Ledger adjustment rejected", "MEDIUM", true),
    LEDGER_ADJUSTMENT_REVERTED("Ledger adjustment reverted", "CRITICAL", true),

    // Policy Decision Operations
    POLICY_DECISION_OVERRIDE("Security policy decision overridden", "CRITICAL", true),
    POLICY_DECISION_ESCALATION("Security policy decision escalated", "HIGH", true),

    // PII/VSP Operations
    PII_EXPORT_FULL_SSN("PII export with full SSN", "CRITICAL", true),
    PII_REDACTION_OVERRIDE("PII redaction policy overridden", "CRITICAL", true),
    VSP_AUDIT_LOG_ACCESSED("VSP audit log accessed", "CRITICAL", true),

    // Administrative Operations
    ADMIN_ROLE_ASSIGNED("Administrative role assigned", "HIGH", true),
    ADMIN_ROLE_REVOKED("Administrative role revoked", "HIGH", true),
    SECURITY_CONFIG_MODIFIED("Security configuration modified", "CRITICAL", true),
    AUDIT_LOG_ACCESSED("Audit log accessed/queried", "HIGH", true),
    AUDIT_LOG_EXPORT_REQUESTED("Audit log export requested", "CRITICAL", true);

    private final String description;
    private final String severity;
    private final boolean requiresJustification;

    PrivilegedActionType(String description, String severity, boolean requiresJustification) {
        this.description = description;
        this.severity = severity;
        this.requiresJustification = requiresJustification;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean requiresJustification() {
        return requiresJustification;
    }

    /**
     * Returns SIEM routing tag for this action type
     */
    public String getSiemTag() {
        return switch (this) {
            case DV_NOTE_READ, DV_NOTE_WRITE, DV_NOTE_DELETE, DV_NOTE_SEAL,
                 DV_NOTE_UNSEAL, DV_NOTE_ACCESS_LIST_MODIFIED -> "pii_audit:dv_note";
            case EXPORT_INITIATED, EXPORT_COMPLETED, EXPORT_FAILED,
                 EXPORT_DOWNLOADED, EXPORT_PURGED -> "pii_audit:export";
            case CONSENT_OVERRIDE_ATTEMPTED, CONSENT_OVERRIDE_GRANTED,
                 CONSENT_OVERRIDE_DENIED, CONSENT_LEDGER_ENTRY_CREATED,
                 CONSENT_LEDGER_ENTRY_MODIFIED -> "pii_audit:consent";
            case LEDGER_ADJUSTMENT_CREATED, LEDGER_ADJUSTMENT_APPROVED,
                 LEDGER_ADJUSTMENT_REJECTED, LEDGER_ADJUSTMENT_REVERTED -> "pii_audit:ledger";
            case POLICY_DECISION_OVERRIDE, POLICY_DECISION_ESCALATION -> "pii_audit:policy";
            case PII_EXPORT_FULL_SSN, PII_REDACTION_OVERRIDE,
                 VSP_AUDIT_LOG_ACCESSED -> "pii_audit:pii";
            default -> "pii_audit:admin";
        };
    }
}
