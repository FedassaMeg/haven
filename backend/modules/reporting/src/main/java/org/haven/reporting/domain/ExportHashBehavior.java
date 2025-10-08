package org.haven.reporting.domain;

/**
 * Defines tenant-level policy for PII hashing in exports
 *
 * ALWAYS_HASH: All exports must use hashed PII (default, most secure)
 * CONSENT_BASED: Unhashed exports allowed with explicit consent + clearance
 * NEVER_HASH: Unhashed exports always allowed (use only for testing/internal)
 */
public enum ExportHashBehavior {
    ALWAYS_HASH,
    CONSENT_BASED,
    NEVER_HASH;

    /**
     * @return true if unhashed exports require explicit approval
     */
    public boolean requiresConsentForUnhashed() {
        return this == CONSENT_BASED;
    }

    /**
     * @return true if unhashed exports are never allowed
     */
    public boolean prohibitsUnhashed() {
        return this == ALWAYS_HASH;
    }

    /**
     * @return true if unhashed exports are always allowed
     */
    public boolean allowsUnhashedByDefault() {
        return this == NEVER_HASH;
    }

    /**
     * @return human-readable description of this hash behavior
     */
    public String getDescription() {
        return switch (this) {
            case ALWAYS_HASH -> "Always Hash PII (Most Secure)";
            case CONSENT_BASED -> "Consent-Based Unhashed Exports";
            case NEVER_HASH -> "Never Hash (Testing/Internal Only)";
        };
    }
}
