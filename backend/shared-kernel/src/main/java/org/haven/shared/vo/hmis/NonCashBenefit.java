package org.haven.shared.vo.hmis;

/**
 * HMIS Non-Cash Benefits - FY2024 Data Standards
 * Aligns with HMIS Data Elements 4.03 Non-Cash Benefits
 */
public enum NonCashBenefit {
    
    SUPPLEMENTAL_NUTRITION_ASSISTANCE_PROGRAM(1, "Supplemental Nutrition Assistance Program (Food Stamps)"),
    SPECIAL_SUPPLEMENTAL_NUTRITION_PROGRAM_WIC(2, "Special Supplemental Nutrition Program for WIC"),
    TANF_CHILD_CARE_SERVICES(3, "TANF Child Care services"),
    TANF_TRANSPORTATION_SERVICES(4, "TANF Transportation services"),
    OTHER_TANF_SERVICES(5, "Other TANF-funded services"),
    OTHER_SOURCE(6, "Other source");
    
    private final int hmisValue;
    private final String description;
    
    NonCashBenefit(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }
    
    public int getHmisValue() {
        return hmisValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isKnownBenefit() {
        return true; // All enum values are known benefits now
    }
    
    public boolean isNutritionBenefit() {
        return this == SUPPLEMENTAL_NUTRITION_ASSISTANCE_PROGRAM ||
               this == SPECIAL_SUPPLEMENTAL_NUTRITION_PROGRAM_WIC;
    }
    
    public boolean isTanfBenefit() {
        return this == TANF_CHILD_CARE_SERVICES ||
               this == TANF_TRANSPORTATION_SERVICES ||
               this == OTHER_TANF_SERVICES;
    }
}