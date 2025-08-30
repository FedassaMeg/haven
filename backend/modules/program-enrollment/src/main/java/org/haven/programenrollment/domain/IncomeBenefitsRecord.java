package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HMIS-compliant Income and Sources Record
 * Represents a single data collection event for income/benefits per HMIS FY2024 standards
 * Supports multiple records per enrollment for lifecycle events (start, update, annual, exit)
 */
public class IncomeBenefitsRecord {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate informationDate;
    private InformationDate recordType;
    
    // Overall income status (4.02.1)
    private HmisFivePointResponse incomeFromAnySource;
    private Integer totalMonthlyIncome;
    
    // Overall benefits status (4.03.1)
    private HmisFivePointResponse benefitsFromAnySource;
    
    // Individual income sources with amounts (4.02.2-4.02.A)
    private DisabilityType earnedIncome;
    private Integer earnedIncomeAmount;
    
    private DisabilityType unemploymentIncome;
    private Integer unemploymentIncomeAmount;
    
    private DisabilityType supplementalSecurityIncome;
    private Integer supplementalSecurityIncomeAmount;
    
    private DisabilityType socialSecurityDisabilityIncome;
    private Integer socialSecurityDisabilityIncomeAmount;
    
    private DisabilityType vaDisabilityServiceConnected;
    private Integer vaDisabilityServiceConnectedAmount;
    
    private DisabilityType vaDisabilityNonServiceConnected;
    private Integer vaDisabilityNonServiceConnectedAmount;
    
    private DisabilityType privateDisabilityIncome;
    private Integer privateDisabilityIncomeAmount;
    
    private DisabilityType workersCompensation;
    private Integer workersCompensationAmount;
    
    private DisabilityType tanfIncome;
    private Integer tanfIncomeAmount;
    
    private DisabilityType generalAssistance;
    private Integer generalAssistanceAmount;
    
    private DisabilityType socialSecurityRetirement;
    private Integer socialSecurityRetirementAmount;
    
    private DisabilityType pensionFromFormerJob;
    private Integer pensionFromFormerJobAmount;
    
    private DisabilityType childSupport;
    private Integer childSupportAmount;
    
    private DisabilityType alimony;
    private Integer alimonyAmount;
    
    private DisabilityType otherIncomeSource;
    private Integer otherIncomeAmount;
    private String otherIncomeSourceIdentify; // Free text field
    
    // Individual non-cash benefits (4.03.2-4.03.A)
    private HmisFivePointResponse snap;
    private HmisFivePointResponse wic;
    private HmisFivePointResponse tanfChildCare;
    private HmisFivePointResponse tanfTransportation;
    private HmisFivePointResponse otherTanf;
    private HmisFivePointResponse otherBenefitsSource;
    private String otherBenefitsSpecify; // Free text field
    
    // Audit fields
    private String collectedBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public IncomeBenefitsRecord() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Create a new income record at project start
     */
    public static IncomeBenefitsRecord createAtProjectStart(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate entryDate,
            HmisFivePointResponse incomeFromAnySource,
            HmisFivePointResponse benefitsFromAnySource,
            String collectedBy) {
        
        IncomeBenefitsRecord record = new IncomeBenefitsRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = entryDate;
        record.recordType = InformationDate.START_OF_PROJECT;
        record.incomeFromAnySource = incomeFromAnySource;
        record.benefitsFromAnySource = benefitsFromAnySource;
        record.collectedBy = collectedBy;
        
        // Initialize all sources as data not collected
        record.initializeSourcesAsNotCollected();
        
        return record;
    }
    
    /**
     * Create an update record due to change in circumstances
     */
    public static IncomeBenefitsRecord createUpdate(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate changeDate,
            HmisFivePointResponse incomeFromAnySource,
            HmisFivePointResponse benefitsFromAnySource,
            String collectedBy) {
        
        IncomeBenefitsRecord record = new IncomeBenefitsRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = changeDate;
        record.recordType = InformationDate.UPDATE;
        record.incomeFromAnySource = incomeFromAnySource;
        record.benefitsFromAnySource = benefitsFromAnySource;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsNotCollected();
        
        return record;
    }
    
    /**
     * Create annual assessment record
     */
    public static IncomeBenefitsRecord createAnnualAssessment(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate assessmentDate,
            HmisFivePointResponse incomeFromAnySource,
            HmisFivePointResponse benefitsFromAnySource,
            String collectedBy) {
        
        IncomeBenefitsRecord record = new IncomeBenefitsRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = assessmentDate;
        record.recordType = InformationDate.ANNUAL_ASSESSMENT;
        record.incomeFromAnySource = incomeFromAnySource;
        record.benefitsFromAnySource = benefitsFromAnySource;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsNotCollected();
        
        return record;
    }
    
    /**
     * Create exit record
     */
    public static IncomeBenefitsRecord createAtProjectExit(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate exitDate,
            HmisFivePointResponse incomeFromAnySource,
            HmisFivePointResponse benefitsFromAnySource,
            String collectedBy) {
        
        IncomeBenefitsRecord record = new IncomeBenefitsRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = exitDate;
        record.recordType = InformationDate.EXIT;
        record.incomeFromAnySource = incomeFromAnySource;
        record.benefitsFromAnySource = benefitsFromAnySource;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsNotCollected();
        
        return record;
    }
    
    /**
     * Create record when minor turns 18
     */
    public static IncomeBenefitsRecord createMinorTurning18(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate birthdayDate,
            HmisFivePointResponse incomeFromAnySource,
            HmisFivePointResponse benefitsFromAnySource,
            String collectedBy) {
        
        IncomeBenefitsRecord record = new IncomeBenefitsRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = birthdayDate;
        record.recordType = InformationDate.MINOR_TURNING_18;
        record.incomeFromAnySource = incomeFromAnySource;
        record.benefitsFromAnySource = benefitsFromAnySource;
        record.collectedBy = collectedBy;
        
        record.initializeSourcesAsNotCollected();
        
        return record;
    }
    
    private void initializeSourcesAsNotCollected() {
        // Initialize income sources
        this.earnedIncome = DisabilityType.DATA_NOT_COLLECTED;
        this.unemploymentIncome = DisabilityType.DATA_NOT_COLLECTED;
        this.supplementalSecurityIncome = DisabilityType.DATA_NOT_COLLECTED;
        this.socialSecurityDisabilityIncome = DisabilityType.DATA_NOT_COLLECTED;
        this.vaDisabilityServiceConnected = DisabilityType.DATA_NOT_COLLECTED;
        this.vaDisabilityNonServiceConnected = DisabilityType.DATA_NOT_COLLECTED;
        this.privateDisabilityIncome = DisabilityType.DATA_NOT_COLLECTED;
        this.workersCompensation = DisabilityType.DATA_NOT_COLLECTED;
        this.tanfIncome = DisabilityType.DATA_NOT_COLLECTED;
        this.generalAssistance = DisabilityType.DATA_NOT_COLLECTED;
        this.socialSecurityRetirement = DisabilityType.DATA_NOT_COLLECTED;
        this.pensionFromFormerJob = DisabilityType.DATA_NOT_COLLECTED;
        this.childSupport = DisabilityType.DATA_NOT_COLLECTED;
        this.alimony = DisabilityType.DATA_NOT_COLLECTED;
        this.otherIncomeSource = DisabilityType.DATA_NOT_COLLECTED;
        
        // Initialize benefits
        this.snap = HmisFivePointResponse.DATA_NOT_COLLECTED;
        this.wic = HmisFivePointResponse.DATA_NOT_COLLECTED;
        this.tanfChildCare = HmisFivePointResponse.DATA_NOT_COLLECTED;
        this.tanfTransportation = HmisFivePointResponse.DATA_NOT_COLLECTED;
        this.otherTanf = HmisFivePointResponse.DATA_NOT_COLLECTED;
        this.otherBenefitsSource = HmisFivePointResponse.DATA_NOT_COLLECTED;
    }
    
    /**
     * Update individual income source
     */
    public void updateIncomeSource(IncomeSource source, DisabilityType hasSource, Integer amount) {
        this.updatedAt = Instant.now();
        
        switch (source) {
            case EARNED_INCOME -> {
                this.earnedIncome = hasSource;
                this.earnedIncomeAmount = amount;
            }
            case UNEMPLOYMENT_INSURANCE -> {
                this.unemploymentIncome = hasSource;
                this.unemploymentIncomeAmount = amount;
            }
            case SUPPLEMENTAL_SECURITY_INCOME -> {
                this.supplementalSecurityIncome = hasSource;
                this.supplementalSecurityIncomeAmount = amount;
            }
            case SOCIAL_SECURITY_DISABILITY_INSURANCE -> {
                this.socialSecurityDisabilityIncome = hasSource;
                this.socialSecurityDisabilityIncomeAmount = amount;
            }
            case VA_DISABILITY_SERVICE_CONNECTED -> {
                this.vaDisabilityServiceConnected = hasSource;
                this.vaDisabilityServiceConnectedAmount = amount;
            }
            case VA_DISABILITY_NON_SERVICE_CONNECTED -> {
                this.vaDisabilityNonServiceConnected = hasSource;
                this.vaDisabilityNonServiceConnectedAmount = amount;
            }
            case PRIVATE_DISABILITY -> {
                this.privateDisabilityIncome = hasSource;
                this.privateDisabilityIncomeAmount = amount;
            }
            case WORKERS_COMPENSATION -> {
                this.workersCompensation = hasSource;
                this.workersCompensationAmount = amount;
            }
            case TANF -> {
                this.tanfIncome = hasSource;
                this.tanfIncomeAmount = amount;
            }
            case GENERAL_ASSISTANCE -> {
                this.generalAssistance = hasSource;
                this.generalAssistanceAmount = amount;
            }
            case RETIREMENT_SOCIAL_SECURITY -> {
                this.socialSecurityRetirement = hasSource;
                this.socialSecurityRetirementAmount = amount;
            }
            case PENSION_RETIREMENT_FROM_FORMER_JOB -> {
                this.pensionFromFormerJob = hasSource;
                this.pensionFromFormerJobAmount = amount;
            }
            case CHILD_SUPPORT -> {
                this.childSupport = hasSource;
                this.childSupportAmount = amount;
            }
            case ALIMONY -> {
                this.alimony = hasSource;
                this.alimonyAmount = amount;
            }
            case OTHER_SOURCE -> {
                this.otherIncomeSource = hasSource;
                this.otherIncomeAmount = amount;
            }
            default -> throw new IllegalArgumentException("Unknown income source: " + source);
        }
    }
    
    /**
     * Update other income source with specify text
     */
    public void updateOtherIncomeSource(DisabilityType hasSource, Integer amount, String specify) {
        this.otherIncomeSource = hasSource;
        this.otherIncomeAmount = amount;
        this.otherIncomeSourceIdentify = specify;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update individual non-cash benefit
     */
    public void updateNonCashBenefit(NonCashBenefit benefit, HmisFivePointResponse hasBenefit) {
        this.updatedAt = Instant.now();
        
        switch (benefit) {
            case SUPPLEMENTAL_NUTRITION_ASSISTANCE_PROGRAM -> this.snap = hasBenefit;
            case SPECIAL_SUPPLEMENTAL_NUTRITION_PROGRAM_WIC -> this.wic = hasBenefit;
            case TANF_CHILD_CARE_SERVICES -> this.tanfChildCare = hasBenefit;
            case TANF_TRANSPORTATION_SERVICES -> this.tanfTransportation = hasBenefit;
            case OTHER_TANF_SERVICES -> this.otherTanf = hasBenefit;
            case OTHER_SOURCE -> this.otherBenefitsSource = hasBenefit;
            default -> throw new IllegalArgumentException("Unknown benefit type: " + benefit);
        }
    }
    
    /**
     * Update other benefits source with specify text
     */
    public void updateOtherBenefitsSource(HmisFivePointResponse hasBenefit, String specify) {
        this.otherBenefitsSource = hasBenefit;
        this.otherBenefitsSpecify = specify;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update total monthly income
     */
    public void updateTotalMonthlyIncome(Integer totalAmount) {
        this.totalMonthlyIncome = totalAmount;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if record meets HMIS data quality standards
     */
    public boolean meetsDataQuality() {
        // Income validation
        if (incomeFromAnySource == null || !incomeFromAnySource.isKnownResponse()) {
            return false;
        }
        
        // If no income, total should be 0 or null
        if (incomeFromAnySource.isNegative()) {
            if (totalMonthlyIncome != null && totalMonthlyIncome > 0) {
                return false;
            }
        }
        
        // If has income, at least one source should be identified and total > 0
        if (incomeFromAnySource.isAffirmative()) {
            if (totalMonthlyIncome == null || totalMonthlyIncome <= 0) {
                return false;
            }
            if (!hasAtLeastOneIncomeSource()) {
                return false;
            }
        }
        
        // Benefits validation
        if (benefitsFromAnySource == null || !benefitsFromAnySource.isKnownResponse()) {
            return false;
        }
        
        // If has benefits, at least one benefit should be identified
        if (benefitsFromAnySource.isAffirmative()) {
            if (!hasAtLeastOneBenefit()) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean hasAtLeastOneIncomeSource() {
        return (earnedIncome != null && earnedIncome.isAffirmative()) ||
               (unemploymentIncome != null && unemploymentIncome.isAffirmative()) ||
               (supplementalSecurityIncome != null && supplementalSecurityIncome.isAffirmative()) ||
               (socialSecurityDisabilityIncome != null && socialSecurityDisabilityIncome.isAffirmative()) ||
               (vaDisabilityServiceConnected != null && vaDisabilityServiceConnected.isAffirmative()) ||
               (vaDisabilityNonServiceConnected != null && vaDisabilityNonServiceConnected.isAffirmative()) ||
               (privateDisabilityIncome != null && privateDisabilityIncome.isAffirmative()) ||
               (workersCompensation != null && workersCompensation.isAffirmative()) ||
               (tanfIncome != null && tanfIncome.isAffirmative()) ||
               (generalAssistance != null && generalAssistance.isAffirmative()) ||
               (socialSecurityRetirement != null && socialSecurityRetirement.isAffirmative()) ||
               (pensionFromFormerJob != null && pensionFromFormerJob.isAffirmative()) ||
               (childSupport != null && childSupport.isAffirmative()) ||
               (alimony != null && alimony.isAffirmative()) ||
               (otherIncomeSource != null && otherIncomeSource.isAffirmative());
    }
    
    private boolean hasAtLeastOneBenefit() {
        return (snap != null && snap.isAffirmative()) ||
               (wic != null && wic.isAffirmative()) ||
               (tanfChildCare != null && tanfChildCare.isAffirmative()) ||
               (tanfTransportation != null && tanfTransportation.isAffirmative()) ||
               (otherTanf != null && otherTanf.isAffirmative()) ||
               (otherBenefitsSource != null && otherBenefitsSource.isAffirmative());
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public InformationDate getRecordType() { return recordType; }
    public HmisFivePointResponse getIncomeFromAnySource() { return incomeFromAnySource; }
    public Integer getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public HmisFivePointResponse getBenefitsFromAnySource() { return benefitsFromAnySource; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    // Individual source getters
    public DisabilityType getEarnedIncome() { return earnedIncome; }
    public Integer getEarnedIncomeAmount() { return earnedIncomeAmount; }
    public DisabilityType getUnemploymentIncome() { return unemploymentIncome; }
    public Integer getUnemploymentIncomeAmount() { return unemploymentIncomeAmount; }
    public DisabilityType getSupplementalSecurityIncome() { return supplementalSecurityIncome; }
    public Integer getSupplementalSecurityIncomeAmount() { return supplementalSecurityIncomeAmount; }
    public DisabilityType getSocialSecurityDisabilityIncome() { return socialSecurityDisabilityIncome; }
    public Integer getSocialSecurityDisabilityIncomeAmount() { return socialSecurityDisabilityIncomeAmount; }
    public DisabilityType getVaDisabilityServiceConnected() { return vaDisabilityServiceConnected; }
    public Integer getVaDisabilityServiceConnectedAmount() { return vaDisabilityServiceConnectedAmount; }
    public DisabilityType getVaDisabilityNonServiceConnected() { return vaDisabilityNonServiceConnected; }
    public Integer getVaDisabilityNonServiceConnectedAmount() { return vaDisabilityNonServiceConnectedAmount; }
    public DisabilityType getPrivateDisabilityIncome() { return privateDisabilityIncome; }
    public Integer getPrivateDisabilityIncomeAmount() { return privateDisabilityIncomeAmount; }
    public DisabilityType getWorkersCompensation() { return workersCompensation; }
    public Integer getWorkersCompensationAmount() { return workersCompensationAmount; }
    public DisabilityType getTanfIncome() { return tanfIncome; }
    public Integer getTanfIncomeAmount() { return tanfIncomeAmount; }
    public DisabilityType getGeneralAssistance() { return generalAssistance; }
    public Integer getGeneralAssistanceAmount() { return generalAssistanceAmount; }
    public DisabilityType getSocialSecurityRetirement() { return socialSecurityRetirement; }
    public Integer getSocialSecurityRetirementAmount() { return socialSecurityRetirementAmount; }
    public DisabilityType getPensionFromFormerJob() { return pensionFromFormerJob; }
    public Integer getPensionFromFormerJobAmount() { return pensionFromFormerJobAmount; }
    public DisabilityType getChildSupport() { return childSupport; }
    public Integer getChildSupportAmount() { return childSupportAmount; }
    public DisabilityType getAlimony() { return alimony; }
    public Integer getAlimonyAmount() { return alimonyAmount; }
    public DisabilityType getOtherIncomeSource() { return otherIncomeSource; }
    public Integer getOtherIncomeAmount() { return otherIncomeAmount; }
    public String getOtherIncomeSourceIdentify() { return otherIncomeSourceIdentify; }
    
    // Benefits getters
    public HmisFivePointResponse getSnap() { return snap; }
    public HmisFivePointResponse getWic() { return wic; }
    public HmisFivePointResponse getTanfChildCare() { return tanfChildCare; }
    public HmisFivePointResponse getTanfTransportation() { return tanfTransportation; }
    public HmisFivePointResponse getOtherTanf() { return otherTanf; }
    public HmisFivePointResponse getOtherBenefitsSource() { return otherBenefitsSource; }
    public String getOtherBenefitsSpecify() { return otherBenefitsSpecify; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncomeBenefitsRecord that = (IncomeBenefitsRecord) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}