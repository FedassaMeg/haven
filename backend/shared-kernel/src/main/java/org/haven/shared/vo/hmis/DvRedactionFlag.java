package org.haven.shared.vo.hmis;

/**
 * VAWA Domestic Violence Data Redaction Classification
 * Controls redaction levels for DV-related data per VAWA requirements
 * Implements role-based access control for sensitive data
 */
public enum DvRedactionFlag {

    NO_REDACTION(0, "No redaction required", false),
    REDACT_FOR_GENERAL_STAFF(1, "Redact for general staff", false),
    REDACT_FOR_NON_DV_SPECIALISTS(2, "Redact for non-DV specialists", true),
    FULL_REDACTION_REQUIRED(3, "Full redaction required", true),
    VICTIM_REQUESTED_CONFIDENTIALITY(4, "Victim requested confidentiality", true);

    private final int level;
    private final String description;
    private final boolean requiresSpecializedAccess;

    DvRedactionFlag(int level, String description, boolean requiresSpecializedAccess) {
        this.level = level;
        this.description = description;
        this.requiresSpecializedAccess = requiresSpecializedAccess;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresSpecializedAccess() {
        return requiresSpecializedAccess;
    }

    public boolean allowsGeneralStaffAccess() {
        return this == NO_REDACTION;
    }

    public boolean allowsDvSpecialistAccess() {
        return this != FULL_REDACTION_REQUIRED && this != VICTIM_REQUESTED_CONFIDENTIALITY;
    }

    /**
     * Check if this redaction level blocks access for a given role
     */
    public boolean blocksAccessForRole(String role) {
        return switch (this) {
            case NO_REDACTION -> false;
            case REDACT_FOR_GENERAL_STAFF ->
                !role.contains("DV_") && !role.contains("ADMIN") && !role.contains("SPECIALIST");
            case REDACT_FOR_NON_DV_SPECIALISTS ->
                !role.contains("DV_SPECIALIST") && !role.contains("ADMIN");
            case FULL_REDACTION_REQUIRED ->
                !role.contains("ADMIN");
            case VICTIM_REQUESTED_CONFIDENTIALITY -> true;
        };
    }
}