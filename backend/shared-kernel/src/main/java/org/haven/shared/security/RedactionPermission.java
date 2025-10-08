package org.haven.shared.security;

import java.util.*;

/**
 * Internal permission model mapping user roles and consent scopes to redaction policies
 * Implements role-based and consent-based data access control
 */
public class RedactionPermission {

    private final Set<UserRole> userRoles;
    private final Set<ConsentScope> consentScopes;
    private final RedactionLevel defaultRedactionLevel;

    private RedactionPermission(Set<UserRole> userRoles, Set<ConsentScope> consentScopes, RedactionLevel defaultLevel) {
        this.userRoles = Collections.unmodifiableSet(userRoles);
        this.consentScopes = Collections.unmodifiableSet(consentScopes);
        this.defaultRedactionLevel = defaultLevel;
    }

    /**
     * Create permission from roles and consent scopes
     */
    public static RedactionPermission from(List<UserRole> roles, List<String> scopeStrings) {
        Set<UserRole> roleSet = new HashSet<>(roles);
        Set<ConsentScope> scopeSet = new HashSet<>();

        for (String scopeString : scopeStrings) {
            try {
                scopeSet.add(ConsentScope.fromString(scopeString));
            } catch (IllegalArgumentException e) {
                // Unknown scope, ignore
            }
        }

        RedactionLevel defaultLevel = determineDefaultLevel(roleSet, scopeSet);
        return new RedactionPermission(roleSet, scopeSet, defaultLevel);
    }

    /**
     * Determine default redaction level based on roles and scopes
     */
    private static RedactionLevel determineDefaultLevel(Set<UserRole> roles, Set<ConsentScope> scopes) {
        // External partners get highest redaction by default
        if (roles.stream().anyMatch(UserRole::isExternalPartner)) {
            return RedactionLevel.FULL_REDACTION;
        }

        // Clinical roles with appropriate consent get minimal redaction
        if (roles.stream().anyMatch(UserRole::isClinical) && scopes.contains(ConsentScope.DV_VIEW)) {
            return RedactionLevel.MINIMAL;
        }

        // Legal advocates with legal consent
        if (roles.stream().anyMatch(UserRole::isLegal) && scopes.contains(ConsentScope.LEGAL_VIEW)) {
            return RedactionLevel.MINIMAL;
        }

        // Medical roles with medical consent
        if (roles.stream().anyMatch(UserRole::isMedical) && scopes.contains(ConsentScope.MEDICAL_VIEW)) {
            return RedactionLevel.MINIMAL;
        }

        // Administrative roles get partial redaction
        if (roles.stream().anyMatch(UserRole::isAdministrative)) {
            return RedactionLevel.PARTIAL;
        }

        // Default to partial redaction
        return RedactionLevel.PARTIAL;
    }

    /**
     * Check if user can view confidential DV notes
     */
    public boolean canViewDVNotes() {
        // DV Counselors with dv_view consent scope
        boolean hasDVRole = userRoles.contains(UserRole.DV_COUNSELOR) ||
                           userRoles.contains(UserRole.DV_COUNSELOR);
        return hasDVRole && consentScopes.contains(ConsentScope.DV_VIEW);
    }

    /**
     * Check if user can view full PII (unredacted identifiers)
     */
    public boolean canViewFullPII() {
        // Only specific roles with appropriate consent
        if (userRoles.stream().anyMatch(UserRole::isExternalPartner)) {
            return false; // External partners never see full PII
        }

        // Clinical, legal, medical with appropriate consent
        boolean hasPrivilegedRole = userRoles.stream().anyMatch(r ->
            r.isClinical() || r.isLegal() || r.isMedical()
        );

        boolean hasAppropriateConsent = consentScopes.contains(ConsentScope.DV_VIEW) ||
                                       consentScopes.contains(ConsentScope.LEGAL_VIEW) ||
                                       consentScopes.contains(ConsentScope.MEDICAL_VIEW);

        return hasPrivilegedRole && hasAppropriateConsent;
    }

    /**
     * Check if user receives redacted payloads by default (like CE intake)
     */
    public boolean receivesRedactedByDefault() {
        // CE intake and external partners receive redacted data by default
        return userRoles.stream().anyMatch(UserRole::isExternalPartner) ||
               !canViewFullPII();
    }

    /**
     * Get redaction level for specific field type
     */
    public RedactionLevel getRedactionLevelForField(FieldType fieldType) {
        return switch (fieldType) {
            case DIRECT_IDENTIFIER -> getIdentifierRedactionLevel();
            case SENSITIVE_DV_NOTE -> getDVNoteRedactionLevel();
            case MEDICAL_INFO -> getMedicalInfoRedactionLevel();
            case LEGAL_INFO -> getLegalInfoRedactionLevel();
            case CONTACT_INFO -> getContactInfoRedactionLevel();
            case SERVICE_DATA -> RedactionLevel.NO_REDACTION;
        };
    }

    private RedactionLevel getIdentifierRedactionLevel() {
        if (canViewFullPII()) {
            return RedactionLevel.NO_REDACTION;
        }
        // External partners and CE intake see hashed identifiers only
        if (receivesRedactedByDefault()) {
            return RedactionLevel.HASH_ONLY;
        }
        return RedactionLevel.PARTIAL;
    }

    private RedactionLevel getDVNoteRedactionLevel() {
        if (canViewDVNotes()) {
            return RedactionLevel.NO_REDACTION;
        }
        return RedactionLevel.FULL_REDACTION;
    }

    private RedactionLevel getMedicalInfoRedactionLevel() {
        if (userRoles.stream().anyMatch(UserRole::isMedical) &&
            consentScopes.contains(ConsentScope.MEDICAL_VIEW)) {
            return RedactionLevel.NO_REDACTION;
        }
        if (userRoles.stream().anyMatch(r -> r.isClinical() || r.isLegal())) {
            return RedactionLevel.PARTIAL;
        }
        return RedactionLevel.FULL_REDACTION;
    }

    private RedactionLevel getLegalInfoRedactionLevel() {
        if (userRoles.stream().anyMatch(UserRole::isLegal) &&
            consentScopes.contains(ConsentScope.LEGAL_VIEW)) {
            return RedactionLevel.NO_REDACTION;
        }
        if (userRoles.stream().anyMatch(r -> r.isClinical() || r.isAdministrative())) {
            return RedactionLevel.PARTIAL;
        }
        return RedactionLevel.FULL_REDACTION;
    }

    private RedactionLevel getContactInfoRedactionLevel() {
        if (canViewFullPII()) {
            return RedactionLevel.NO_REDACTION;
        }
        return RedactionLevel.PARTIAL;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public Set<ConsentScope> getConsentScopes() {
        return consentScopes;
    }

    public RedactionLevel getDefaultRedactionLevel() {
        return defaultRedactionLevel;
    }

    /**
     * Check if permission has specific role
     */
    public boolean hasRole(UserRole role) {
        return userRoles.contains(role);
    }

    /**
     * Check if permission has specific consent scope
     */
    public boolean hasConsentScope(ConsentScope scope) {
        return consentScopes.contains(scope);
    }

    @Override
    public String toString() {
        return String.format("RedactionPermission[roles=%s, scopes=%s, defaultLevel=%s]",
                userRoles, consentScopes, defaultRedactionLevel);
    }

    /**
     * Consent scopes that affect data redaction
     */
    public enum ConsentScope {
        DV_VIEW("dv_view"),
        LEGAL_VIEW("legal_view"),
        MEDICAL_VIEW("medical_view"),
        HMIS_EXPORT("hmis_export"),
        RESEARCH_VIEW("research_view"),
        COURT_TESTIMONY("court_testimony");

        private final String scopeString;

        ConsentScope(String scopeString) {
            this.scopeString = scopeString;
        }

        public String getScopeString() {
            return scopeString;
        }

        public static ConsentScope fromString(String scopeString) {
            for (ConsentScope scope : values()) {
                if (scope.scopeString.equalsIgnoreCase(scopeString)) {
                    return scope;
                }
            }
            throw new IllegalArgumentException("Unknown consent scope: " + scopeString);
        }
    }

    /**
     * Redaction levels for different data types
     */
    public enum RedactionLevel {
        NO_REDACTION,      // Full data visible
        MINIMAL,           // Minor redaction (e.g., last 4 digits only)
        PARTIAL,           // Significant redaction (e.g., masked)
        HASH_ONLY,         // Only deterministic hash visible
        FULL_REDACTION     // Completely hidden/null
    }

    /**
     * Field types that require different redaction levels
     */
    public enum FieldType {
        DIRECT_IDENTIFIER,
        SENSITIVE_DV_NOTE,
        MEDICAL_INFO,
        LEGAL_INFO,
        CONTACT_INFO,
        SERVICE_DATA
    }
}
