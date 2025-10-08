package org.haven.reporting.domain;

import org.haven.shared.security.UserRole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/**
 * Security clearance for unhashed export authorization
 * Combines role-based access with time-limited authorization and audit trail
 */
public record ExportSecurityClearance(
    UUID clearanceId,
    UUID userId,
    String userName,
    Set<UserRole> authorizedRoles,
    Set<ExportConsentScope> grantedScopes,
    Instant grantedAt,
    Instant expiresAt,
    String grantedBy,
    String justification,
    String policyVersion
) {
    /**
     * Create a new security clearance
     */
    public static ExportSecurityClearance grant(
            UUID userId,
            String userName,
            Set<UserRole> authorizedRoles,
            Set<ExportConsentScope> grantedScopes,
            String grantedBy,
            String justification,
            int validForHours) {

        return new ExportSecurityClearance(
            UUID.randomUUID(),
            userId,
            userName,
            authorizedRoles,
            grantedScopes,
            Instant.now(),
            Instant.now().plus(validForHours, ChronoUnit.HOURS),
            grantedBy,
            justification,
            "v1.0"
        );
    }

    /**
     * @return true if clearance is currently valid
     */
    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * @return true if clearance has expired
     */
    public boolean isExpired() {
        return !isValid();
    }

    /**
     * @return true if clearance authorizes unhashed exports
     */
    public boolean authorizesUnhashedExports() {
        return isValid() && grantedScopes.stream()
                .anyMatch(ExportConsentScope::authorizesUnhashedPII);
    }

    /**
     * @return true if clearance includes VAWA override
     */
    public boolean hasVawaOverride() {
        return isValid() && grantedScopes.contains(ExportConsentScope.VAWA_OVERRIDE);
    }

    /**
     * @return true if user has required role
     */
    public boolean hasRole(UserRole role) {
        return authorizedRoles.contains(role);
    }

    /**
     * @return true if clearance covers the requested scope
     */
    public boolean coversScope(ExportConsentScope requestedScope) {
        return isValid() && grantedScopes.contains(requestedScope);
    }
}
