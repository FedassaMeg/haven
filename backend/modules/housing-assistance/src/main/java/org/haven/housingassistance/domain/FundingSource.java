package org.haven.housingassistance.domain;

import org.haven.shared.vo.CodeableConcept;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Represents a funding source with specific restrictions and requirements
 */
public class FundingSource {
    private String fundingSourceCode;
    private String fundingSourceName;
    private CodeableConcept fundingType; // ESG, CoC, HOME, VASH, etc.
    private BigDecimal totalAllocation;
    private BigDecimal remainingBalance;
    private LocalDate availabilityStartDate;
    private LocalDate availabilityEndDate;
    private Set<RentalAssistanceType> allowedAssistanceTypes;
    private Set<String> eligibilityCriteria;
    private BigDecimal maxMonthlyAmount;
    private Integer maxDurationMonths;
    private Boolean requiresTwoPersonApproval;
    private String grantNumber;
    private String administeredBy;
    
    public FundingSource(String fundingSourceCode, String fundingSourceName, 
                        CodeableConcept fundingType, BigDecimal totalAllocation) {
        this.fundingSourceCode = fundingSourceCode;
        this.fundingSourceName = fundingSourceName;
        this.fundingType = fundingType;
        this.totalAllocation = totalAllocation;
        this.remainingBalance = totalAllocation;
        this.requiresTwoPersonApproval = true; // Default to requiring dual approval
    }
    
    public boolean canProvideAssistance(RentalAssistanceType assistanceType, BigDecimal amount) {
        return allowedAssistanceTypes.contains(assistanceType) && 
               remainingBalance.compareTo(amount) >= 0 &&
               (maxMonthlyAmount == null || amount.compareTo(maxMonthlyAmount) <= 0);
    }
    
    public void reserveFunds(BigDecimal amount) {
        if (remainingBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds available");
        }
        this.remainingBalance = remainingBalance.subtract(amount);
    }
    
    public void releaseFunds(BigDecimal amount) {
        this.remainingBalance = remainingBalance.add(amount);
    }
    
    // Getters
    public String getFundingSourceCode() { return fundingSourceCode; }
    public String getFundingSourceName() { return fundingSourceName; }
    public CodeableConcept getFundingType() { return fundingType; }
    public BigDecimal getTotalAllocation() { return totalAllocation; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public LocalDate getAvailabilityStartDate() { return availabilityStartDate; }
    public LocalDate getAvailabilityEndDate() { return availabilityEndDate; }
    public Set<RentalAssistanceType> getAllowedAssistanceTypes() { return allowedAssistanceTypes; }
    public Set<String> getEligibilityCriteria() { return eligibilityCriteria; }
    public BigDecimal getMaxMonthlyAmount() { return maxMonthlyAmount; }
    public Integer getMaxDurationMonths() { return maxDurationMonths; }
    public Boolean getRequiresTwoPersonApproval() { return requiresTwoPersonApproval; }
    public String getGrantNumber() { return grantNumber; }
    public String getAdministeredBy() { return administeredBy; }
}