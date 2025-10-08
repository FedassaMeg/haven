package org.haven.shared.vo.services;

/**
 * Service categories for grouping and reporting purposes
 * Aligned with HMIS and funder reporting requirements
 */
public enum ServiceCategory {
    HOUSING("Housing Services"),
    CRISIS_RESPONSE("Crisis Response"),
    COUNSELING("Counseling & Mental Health"),
    LEGAL("Legal Services"),
    CASE_MANAGEMENT("Case Management"),
    FINANCIAL("Financial Assistance"),
    HEALTHCARE("Healthcare & Medical"),
    CHILDREN_FAMILY("Children & Family Services"),
    EDUCATION("Education & Life Skills"),
    SUPPORT_SERVICES("Transportation & Support"),
    INFORMATION("Information & Referral"),
    DV_SPECIFIC("Domestic Violence Specific"),
    SA_SPECIFIC("Sexual Assault Specific"),
    OTHER("Other");

    private final String description;

    ServiceCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determine if this category typically requires specialized training
     */
    public boolean requiresSpecializedTraining() {
        return this == DV_SPECIFIC ||
               this == SA_SPECIFIC ||
               this == CRISIS_RESPONSE ||
               this == COUNSELING ||
               this == LEGAL;
    }

    /**
     * Determine if this category has specific licensing requirements
     */
    public boolean hasLicensingRequirements() {
        return this == COUNSELING ||
               this == LEGAL ||
               this == HEALTHCARE;
    }

    /**
     * Get reporting priority level for funders
     */
    public ReportingPriority getReportingPriority() {
        return switch (this) {
            case HOUSING, CRISIS_RESPONSE -> ReportingPriority.HIGH;
            case COUNSELING, LEGAL, CASE_MANAGEMENT -> ReportingPriority.MEDIUM;
            case FINANCIAL, HEALTHCARE, CHILDREN_FAMILY -> ReportingPriority.MEDIUM;
            case DV_SPECIFIC, SA_SPECIFIC -> ReportingPriority.HIGH;
            default -> ReportingPriority.LOW;
        };
    }

    public enum ReportingPriority {
        HIGH, MEDIUM, LOW
    }
}