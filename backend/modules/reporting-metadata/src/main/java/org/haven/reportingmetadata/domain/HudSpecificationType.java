package org.haven.reportingmetadata.domain;

/**
 * HUD reporting specification types
 *
 * Compliance references:
 * - HMIS_CSV: HUD HMIS CSV Format Specifications (HDX 2024)
 * - CoC_APR: Continuum of Care Annual Performance Report (24 CFR 578.103)
 * - ESG_CAPER: Emergency Solutions Grants Consolidated Annual Performance and Evaluation Report
 * - SYSTEM_PERFORMANCE_MEASURES: HUD System Performance Measures (Notice CPD-17-01)
 * - PIT_HIC: Point-in-Time Count and Housing Inventory Count
 * - LSA: Longitudinal Systems Analysis
 */
public enum HudSpecificationType {
    /**
     * HMIS CSV export format for data exchange
     * Universal export format for all HMIS implementations
     */
    HMIS_CSV,

    /**
     * CoC Annual Performance Report (APR)
     * Required annually for CoC-funded projects per 24 CFR 578.103
     * Sections Q04a through Q27c
     */
    CoC_APR,

    /**
     * ESG Consolidated Annual Performance and Evaluation Report (CAPER)
     * Required annually for ESG grantees
     */
    ESG_CAPER,

    /**
     * System Performance Measures (SPM)
     * Seven measures tracking CoC-wide homeless system performance
     */
    SYSTEM_PERFORMANCE_MEASURES,

    /**
     * Point-in-Time Count and Housing Inventory Count
     * Annual enumeration of homeless persons and housing resources
     */
    PIT_HIC,

    /**
     * Longitudinal Systems Analysis
     * Advanced reporting for CoC system analysis
     */
    LSA
}
