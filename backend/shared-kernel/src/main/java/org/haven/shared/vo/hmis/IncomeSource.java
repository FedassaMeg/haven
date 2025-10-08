package org.haven.shared.vo.hmis;

/**
 * HMIS Income Sources - FY2024 Data Standards
 * Aligns with HMIS Data Elements 4.02 Income and Sources
 */
public enum IncomeSource {
    
    // Employment Income
    EARNED_INCOME(1, "Earned income"),
    
    // Benefits
    UNEMPLOYMENT_INSURANCE(2, "Unemployment insurance"),
    SUPPLEMENTAL_SECURITY_INCOME(3, "Supplemental Security Income (SSI)"),
    SOCIAL_SECURITY_DISABILITY_INSURANCE(4, "Social Security Disability Insurance (SSDI)"),
    VA_DISABILITY_SERVICE_CONNECTED(5, "VA Disability Service-Connected"),
    VA_DISABILITY_NON_SERVICE_CONNECTED(6, "VA Disability Non-Service-Connected"),
    PRIVATE_DISABILITY(7, "Private disability insurance"),
    WORKERS_COMPENSATION(8, "Worker's compensation"),
    TANF(9, "TANF"),
    GENERAL_ASSISTANCE(10, "General Assistance (GA)"),
    RETIREMENT_SOCIAL_SECURITY(11, "Retirement income from Social Security"),
    PENSION_RETIREMENT_FROM_FORMER_JOB(12, "Pension or retirement income from former job"),
    CHILD_SUPPORT(13, "Child support"),
    ALIMONY(14, "Alimony or other spousal support"),
    OTHER_SOURCE(15, "Other source");
    
    private final int hmisValue;
    private final String description;
    
    IncomeSource(int hmisValue, String description) {
        this.hmisValue = hmisValue;
        this.description = description;
    }
    
    public int getHmisValue() {
        return hmisValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isKnownSource() {
        return true; // All enum values are known sources now
    }
    
    public boolean isBenefitIncome() {
        return this == UNEMPLOYMENT_INSURANCE ||
               this == SUPPLEMENTAL_SECURITY_INCOME ||
               this == SOCIAL_SECURITY_DISABILITY_INSURANCE ||
               this == VA_DISABILITY_SERVICE_CONNECTED ||
               this == VA_DISABILITY_NON_SERVICE_CONNECTED ||
               this == PRIVATE_DISABILITY ||
               this == WORKERS_COMPENSATION ||
               this == TANF ||
               this == GENERAL_ASSISTANCE ||
               this == RETIREMENT_SOCIAL_SECURITY ||
               this == PENSION_RETIREMENT_FROM_FORMER_JOB;
    }
    
    public boolean isEarnedIncome() {
        return this == EARNED_INCOME;
    }
    
    public boolean isOtherIncome() {
        return this == CHILD_SUPPORT ||
               this == ALIMONY ||
               this == OTHER_SOURCE;
    }
}