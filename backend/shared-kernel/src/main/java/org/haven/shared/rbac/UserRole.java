package org.haven.shared.rbac;

/**
 * User role enum matching database user_role type.
 * Maps to Keycloak realm roles via kebab-case transformation.
 */
public enum UserRole {
    ADMIN,              // admin
    SUPERVISOR,         // supervisor
    CASE_MANAGER,       // case-manager
    INTAKE_SPECIALIST,  // intake-specialist
    REPORT_VIEWER,      // report-viewer
    EXTERNAL_PARTNER,   // external-partner
    CE_INTAKE,          // ce-intake
    DV_ADVOCATE,        // dv-advocate
    COMPLIANCE_AUDITOR, // compliance-auditor
    EXEC;               // exec

    /**
     * Convert to Keycloak role name (kebab-case).
     */
    public String toKeycloakRole() {
        return this.name().toLowerCase().replace('_', '-');
    }

    /**
     * Parse from Keycloak role name.
     */
    public static UserRole fromKeycloakRole(String keycloakRole) {
        String enumName = keycloakRole.toUpperCase().replace('-', '_');
        return UserRole.valueOf(enumName);
    }
}
