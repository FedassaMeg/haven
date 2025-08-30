package org.haven.shared.vo.hmis;

/**
 * HMIS Health Insurance - FY2024 Data Standards
 * Aligns with HMIS Data Elements 4.04 Health Insurance
 */
public enum HealthInsurance {
    
    MEDICAID(1, "Medicaid"),
    MEDICARE(2, "Medicare"),
    SCHIP(3, "State Children's Health Insurance Program"),
    VA_MEDICAL_SERVICES(4, "VA Medical Services"),
    EMPLOYER_PROVIDED(5, "Employer-provided health insurance"),
    COBRA(6, "COBRA (Consolidated Omnibus Budget Reconciliation Act)"),
    PRIVATE_PAY(7, "Private payment (purchased directly from insurance company)"),
    STATE_ADULT_HEALTH_INSURANCE(8, "State adult health insurance"),
    INDIAN_HEALTH_SERVICE(9, "Indian Health Service"),
    OTHER_INSURANCE(10, "Other insurance"),
    
    // Backward compatibility
    @Deprecated
    HEALTH_INSURANCE_PURCHASED_DIRECTLY(7, "Health insurance purchased directly from insurance company"),
    
    // Standard responses  
    CLIENT_DOESNT_KNOW(8, "Client doesn't know"),
    CLIENT_REFUSED(9, "Client refused"),
    DATA_NOT_COLLECTED(99, "Data not collected");
    
    private final int hmisValue;
    private final String description;
    
    HealthInsurance(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }
    
    public int getHmisValue() {
        return hmisValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isKnownInsurance() {
        return this != CLIENT_DOESNT_KNOW && 
               this != CLIENT_REFUSED && 
               this != DATA_NOT_COLLECTED;
    }
    
    public boolean isGovernmentInsurance() {
        return this == MEDICAID ||
               this == MEDICARE ||
               this == SCHIP ||
               this == VA_MEDICAL_SERVICES;
    }
    
    public boolean isPrivateInsurance() {
        return this == EMPLOYER_PROVIDED ||
               this == COBRA ||
               this == PRIVATE_PAY ||
               this == HEALTH_INSURANCE_PURCHASED_DIRECTLY || // backward compatibility
               this == OTHER_INSURANCE;
    }
    
    /**
     * Check if this is a state-provided insurance
     */
    public boolean isStateInsurance() {
        return this == MEDICAID ||
               this == SCHIP ||
               this == STATE_ADULT_HEALTH_INSURANCE;
    }
    
    /**
     * Check if this is federal government insurance
     */
    public boolean isFederalInsurance() {
        return this == MEDICARE ||
               this == VA_MEDICAL_SERVICES ||
               this == INDIAN_HEALTH_SERVICE;
    }
    
    /**
     * Map insurance type to boolean field name for database storage
     */
    public String toBooleanFieldName() {
        return switch (this) {
            case MEDICAID -> "medicaid";
            case MEDICARE -> "medicare";
            case SCHIP -> "schip";
            case VA_MEDICAL_SERVICES -> "vha_medical_services";
            case EMPLOYER_PROVIDED -> "employer_provided";
            case COBRA -> "cobra";
            case PRIVATE_PAY, HEALTH_INSURANCE_PURCHASED_DIRECTLY -> "private_pay";
            case STATE_ADULT_HEALTH_INSURANCE -> "state_adult_health_insurance";
            case INDIAN_HEALTH_SERVICE -> "indian_health_service";
            case OTHER_INSURANCE -> "other_insurance";
            default -> throw new IllegalArgumentException("No boolean field for: " + this);
        };
    }
}