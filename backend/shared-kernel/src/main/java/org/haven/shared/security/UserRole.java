package org.haven.shared.security;

/**
 * Centralized role definitions for HAVEN system
 * Prevents role string duplication across codebase
 */
public enum UserRole {
    // Case Management Roles
    CASE_MANAGER("Case Manager", RoleCategory.CASE_MANAGEMENT),
    SUPERVISOR("Supervisor", RoleCategory.MANAGEMENT),

    // Clinical Roles
    DV_COUNSELOR("DV Counselor", RoleCategory.CLINICAL),
    LICENSED_CLINICIAN("Licensed Clinician", RoleCategory.CLINICAL),
    CLINICIAN("Clinician", RoleCategory.CLINICAL),
    THERAPIST("Therapist", RoleCategory.CLINICAL),
    COUNSELOR("Counselor", RoleCategory.CLINICAL),

    // Legal Roles
    LEGAL_ADVOCATE("Legal Advocate", RoleCategory.LEGAL),
    ATTORNEY("Attorney", RoleCategory.LEGAL),

    // Safety Roles
    SAFETY_SPECIALIST("Safety Specialist", RoleCategory.SAFETY),
    CRISIS_COUNSELOR("Crisis Counselor", RoleCategory.SAFETY),

    // Medical Roles
    NURSE("Nurse", RoleCategory.MEDICAL),
    DOCTOR("Doctor", RoleCategory.MEDICAL),
    MEDICAL_ADVOCATE("Medical Advocate", RoleCategory.MEDICAL),

    // Administrative Roles
    ADMINISTRATOR("Administrator", RoleCategory.ADMINISTRATIVE),

    // External Partner Roles
    VSP("Victim Service Provider", RoleCategory.EXTERNAL_PARTNER),
    VICTIM_SERVICE_PROVIDER("Victim Service Provider", RoleCategory.EXTERNAL_PARTNER),
    COMMUNITY_PARTNER("Community Partner", RoleCategory.EXTERNAL_PARTNER);

    private final String displayName;
    private final RoleCategory category;

    UserRole(String displayName, RoleCategory category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RoleCategory getCategory() {
        return category;
    }

    public boolean isClinical() {
        return category == RoleCategory.CLINICAL;
    }

    public boolean isLegal() {
        return category == RoleCategory.LEGAL;
    }

    public boolean isMedical() {
        return category == RoleCategory.MEDICAL;
    }

    public boolean isAdministrative() {
        return category == RoleCategory.ADMINISTRATIVE || category == RoleCategory.MANAGEMENT;
    }

    public boolean isExternalPartner() {
        return category == RoleCategory.EXTERNAL_PARTNER;
    }

    /**
     * Parse role from string (backwards compatibility)
     */
    public static UserRole fromString(String roleString) {
        try {
            return UserRole.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + roleString);
        }
    }

    public enum RoleCategory {
        CASE_MANAGEMENT,
        CLINICAL,
        LEGAL,
        SAFETY,
        MEDICAL,
        ADMINISTRATIVE,
        MANAGEMENT,
        EXTERNAL_PARTNER
    }
}
