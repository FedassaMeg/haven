package org.haven.reporting.domain;

import java.util.Set;
import java.util.UUID;

/**
 * Tenant-level configuration for export behavior and security policies
 * Controls PII hashing, consent requirements, and authorization rules
 */
public class TenantExportConfiguration {

    private final UUID tenantId;
    private final String organizationName;
    private ExportHashBehavior hashBehavior;
    private Set<ExportConsentScope> requiredScopesForUnhashed;
    private boolean requireDualAuthorization;
    private boolean requireLegalReview;
    private int clearanceValidityHours;
    private boolean enableExportAuditAlerts;
    private Set<String> alertRecipients;

    public TenantExportConfiguration(
            UUID tenantId,
            String organizationName,
            ExportHashBehavior hashBehavior,
            Set<ExportConsentScope> requiredScopesForUnhashed,
            boolean requireDualAuthorization,
            boolean requireLegalReview,
            int clearanceValidityHours,
            boolean enableExportAuditAlerts,
            Set<String> alertRecipients) {
        this.tenantId = tenantId;
        this.organizationName = organizationName;
        this.hashBehavior = hashBehavior;
        this.requiredScopesForUnhashed = requiredScopesForUnhashed;
        this.requireDualAuthorization = requireDualAuthorization;
        this.requireLegalReview = requireLegalReview;
        this.clearanceValidityHours = clearanceValidityHours;
        this.enableExportAuditAlerts = enableExportAuditAlerts;
        this.alertRecipients = alertRecipients;
    }

    /**
     * Create default secure configuration
     */
    public static TenantExportConfiguration defaultConfiguration(UUID tenantId, String organizationName) {
        return new TenantExportConfiguration(
            tenantId,
            organizationName,
            ExportHashBehavior.ALWAYS_HASH,
            Set.of(ExportConsentScope.PII_DISCLOSURE, ExportConsentScope.HUD_REPORTING),
            true,
            true,
            24, // 24 hour clearance validity
            true,
            Set.of()
        );
    }

    /**
     * Validate if unhashed export is allowed under current policy
     */
    public boolean allowsUnhashedExport(
            Set<ExportConsentScope> providedScopes,
            ExportSecurityClearance clearance) {

        // If always hash, reject immediately
        if (hashBehavior.prohibitsUnhashed()) {
            return false;
        }

        // If never hash, allow immediately
        if (hashBehavior.allowsUnhashedByDefault()) {
            return true;
        }

        // Consent-based: verify all requirements
        if (!hasRequiredScopes(providedScopes)) {
            return false;
        }

        if (clearance == null || !clearance.isValid()) {
            return false;
        }

        if (!clearance.authorizesUnhashedExports()) {
            return false;
        }

        return true;
    }

    /**
     * Check if provided scopes satisfy requirements
     */
    private boolean hasRequiredScopes(Set<ExportConsentScope> providedScopes) {
        return providedScopes.containsAll(requiredScopesForUnhashed);
    }

    /**
     * Get failure reason for unhashed export rejection
     */
    public String getUnhashedRejectionReason(
            Set<ExportConsentScope> providedScopes,
            ExportSecurityClearance clearance) {

        if (hashBehavior.prohibitsUnhashed()) {
            return "Organization policy prohibits unhashed exports (ALWAYS_HASH mode)";
        }

        if (!hasRequiredScopes(providedScopes)) {
            Set<ExportConsentScope> missing = Set.copyOf(requiredScopesForUnhashed);
            missing.removeAll(providedScopes);
            return "Missing required consent scopes: " + missing;
        }

        if (clearance == null) {
            return "No security clearance provided";
        }

        if (clearance.isExpired()) {
            return "Security clearance expired at " + clearance.expiresAt();
        }

        if (!clearance.authorizesUnhashedExports()) {
            return "Security clearance does not authorize unhashed exports";
        }

        return "Unknown rejection reason";
    }

    // Getters and setters

    public UUID getTenantId() {
        return tenantId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public ExportHashBehavior getHashBehavior() {
        return hashBehavior;
    }

    public void setHashBehavior(ExportHashBehavior hashBehavior) {
        this.hashBehavior = hashBehavior;
    }

    public Set<ExportConsentScope> getRequiredScopesForUnhashed() {
        return requiredScopesForUnhashed;
    }

    public void setRequiredScopesForUnhashed(Set<ExportConsentScope> requiredScopesForUnhashed) {
        this.requiredScopesForUnhashed = requiredScopesForUnhashed;
    }

    public boolean isRequireDualAuthorization() {
        return requireDualAuthorization;
    }

    public void setRequireDualAuthorization(boolean requireDualAuthorization) {
        this.requireDualAuthorization = requireDualAuthorization;
    }

    public boolean isRequireLegalReview() {
        return requireLegalReview;
    }

    public void setRequireLegalReview(boolean requireLegalReview) {
        this.requireLegalReview = requireLegalReview;
    }

    public int getClearanceValidityHours() {
        return clearanceValidityHours;
    }

    public void setClearanceValidityHours(int clearanceValidityHours) {
        this.clearanceValidityHours = clearanceValidityHours;
    }

    public boolean isEnableExportAuditAlerts() {
        return enableExportAuditAlerts;
    }

    public void setEnableExportAuditAlerts(boolean enableExportAuditAlerts) {
        this.enableExportAuditAlerts = enableExportAuditAlerts;
    }

    public Set<String> getAlertRecipients() {
        return alertRecipients;
    }

    public void setAlertRecipients(Set<String> alertRecipients) {
        this.alertRecipients = alertRecipients;
    }
}
