package org.haven.programenrollment.infrastructure.persistence;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Income and Benefits Records
 * Maps to income_benefits table with HMIS-compliant structure
 */
@Entity
@Table(name = "income_benefits")
public class JpaIncomeBenefitsEntity {
    
    @Id
    @Column(name = "record_id")
    private UUID recordId;
    
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "information_date", nullable = false)
    private LocalDate informationDate;
    
    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;
    
    // Overall income status
    @Column(name = "income_from_any_source", nullable = false)
    private Integer incomeFromAnySource;
    
    @Column(name = "total_monthly_income")
    private Integer totalMonthlyIncome;
    
    // Individual income sources
    @Column(name = "earned_income")
    private Integer earnedIncome;
    
    @Column(name = "earned_income_amount")
    private Integer earnedIncomeAmount;
    
    @Column(name = "unemployment_income")
    private Integer unemploymentIncome;
    
    @Column(name = "unemployment_income_amount")
    private Integer unemploymentIncomeAmount;
    
    @Column(name = "supplemental_security_income")
    private Integer supplementalSecurityIncome;
    
    @Column(name = "supplemental_security_income_amount")
    private Integer supplementalSecurityIncomeAmount;
    
    @Column(name = "social_security_disability_income")
    private Integer socialSecurityDisabilityIncome;
    
    @Column(name = "social_security_disability_income_amount")
    private Integer socialSecurityDisabilityIncomeAmount;
    
    @Column(name = "va_disability_service_connected")
    private Integer vaDisabilityServiceConnected;
    
    @Column(name = "va_disability_service_connected_amount")
    private Integer vaDisabilityServiceConnectedAmount;
    
    @Column(name = "va_disability_non_service_connected")
    private Integer vaDisabilityNonServiceConnected;
    
    @Column(name = "va_disability_non_service_connected_amount")
    private Integer vaDisabilityNonServiceConnectedAmount;
    
    @Column(name = "private_disability_income")
    private Integer privateDisabilityIncome;
    
    @Column(name = "private_disability_income_amount")
    private Integer privateDisabilityIncomeAmount;
    
    @Column(name = "workers_compensation")
    private Integer workersCompensation;
    
    @Column(name = "workers_compensation_amount")
    private Integer workersCompensationAmount;
    
    @Column(name = "tanf_income")
    private Integer tanfIncome;
    
    @Column(name = "tanf_income_amount")
    private Integer tanfIncomeAmount;
    
    @Column(name = "general_assistance")
    private Integer generalAssistance;
    
    @Column(name = "general_assistance_amount")
    private Integer generalAssistanceAmount;
    
    @Column(name = "social_security_retirement")
    private Integer socialSecurityRetirement;
    
    @Column(name = "social_security_retirement_amount")
    private Integer socialSecurityRetirementAmount;
    
    @Column(name = "pension_from_former_job")
    private Integer pensionFromFormerJob;
    
    @Column(name = "pension_from_former_job_amount")
    private Integer pensionFromFormerJobAmount;
    
    @Column(name = "child_support")
    private Integer childSupport;
    
    @Column(name = "child_support_amount")
    private Integer childSupportAmount;
    
    @Column(name = "alimony")
    private Integer alimony;
    
    @Column(name = "alimony_amount")
    private Integer alimonyAmount;
    
    @Column(name = "other_income_source")
    private Integer otherIncomeSource;
    
    @Column(name = "other_income_amount")
    private Integer otherIncomeAmount;
    
    @Column(name = "other_income_source_identify")
    private String otherIncomeSourceIdentify;
    
    // Audit fields
    @Column(name = "collected_by", nullable = false)
    private String collectedBy;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected JpaIncomeBenefitsEntity() {
        // JPA constructor
    }
    
    public JpaIncomeBenefitsEntity(IncomeBenefitsRecord record) {
        this.recordId = record.getRecordId();
        this.enrollmentId = record.getEnrollmentId().value();
        this.clientId = record.getClientId().value();
        this.informationDate = record.getInformationDate();
        this.recordType = record.getRecordType().getCode();
        this.incomeFromAnySource = record.getIncomeFromAnySource().getHmisValue();
        this.totalMonthlyIncome = record.getTotalMonthlyIncome();
        this.collectedBy = record.getCollectedBy();
        this.createdAt = record.getCreatedAt();
        this.updatedAt = record.getUpdatedAt();
        
        // Map individual income sources
        this.earnedIncome = record.getEarnedIncome() != null ? record.getEarnedIncome().getHmisValue() : 99;
        this.earnedIncomeAmount = record.getEarnedIncomeAmount();
        this.unemploymentIncome = record.getUnemploymentIncome() != null ? record.getUnemploymentIncome().getHmisValue() : 99;
        this.unemploymentIncomeAmount = record.getUnemploymentIncomeAmount();
        this.supplementalSecurityIncome = record.getSupplementalSecurityIncome() != null ? record.getSupplementalSecurityIncome().getHmisValue() : 99;
        this.supplementalSecurityIncomeAmount = record.getSupplementalSecurityIncomeAmount();
        this.socialSecurityDisabilityIncome = record.getSocialSecurityDisabilityIncome() != null ? record.getSocialSecurityDisabilityIncome().getHmisValue() : 99;
        this.socialSecurityDisabilityIncomeAmount = record.getSocialSecurityDisabilityIncomeAmount();
        this.vaDisabilityServiceConnected = record.getVaDisabilityServiceConnected() != null ? record.getVaDisabilityServiceConnected().getHmisValue() : 99;
        this.vaDisabilityServiceConnectedAmount = record.getVaDisabilityServiceConnectedAmount();
        this.vaDisabilityNonServiceConnected = record.getVaDisabilityNonServiceConnected() != null ? record.getVaDisabilityNonServiceConnected().getHmisValue() : 99;
        this.vaDisabilityNonServiceConnectedAmount = record.getVaDisabilityNonServiceConnectedAmount();
        this.privateDisabilityIncome = record.getPrivateDisabilityIncome() != null ? record.getPrivateDisabilityIncome().getHmisValue() : 99;
        this.privateDisabilityIncomeAmount = record.getPrivateDisabilityIncomeAmount();
        this.workersCompensation = record.getWorkersCompensation() != null ? record.getWorkersCompensation().getHmisValue() : 99;
        this.workersCompensationAmount = record.getWorkersCompensationAmount();
        this.tanfIncome = record.getTanfIncome() != null ? record.getTanfIncome().getHmisValue() : 99;
        this.tanfIncomeAmount = record.getTanfIncomeAmount();
        this.generalAssistance = record.getGeneralAssistance() != null ? record.getGeneralAssistance().getHmisValue() : 99;
        this.generalAssistanceAmount = record.getGeneralAssistanceAmount();
        this.socialSecurityRetirement = record.getSocialSecurityRetirement() != null ? record.getSocialSecurityRetirement().getHmisValue() : 99;
        this.socialSecurityRetirementAmount = record.getSocialSecurityRetirementAmount();
        this.pensionFromFormerJob = record.getPensionFromFormerJob() != null ? record.getPensionFromFormerJob().getHmisValue() : 99;
        this.pensionFromFormerJobAmount = record.getPensionFromFormerJobAmount();
        this.childSupport = record.getChildSupport() != null ? record.getChildSupport().getHmisValue() : 99;
        this.childSupportAmount = record.getChildSupportAmount();
        this.alimony = record.getAlimony() != null ? record.getAlimony().getHmisValue() : 99;
        this.alimonyAmount = record.getAlimonyAmount();
        this.otherIncomeSource = record.getOtherIncomeSource() != null ? record.getOtherIncomeSource().getHmisValue() : 99;
        this.otherIncomeAmount = record.getOtherIncomeAmount();
        this.otherIncomeSourceIdentify = record.getOtherIncomeSourceIdentify();
    }
    
    public IncomeBenefitsRecord toDomainObject() {
        IncomeBenefitsRecord record = new IncomeBenefitsRecord();
        
        // Use reflection or a builder pattern to set private fields
        // For now, we'll need to create a reconstitution method
        return reconstituteDomainObject();
    }
    
    private IncomeBenefitsRecord reconstituteDomainObject() {
        // Create appropriate factory method based on record type
        InformationDate infoDateType = InformationDate.valueOf(recordType);
        ProgramEnrollmentId enrollmentDomainId = ProgramEnrollmentId.of(enrollmentId);
        ClientId clientDomainId = new ClientId(clientId);
        IncomeFromAnySource incomeStatus = mapIntegerToIncomeFromAnySource(incomeFromAnySource);
        
        IncomeBenefitsRecord record;
        
        switch (infoDateType) {
            case START_OF_PROJECT -> record = IncomeBenefitsRecord.createAtProjectStart(
                enrollmentDomainId, clientDomainId, informationDate, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, collectedBy);
            case UPDATE -> record = IncomeBenefitsRecord.createUpdate(
                enrollmentDomainId, clientDomainId, informationDate, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, collectedBy);
            case ANNUAL_ASSESSMENT -> record = IncomeBenefitsRecord.createAnnualAssessment(
                enrollmentDomainId, clientDomainId, informationDate, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, collectedBy);
            case EXIT -> record = IncomeBenefitsRecord.createAtProjectExit(
                enrollmentDomainId, clientDomainId, informationDate, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, collectedBy);
            case MINOR_TURNING_18 -> record = IncomeBenefitsRecord.createMinorTurning18(
                enrollmentDomainId, clientDomainId, informationDate, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, 
                org.haven.shared.vo.hmis.HmisFivePointResponse.DATA_NOT_COLLECTED, collectedBy);
            default -> throw new IllegalArgumentException("Unknown record type: " + recordType);
        }
        
        // Update total income
        if (totalMonthlyIncome != null) {
            record.updateTotalMonthlyIncome(totalMonthlyIncome);
        }
        
        // Update individual sources
        updateDomainRecordWithSources(record);
        
        return record;
    }
    
    private void updateDomainRecordWithSources(IncomeBenefitsRecord record) {
        if (earnedIncome != null && earnedIncome != 99) {
            record.updateIncomeSource(IncomeSource.EARNED_INCOME, mapIntegerToDisabilityType(earnedIncome), earnedIncomeAmount);
        }
        if (unemploymentIncome != null && unemploymentIncome != 99) {
            record.updateIncomeSource(IncomeSource.UNEMPLOYMENT_INSURANCE, mapIntegerToDisabilityType(unemploymentIncome), unemploymentIncomeAmount);
        }
        if (supplementalSecurityIncome != null && supplementalSecurityIncome != 99) {
            record.updateIncomeSource(IncomeSource.SUPPLEMENTAL_SECURITY_INCOME, mapIntegerToDisabilityType(supplementalSecurityIncome), supplementalSecurityIncomeAmount);
        }
        if (socialSecurityDisabilityIncome != null && socialSecurityDisabilityIncome != 99) {
            record.updateIncomeSource(IncomeSource.SOCIAL_SECURITY_DISABILITY_INSURANCE, mapIntegerToDisabilityType(socialSecurityDisabilityIncome), socialSecurityDisabilityIncomeAmount);
        }
        if (vaDisabilityServiceConnected != null && vaDisabilityServiceConnected != 99) {
            record.updateIncomeSource(IncomeSource.VA_DISABILITY_SERVICE_CONNECTED, mapIntegerToDisabilityType(vaDisabilityServiceConnected), vaDisabilityServiceConnectedAmount);
        }
        if (vaDisabilityNonServiceConnected != null && vaDisabilityNonServiceConnected != 99) {
            record.updateIncomeSource(IncomeSource.VA_DISABILITY_NON_SERVICE_CONNECTED, mapIntegerToDisabilityType(vaDisabilityNonServiceConnected), vaDisabilityNonServiceConnectedAmount);
        }
        if (privateDisabilityIncome != null && privateDisabilityIncome != 99) {
            record.updateIncomeSource(IncomeSource.PRIVATE_DISABILITY, mapIntegerToDisabilityType(privateDisabilityIncome), privateDisabilityIncomeAmount);
        }
        if (workersCompensation != null && workersCompensation != 99) {
            record.updateIncomeSource(IncomeSource.WORKERS_COMPENSATION, mapIntegerToDisabilityType(workersCompensation), workersCompensationAmount);
        }
        if (tanfIncome != null && tanfIncome != 99) {
            record.updateIncomeSource(IncomeSource.TANF, mapIntegerToDisabilityType(tanfIncome), tanfIncomeAmount);
        }
        if (generalAssistance != null && generalAssistance != 99) {
            record.updateIncomeSource(IncomeSource.GENERAL_ASSISTANCE, mapIntegerToDisabilityType(generalAssistance), generalAssistanceAmount);
        }
        if (socialSecurityRetirement != null && socialSecurityRetirement != 99) {
            record.updateIncomeSource(IncomeSource.RETIREMENT_SOCIAL_SECURITY, mapIntegerToDisabilityType(socialSecurityRetirement), socialSecurityRetirementAmount);
        }
        if (pensionFromFormerJob != null && pensionFromFormerJob != 99) {
            record.updateIncomeSource(IncomeSource.PENSION_RETIREMENT_FROM_FORMER_JOB, mapIntegerToDisabilityType(pensionFromFormerJob), pensionFromFormerJobAmount);
        }
        if (childSupport != null && childSupport != 99) {
            record.updateIncomeSource(IncomeSource.CHILD_SUPPORT, mapIntegerToDisabilityType(childSupport), childSupportAmount);
        }
        if (alimony != null && alimony != 99) {
            record.updateIncomeSource(IncomeSource.ALIMONY, mapIntegerToDisabilityType(alimony), alimonyAmount);
        }
        if (otherIncomeSource != null && otherIncomeSource != 99) {
            record.updateOtherIncomeSource(mapIntegerToDisabilityType(otherIncomeSource), otherIncomeAmount, otherIncomeSourceIdentify);
        }
    }
    
    private IncomeFromAnySource mapIntegerToIncomeFromAnySource(Integer value) {
        return switch (value) {
            case 0 -> IncomeFromAnySource.NO;
            case 1 -> IncomeFromAnySource.YES;
            case 8 -> IncomeFromAnySource.CLIENT_DOESNT_KNOW;
            case 9 -> IncomeFromAnySource.CLIENT_REFUSED;
            case 99 -> IncomeFromAnySource.DATA_NOT_COLLECTED;
            default -> IncomeFromAnySource.DATA_NOT_COLLECTED;
        };
    }
    
    private DisabilityType mapIntegerToDisabilityType(Integer value) {
        return switch (value) {
            case 0 -> DisabilityType.NO;
            case 1 -> DisabilityType.YES;
            case 8 -> DisabilityType.CLIENT_DOESNT_KNOW;
            case 9 -> DisabilityType.CLIENT_REFUSED;
            case 99 -> DisabilityType.DATA_NOT_COLLECTED;
            default -> DisabilityType.DATA_NOT_COLLECTED;
        };
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public String getRecordType() { return recordType; }
    public Integer getIncomeFromAnySource() { return incomeFromAnySource; }
    public Integer getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    // All individual source getters
    public Integer getEarnedIncome() { return earnedIncome; }
    public Integer getEarnedIncomeAmount() { return earnedIncomeAmount; }
    public Integer getUnemploymentIncome() { return unemploymentIncome; }
    public Integer getUnemploymentIncomeAmount() { return unemploymentIncomeAmount; }
    public Integer getSupplementalSecurityIncome() { return supplementalSecurityIncome; }
    public Integer getSupplementalSecurityIncomeAmount() { return supplementalSecurityIncomeAmount; }
    public Integer getSocialSecurityDisabilityIncome() { return socialSecurityDisabilityIncome; }
    public Integer getSocialSecurityDisabilityIncomeAmount() { return socialSecurityDisabilityIncomeAmount; }
    public Integer getVaDisabilityServiceConnected() { return vaDisabilityServiceConnected; }
    public Integer getVaDisabilityServiceConnectedAmount() { return vaDisabilityServiceConnectedAmount; }
    public Integer getVaDisabilityNonServiceConnected() { return vaDisabilityNonServiceConnected; }
    public Integer getVaDisabilityNonServiceConnectedAmount() { return vaDisabilityNonServiceConnectedAmount; }
    public Integer getPrivateDisabilityIncome() { return privateDisabilityIncome; }
    public Integer getPrivateDisabilityIncomeAmount() { return privateDisabilityIncomeAmount; }
    public Integer getWorkersCompensation() { return workersCompensation; }
    public Integer getWorkersCompensationAmount() { return workersCompensationAmount; }
    public Integer getTanfIncome() { return tanfIncome; }
    public Integer getTanfIncomeAmount() { return tanfIncomeAmount; }
    public Integer getGeneralAssistance() { return generalAssistance; }
    public Integer getGeneralAssistanceAmount() { return generalAssistanceAmount; }
    public Integer getSocialSecurityRetirement() { return socialSecurityRetirement; }
    public Integer getSocialSecurityRetirementAmount() { return socialSecurityRetirementAmount; }
    public Integer getPensionFromFormerJob() { return pensionFromFormerJob; }
    public Integer getPensionFromFormerJobAmount() { return pensionFromFormerJobAmount; }
    public Integer getChildSupport() { return childSupport; }
    public Integer getChildSupportAmount() { return childSupportAmount; }
    public Integer getAlimony() { return alimony; }
    public Integer getAlimonyAmount() { return alimonyAmount; }
    public Integer getOtherIncomeSource() { return otherIncomeSource; }
    public Integer getOtherIncomeAmount() { return otherIncomeAmount; }
    public String getOtherIncomeSourceIdentify() { return otherIncomeSourceIdentify; }
}