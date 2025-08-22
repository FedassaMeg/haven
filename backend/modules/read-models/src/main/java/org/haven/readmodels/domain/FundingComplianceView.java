package org.haven.readmodels.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class FundingComplianceView {
    private UUID fundingSourceId;
    private String fundingSourceName;
    private String fundingType;
    private BigDecimal totalBudget;
    private BigDecimal amountSpent;
    private BigDecimal amountCommitted;
    private BigDecimal amountAvailable;
    private Double utilizationPercentage;
    private LocalDate fundingPeriodStart;
    private LocalDate fundingPeriodEnd;
    private Integer daysRemaining;
    private ComplianceStatus complianceStatus;
    private List<DocumentationGap> documentationGaps;
    private List<PendingPayment> pendingPayments;
    private SpendDownTracking spendDownTracking;
    private LocalDate lastAuditDate;
    private String grantNumber;
    private String programArea;
    
    public enum ComplianceStatus {
        COMPLIANT,           // All requirements met
        ATTENTION_NEEDED,    // Minor issues to address
        AT_RISK,            // Significant compliance issues
        NON_COMPLIANT,      // Critical compliance failures
        UNDER_REVIEW        // Compliance review in progress
    }
    
    public static class DocumentationGap {
        private UUID clientId;
        private String clientName;
        private String missingDocument;
        private LocalDate dueDate;
        private Integer daysOverdue;
        private String caseNumber;
        
        // Constructor
        public DocumentationGap() {}
        
        public DocumentationGap(UUID clientId, String clientName, String missingDocument, 
                              LocalDate dueDate, String caseNumber) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.missingDocument = missingDocument;
            this.dueDate = dueDate;
            this.caseNumber = caseNumber;
            this.daysOverdue = calculateDaysOverdue(dueDate);
        }
        
        private Integer calculateDaysOverdue(LocalDate dueDate) {
            if (dueDate == null) return null;
            long days = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
            return days > 0 ? (int) days : null;
        }
        
        // Getters and Setters
        public UUID getClientId() { return clientId; }
        public void setClientId(UUID clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public String getMissingDocument() { return missingDocument; }
        public void setMissingDocument(String missingDocument) { this.missingDocument = missingDocument; }
        
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        
        public Integer getDaysOverdue() { return daysOverdue; }
        public void setDaysOverdue(Integer daysOverdue) { this.daysOverdue = daysOverdue; }
        
        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    }
    
    public static class SpendDownTracking {
        private BigDecimal quarterlyTarget;
        private BigDecimal quarterlySpent;
        private BigDecimal monthlyTarget;
        private BigDecimal monthlySpent;
        private BigDecimal dailyBurnRate;
        private BigDecimal projectedYearEndSpend;
        private Double spendVelocity;
        private Boolean isUnderspending;
        private Boolean isOverspending;
        private Integer daysToTargetReached;
        
        public SpendDownTracking() {}
        
        public BigDecimal getQuarterlyTarget() { return quarterlyTarget; }
        public void setQuarterlyTarget(BigDecimal quarterlyTarget) { this.quarterlyTarget = quarterlyTarget; }
        
        public BigDecimal getQuarterlySpent() { return quarterlySpent; }
        public void setQuarterlySpent(BigDecimal quarterlySpent) { this.quarterlySpent = quarterlySpent; }
        
        public BigDecimal getMonthlyTarget() { return monthlyTarget; }
        public void setMonthlyTarget(BigDecimal monthlyTarget) { this.monthlyTarget = monthlyTarget; }
        
        public BigDecimal getMonthlySpent() { return monthlySpent; }
        public void setMonthlySpent(BigDecimal monthlySpent) { this.monthlySpent = monthlySpent; }
        
        public BigDecimal getDailyBurnRate() { return dailyBurnRate; }
        public void setDailyBurnRate(BigDecimal dailyBurnRate) { this.dailyBurnRate = dailyBurnRate; }
        
        public BigDecimal getProjectedYearEndSpend() { return projectedYearEndSpend; }
        public void setProjectedYearEndSpend(BigDecimal projectedYearEndSpend) { this.projectedYearEndSpend = projectedYearEndSpend; }
        
        public Double getSpendVelocity() { return spendVelocity; }
        public void setSpendVelocity(Double spendVelocity) { this.spendVelocity = spendVelocity; }
        
        public Boolean getIsUnderspending() { return isUnderspending; }
        public void setIsUnderspending(Boolean isUnderspending) { this.isUnderspending = isUnderspending; }
        
        public Boolean getIsOverspending() { return isOverspending; }
        public void setIsOverspending(Boolean isOverspending) { this.isOverspending = isOverspending; }
        
        public Integer getDaysToTargetReached() { return daysToTargetReached; }
        public void setDaysToTargetReached(Integer daysToTargetReached) { this.daysToTargetReached = daysToTargetReached; }
    }

    public static class PendingPayment {
        private UUID paymentId;
        private UUID clientId;
        private String clientName;
        private BigDecimal amount;
        private String paymentType;
        private LocalDate requestDate;
        private LocalDate dueDate;
        private String approvalStatus;
        private String vendorName;
        
        // Constructor
        public PendingPayment() {}
        
        // Getters and Setters
        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
        
        public UUID getClientId() { return clientId; }
        public void setClientId(UUID clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getPaymentType() { return paymentType; }
        public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
        
        public LocalDate getRequestDate() { return requestDate; }
        public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }
        
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        
        public String getApprovalStatus() { return approvalStatus; }
        public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
        
        public String getVendorName() { return vendorName; }
        public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    }
    
    // Constructor
    public FundingComplianceView() {}
    
    // Methods
    public BigDecimal calculateAmountAvailable() {
        if (totalBudget == null || amountSpent == null || amountCommitted == null) {
            return BigDecimal.ZERO;
        }
        return totalBudget.subtract(amountSpent).subtract(amountCommitted);
    }
    
    public Double calculateUtilizationPercentage() {
        if (totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        BigDecimal spent = amountSpent != null ? amountSpent : BigDecimal.ZERO;
        return spent.divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP)
                   .multiply(new BigDecimal(100)).doubleValue();
    }
    
    public Double calculateBurnRate() {
        if (fundingPeriodStart == null || totalBudget == null) {
            return 0.0;
        }
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(fundingPeriodStart, LocalDate.now());
        if (daysElapsed <= 0) return 0.0;
        
        BigDecimal spent = amountSpent != null ? amountSpent : BigDecimal.ZERO;
        return spent.divide(new BigDecimal(daysElapsed), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public boolean isUnderspent() {
        Double utilization = calculateUtilizationPercentage();
        if (fundingPeriodEnd == null || utilization == null) return false;
        
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(fundingPeriodStart, fundingPeriodEnd);
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(fundingPeriodStart, LocalDate.now());
        
        if (totalDays <= 0 || daysElapsed <= 0) return false;
        
        double expectedUtilization = (double) daysElapsed / totalDays * 100;
        return utilization < (expectedUtilization - 10); // 10% threshold
    }
    
    // Getters and Setters
    public UUID getFundingSourceId() { return fundingSourceId; }
    public void setFundingSourceId(UUID fundingSourceId) { this.fundingSourceId = fundingSourceId; }
    
    public String getFundingSourceName() { return fundingSourceName; }
    public void setFundingSourceName(String fundingSourceName) { this.fundingSourceName = fundingSourceName; }
    
    public String getFundingType() { return fundingType; }
    public void setFundingType(String fundingType) { this.fundingType = fundingType; }
    
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    
    public BigDecimal getAmountSpent() { return amountSpent; }
    public void setAmountSpent(BigDecimal amountSpent) { this.amountSpent = amountSpent; }
    
    public BigDecimal getAmountCommitted() { return amountCommitted; }
    public void setAmountCommitted(BigDecimal amountCommitted) { this.amountCommitted = amountCommitted; }
    
    public BigDecimal getAmountAvailable() { return amountAvailable; }
    public void setAmountAvailable(BigDecimal amountAvailable) { this.amountAvailable = amountAvailable; }
    
    public Double getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(Double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
    
    public LocalDate getFundingPeriodStart() { return fundingPeriodStart; }
    public void setFundingPeriodStart(LocalDate fundingPeriodStart) { this.fundingPeriodStart = fundingPeriodStart; }
    
    public LocalDate getFundingPeriodEnd() { return fundingPeriodEnd; }
    public void setFundingPeriodEnd(LocalDate fundingPeriodEnd) { this.fundingPeriodEnd = fundingPeriodEnd; }
    
    public Integer getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }
    
    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }
    
    public List<DocumentationGap> getDocumentationGaps() { return documentationGaps; }
    public void setDocumentationGaps(List<DocumentationGap> documentationGaps) { this.documentationGaps = documentationGaps; }
    
    public List<PendingPayment> getPendingPayments() { return pendingPayments; }
    public void setPendingPayments(List<PendingPayment> pendingPayments) { this.pendingPayments = pendingPayments; }
    
    public SpendDownTracking getSpendDownTracking() { return spendDownTracking; }
    public void setSpendDownTracking(SpendDownTracking spendDownTracking) { this.spendDownTracking = spendDownTracking; }
    
    public LocalDate getLastAuditDate() { return lastAuditDate; }
    public void setLastAuditDate(LocalDate lastAuditDate) { this.lastAuditDate = lastAuditDate; }
    
    public String getGrantNumber() { return grantNumber; }
    public void setGrantNumber(String grantNumber) { this.grantNumber = grantNumber; }
    
    public String getProgramArea() { return programArea; }
    public void setProgramArea(String programArea) { this.programArea = programArea; }
}