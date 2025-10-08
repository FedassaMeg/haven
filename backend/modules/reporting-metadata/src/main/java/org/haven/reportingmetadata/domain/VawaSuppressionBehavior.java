package org.haven.reportingmetadata.domain;

/**
 * VAWA confidentiality suppression behaviors when consent not granted
 *
 * Per Violence Against Women Act (VAWA) confidentiality requirements,
 * certain data elements require explicit client consent before disclosure
 * in reports or exports.
 */
public enum VawaSuppressionBehavior {
    /**
     * Completely suppress field from export/report
     * Individual-level record excluded entirely
     */
    SUPPRESS,

    /**
     * Allow field in aggregate counts only
     * Individual-level data suppressed, but included in totals
     * Example: Can count total DV victims but not show individual records
     */
    AGGREGATE_ONLY,

    /**
     * Show field with redacted value (null, 8, 9, 99 per HUD spec)
     * Record included but sensitive field shows as "Data not collected" or "Client refused"
     */
    REDACT
}
