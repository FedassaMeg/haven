package org.haven.financialassistance.domain;

/**
 * Types of financial assistance beyond housing
 */
public enum FinancialAssistanceType {
    // Emergency assistance
    EMERGENCY_CASH_ASSISTANCE,
    EMERGENCY_FOOD_ASSISTANCE,
    EMERGENCY_CLOTHING,
    EMERGENCY_MEDICAL_COSTS,
    
    // Transportation
    TRANSPORTATION_ASSISTANCE,
    BUS_PASSES,
    GAS_VOUCHERS,
    CAR_REPAIR_ASSISTANCE,
    
    // Employment support
    EMPLOYMENT_ASSISTANCE,
    JOB_TRAINING_COSTS,
    WORK_UNIFORMS,
    CHILDCARE_FOR_EMPLOYMENT,
    
    // Education support
    EDUCATION_ASSISTANCE,
    SCHOOL_SUPPLIES,
    TUITION_ASSISTANCE,
    
    // Legal assistance
    LEGAL_FEES,
    COURT_COSTS,
    DOCUMENT_FEES,
    
    // Healthcare
    MEDICAL_COPAYS,
    PRESCRIPTION_ASSISTANCE,
    MENTAL_HEALTH_SERVICES,
    DENTAL_CARE,
    
    // Family support
    CHILDCARE_ASSISTANCE,
    CHILD_SUPPORT_PAYMENTS,
    FAMILY_REUNIFICATION_COSTS,
    
    // Basic needs
    HOUSEHOLD_GOODS,
    FURNITURE_ASSISTANCE,
    APPLIANCE_ASSISTANCE,
    
    // Technology
    PHONE_SERVICE,
    INTERNET_SERVICE,
    COMPUTER_ASSISTANCE,
    
    // Other
    OTHER_ASSISTANCE
}