package org.haven.shared.vo.services;

/**
 * Comprehensive service type enumeration aligned with HMIS standards
 * and common domestic violence/homeless services
 */
public enum ServiceType {
    // Housing Services
    EMERGENCY_SHELTER("Emergency Shelter", ServiceCategory.HOUSING),
    TRANSITIONAL_HOUSING("Transitional Housing", ServiceCategory.HOUSING),
    RAPID_REHOUSING("Rapid Re-housing", ServiceCategory.HOUSING),
    PERMANENT_SUPPORTIVE_HOUSING("Permanent Supportive Housing", ServiceCategory.HOUSING),
    HOUSING_SEARCH_ASSISTANCE("Housing Search Assistance", ServiceCategory.HOUSING),
    HOUSING_STABILITY_CASE_MANAGEMENT("Housing Stability Case Management", ServiceCategory.HOUSING),
    RENT_ASSISTANCE("Rent Assistance", ServiceCategory.HOUSING),
    UTILITY_ASSISTANCE("Utility Assistance", ServiceCategory.HOUSING),
    SECURITY_DEPOSIT_ASSISTANCE("Security Deposit Assistance", ServiceCategory.HOUSING),
    
    // Crisis Intervention & Safety
    CRISIS_INTERVENTION("Crisis Intervention", ServiceCategory.CRISIS_RESPONSE),
    SAFETY_PLANNING("Safety Planning", ServiceCategory.CRISIS_RESPONSE),
    RISK_ASSESSMENT("Risk Assessment", ServiceCategory.CRISIS_RESPONSE),
    EMERGENCY_RESPONSE("Emergency Response", ServiceCategory.CRISIS_RESPONSE),
    MOBILE_CRISIS_RESPONSE("Mobile Crisis Response", ServiceCategory.CRISIS_RESPONSE),
    HOTLINE_CRISIS_CALL("Hotline Crisis Call", ServiceCategory.CRISIS_RESPONSE),
    
    // Counseling & Mental Health
    INDIVIDUAL_COUNSELING("Individual Counseling", ServiceCategory.COUNSELING),
    GROUP_COUNSELING("Group Counseling", ServiceCategory.COUNSELING),
    FAMILY_COUNSELING("Family Counseling", ServiceCategory.COUNSELING),
    TRAUMA_COUNSELING("Trauma Counseling", ServiceCategory.COUNSELING),
    SUBSTANCE_ABUSE_COUNSELING("Substance Abuse Counseling", ServiceCategory.COUNSELING),
    MENTAL_HEALTH_SERVICES("Mental Health Services", ServiceCategory.COUNSELING),
    PSYCHIATRIC_EVALUATION("Psychiatric Evaluation", ServiceCategory.COUNSELING),
    THERAPY_SESSION("Therapy Session", ServiceCategory.COUNSELING),
    SUPPORT_GROUP("Support Group", ServiceCategory.COUNSELING),
    
    // Legal Services
    LEGAL_ADVOCACY("Legal Advocacy", ServiceCategory.LEGAL),
    COURT_ACCOMPANIMENT("Court Accompaniment", ServiceCategory.LEGAL),
    PROTECTION_ORDER_ASSISTANCE("Protection Order Assistance", ServiceCategory.LEGAL),
    IMMIGRATION_LEGAL_SERVICES("Immigration Legal Services", ServiceCategory.LEGAL),
    FAMILY_LAW_ASSISTANCE("Family Law Assistance", ServiceCategory.LEGAL),
    LEGAL_CLINIC("Legal Clinic", ServiceCategory.LEGAL),
    LEGAL_CONSULTATION("Legal Consultation", ServiceCategory.LEGAL),
    DOCUMENT_PREPARATION("Document Preparation", ServiceCategory.LEGAL),
    
    // Case Management
    CASE_MANAGEMENT("Case Management", ServiceCategory.CASE_MANAGEMENT),
    SERVICE_PLANNING("Service Planning", ServiceCategory.CASE_MANAGEMENT),
    RESOURCE_COORDINATION("Resource Coordination", ServiceCategory.CASE_MANAGEMENT),
    FOLLOW_UP_CONTACT("Follow-up Contact", ServiceCategory.CASE_MANAGEMENT),
    DISCHARGE_PLANNING("Discharge Planning", ServiceCategory.CASE_MANAGEMENT),
    INTAKE_ASSESSMENT("Intake Assessment", ServiceCategory.CASE_MANAGEMENT),
    COMPREHENSIVE_ASSESSMENT("Comprehensive Assessment", ServiceCategory.CASE_MANAGEMENT),
    
    // Financial Assistance
    EMERGENCY_FINANCIAL_ASSISTANCE("Emergency Financial Assistance", ServiceCategory.FINANCIAL),
    BENEFIT_ASSISTANCE("Benefit Assistance", ServiceCategory.FINANCIAL),
    EMPLOYMENT_ASSISTANCE("Employment Assistance", ServiceCategory.FINANCIAL),
    FINANCIAL_LITERACY("Financial Literacy", ServiceCategory.FINANCIAL),
    BUDGET_COUNSELING("Budget Counseling", ServiceCategory.FINANCIAL),
    
    // Healthcare & Medical
    MEDICAL_ADVOCACY("Medical Advocacy", ServiceCategory.HEALTHCARE),
    HEALTHCARE_COORDINATION("Healthcare Coordination", ServiceCategory.HEALTHCARE),
    MEDICAL_ACCOMPANIMENT("Medical Accompaniment", ServiceCategory.HEALTHCARE),
    HEALTH_EDUCATION("Health Education", ServiceCategory.HEALTHCARE),
    REPRODUCTIVE_HEALTH_SERVICES("Reproductive Health Services", ServiceCategory.HEALTHCARE),
    
    // Children & Family Services
    CHILDCARE("Childcare", ServiceCategory.CHILDREN_FAMILY),
    CHILDREN_COUNSELING("Children's Counseling", ServiceCategory.CHILDREN_FAMILY),
    PARENTING_SUPPORT("Parenting Support", ServiceCategory.CHILDREN_FAMILY),
    FAMILY_REUNIFICATION("Family Reunification", ServiceCategory.CHILDREN_FAMILY),
    SUPERVISED_VISITATION("Supervised Visitation", ServiceCategory.CHILDREN_FAMILY),
    
    // Education & Life Skills
    EDUCATION_SERVICES("Education Services", ServiceCategory.EDUCATION),
    GED_PREPARATION("GED Preparation", ServiceCategory.EDUCATION),
    LIFE_SKILLS_TRAINING("Life Skills Training", ServiceCategory.EDUCATION),
    JOB_TRAINING("Job Training", ServiceCategory.EDUCATION),
    COMPUTER_LITERACY("Computer Literacy", ServiceCategory.EDUCATION),
    
    // Transportation & Support
    TRANSPORTATION("Transportation", ServiceCategory.SUPPORT_SERVICES),
    INTERPRETATION_SERVICES("Interpretation Services", ServiceCategory.SUPPORT_SERVICES),
    CHILDCARE_DURING_SERVICES("Childcare During Services", ServiceCategory.SUPPORT_SERVICES),
    FOOD_ASSISTANCE("Food Assistance", ServiceCategory.SUPPORT_SERVICES),
    CLOTHING_ASSISTANCE("Clothing Assistance", ServiceCategory.SUPPORT_SERVICES),
    
    // Information & Referral
    INFORMATION_AND_REFERRAL("Information and Referral", ServiceCategory.INFORMATION),
    RESOURCE_REFERRAL("Resource Referral", ServiceCategory.INFORMATION),
    COMMUNITY_EDUCATION("Community Education", ServiceCategory.INFORMATION),
    PREVENTION_EDUCATION("Prevention Education", ServiceCategory.INFORMATION),
    
    // Specialized DV Services
    DV_COUNSELING("Domestic Violence Counseling", ServiceCategory.DV_SPECIFIC),
    DV_SUPPORT_GROUP("Domestic Violence Support Group", ServiceCategory.DV_SPECIFIC),
    DV_SAFETY_PLANNING("Domestic Violence Safety Planning", ServiceCategory.DV_SPECIFIC),
    STALKING_ADVOCACY("Stalking Advocacy", ServiceCategory.DV_SPECIFIC),
    
    // Sexual Assault Services
    SA_COUNSELING("Sexual Assault Counseling", ServiceCategory.SA_SPECIFIC),
    SA_CRISIS_INTERVENTION("Sexual Assault Crisis Intervention", ServiceCategory.SA_SPECIFIC),
    SANE_ACCOMPANIMENT("SANE Accompaniment", ServiceCategory.SA_SPECIFIC),
    SA_SUPPORT_GROUP("Sexual Assault Support Group", ServiceCategory.SA_SPECIFIC),
    
    // Other
    OTHER("Other", ServiceCategory.OTHER);

    private final String description;
    private final ServiceCategory category;

    ServiceType(String description, ServiceCategory category) {
        this.description = description;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    /**
     * Determine if this service type typically requires confidential handling
     */
    public boolean requiresConfidentialHandling() {
        return category == ServiceCategory.DV_SPECIFIC ||
               category == ServiceCategory.SA_SPECIFIC ||
               category == ServiceCategory.CRISIS_RESPONSE ||
               this == TRAUMA_COUNSELING ||
               this == INDIVIDUAL_COUNSELING ||
               this == LEGAL_ADVOCACY;
    }

    /**
     * Determine if this service type is billable/funded
     */
    public boolean isBillableService() {
        return category != ServiceCategory.INFORMATION &&
               this != FOLLOW_UP_CONTACT &&
               this != RESOURCE_REFERRAL;
    }

    /**
     * Get typical duration range for this service type (in minutes)
     */
    public DurationRange getTypicalDuration() {
        return switch (this) {
            case HOTLINE_CRISIS_CALL -> new DurationRange(15, 60);
            case INDIVIDUAL_COUNSELING, TRAUMA_COUNSELING -> new DurationRange(45, 60);
            case GROUP_COUNSELING, SUPPORT_GROUP -> new DurationRange(60, 90);
            case COURT_ACCOMPANIMENT -> new DurationRange(120, 480);
            case CASE_MANAGEMENT -> new DurationRange(30, 60);
            case INTAKE_ASSESSMENT, COMPREHENSIVE_ASSESSMENT -> new DurationRange(60, 120);
            case CRISIS_INTERVENTION -> new DurationRange(30, 120);
            case SAFETY_PLANNING -> new DurationRange(45, 90);
            case LEGAL_CONSULTATION -> new DurationRange(30, 60);
            case MEDICAL_ACCOMPANIMENT -> new DurationRange(60, 240);
            case FOLLOW_UP_CONTACT -> new DurationRange(10, 30);
            case INFORMATION_AND_REFERRAL -> new DurationRange(15, 30);
            default -> new DurationRange(30, 60);
        };
    }

    public record DurationRange(int minMinutes, int maxMinutes) {
        public boolean isValidDuration(int minutes) {
            return minutes >= minMinutes && minutes <= maxMinutes;
        }
    }
}