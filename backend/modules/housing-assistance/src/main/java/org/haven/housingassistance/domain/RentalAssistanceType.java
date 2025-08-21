package org.haven.housingassistance.domain;

/**
 * Types of rental assistance aligned with HUD standards
 */
public enum RentalAssistanceType {
    // Rapid Re-Housing (RRH)
    RRH_SHORT_TERM_RENTAL_ASSISTANCE,    // Up to 3 months
    RRH_MEDIUM_TERM_RENTAL_ASSISTANCE,   // 4-24 months
    
    // Transitional Housing (TH)  
    TH_TEMPORARY_HOUSING,                 // Up to 24 months
    TH_SUPPORTIVE_SERVICES,
    
    // Permanent Supportive Housing (PSH)
    PSH_RENTAL_ASSISTANCE,
    PSH_SUPPORTIVE_SERVICES,
    
    // Emergency Solutions Grant (ESG)
    ESG_RENTAL_ASSISTANCE,
    ESG_RENTAL_APPLICATION_FEES,
    ESG_SECURITY_DEPOSITS,
    ESG_UTILITY_DEPOSITS,
    ESG_UTILITY_PAYMENTS,
    ESG_MOVING_COSTS,
    
    // HOME TBRA
    HOME_TENANT_BASED_RENTAL_ASSISTANCE,
    
    // Other assistance
    UTILITY_ASSISTANCE,
    SECURITY_DEPOSIT_ASSISTANCE,
    FIRST_MONTH_RENT,
    LAST_MONTH_RENT,
    MOVING_COSTS_ASSISTANCE,
    APPLICATION_FEE_ASSISTANCE
}