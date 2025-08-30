package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.ProgramSpecificDataElements;
import org.haven.programenrollment.domain.IncomeBenefitsRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HMIS IncomeBenefits.csv projection
 * Represents income and benefits data for HMIS CSV export per FY2024 Data Standards
 * Aligns with HMIS Data Elements 4.02 Income and Sources, 4.03 Non-Cash Benefits
 */
public record HmisIncomeBenefitsProjection(
    String incomeBenefitsId,
    String enrollmentId,
    HmisPersonalId personalId,
    LocalDate informationDate,
    
    // Income data
    Integer totalMonthlyIncome,
    DisabilityType earnedIncome,
    Integer earnedIncomeAmount,
    DisabilityType unemploymentIncome,
    Integer unemploymentIncomeAmount,
    DisabilityType ssiIncome,
    Integer ssiIncomeAmount,
    DisabilityType ssdiIncome,
    Integer ssdiIncomeAmount,
    DisabilityType vaDisabilityServiceIncome,
    Integer vaDisabilityServiceIncomeAmount,
    DisabilityType vaDisabilityNonServiceIncome,
    Integer vaDisabilityNonServiceIncomeAmount,
    DisabilityType privateDisabilityIncome,
    Integer privateDisabilityIncomeAmount,
    DisabilityType workersCompIncome,
    Integer workersCompIncomeAmount,
    DisabilityType tanfIncome,
    Integer tanfIncomeAmount,
    DisabilityType gaIncome,
    Integer gaIncomeAmount,
    DisabilityType socialSecurityRetirementIncome,
    Integer socialSecurityRetirementIncomeAmount,
    DisabilityType pensionIncome,
    Integer pensionIncomeAmount,
    DisabilityType childSupportIncome,
    Integer childSupportIncomeAmount,
    DisabilityType alimonyIncome,
    Integer alimonyIncomeAmount,
    DisabilityType otherIncomeSource,
    Integer otherIncomeAmount,
    
    // Benefits data
    DisabilityType snap,
    DisabilityType wic,
    DisabilityType tanfChildCare,
    DisabilityType tanfTransportation,
    DisabilityType otherTanf,
    DisabilityType otherBenefitsSource,
    
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId
) {

    /**
     * Create projection from IncomeBenefitsRecord (HMIS-compliant approach)
     */
    public static HmisIncomeBenefitsProjection fromIncomeBenefitsRecord(
            String enrollmentId,
            HmisPersonalId personalId,
            IncomeBenefitsRecord incomeRecord,
            String userId,
            String exportId) {
        
        if (incomeRecord == null) {
            return createEmptyProjection(enrollmentId, personalId, userId, exportId);
        }
        
        return new HmisIncomeBenefitsProjection(
            generateIncomeBenefitsId(incomeRecord.getRecordId().toString()),
            enrollmentId,
            personalId,
            incomeRecord.getInformationDate(),
            
            // Income data with real amounts
            incomeRecord.getTotalMonthlyIncome(),
            incomeRecord.getEarnedIncome() != null ? incomeRecord.getEarnedIncome() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getEarnedIncomeAmount(),
            incomeRecord.getUnemploymentIncome() != null ? incomeRecord.getUnemploymentIncome() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getUnemploymentIncomeAmount(),
            incomeRecord.getSupplementalSecurityIncome() != null ? incomeRecord.getSupplementalSecurityIncome() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getSupplementalSecurityIncomeAmount(),
            incomeRecord.getSocialSecurityDisabilityIncome() != null ? incomeRecord.getSocialSecurityDisabilityIncome() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getSocialSecurityDisabilityIncomeAmount(),
            incomeRecord.getVaDisabilityServiceConnected() != null ? incomeRecord.getVaDisabilityServiceConnected() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getVaDisabilityServiceConnectedAmount(),
            incomeRecord.getVaDisabilityNonServiceConnected() != null ? incomeRecord.getVaDisabilityNonServiceConnected() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getVaDisabilityNonServiceConnectedAmount(),
            incomeRecord.getPrivateDisabilityIncome() != null ? incomeRecord.getPrivateDisabilityIncome() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getPrivateDisabilityIncomeAmount(),
            incomeRecord.getWorkersCompensation() != null ? incomeRecord.getWorkersCompensation() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getWorkersCompensationAmount(),
            incomeRecord.getTanfIncome() != null ? incomeRecord.getTanfIncome() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getTanfIncomeAmount(),
            incomeRecord.getGeneralAssistance() != null ? incomeRecord.getGeneralAssistance() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getGeneralAssistanceAmount(),
            incomeRecord.getSocialSecurityRetirement() != null ? incomeRecord.getSocialSecurityRetirement() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getSocialSecurityRetirementAmount(),
            incomeRecord.getPensionFromFormerJob() != null ? incomeRecord.getPensionFromFormerJob() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getPensionFromFormerJobAmount(),
            incomeRecord.getChildSupport() != null ? incomeRecord.getChildSupport() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getChildSupportAmount(),
            incomeRecord.getAlimony() != null ? incomeRecord.getAlimony() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getAlimonyAmount(),
            incomeRecord.getOtherIncomeSource() != null ? incomeRecord.getOtherIncomeSource() : DisabilityType.DATA_NOT_COLLECTED,
            incomeRecord.getOtherIncomeAmount(),
            
            // Benefits data - these would need separate Benefits records, for now default to DATA_NOT_COLLECTED
            DisabilityType.DATA_NOT_COLLECTED, // SNAP
            DisabilityType.DATA_NOT_COLLECTED, // WIC  
            DisabilityType.DATA_NOT_COLLECTED, // TANF Child Care
            DisabilityType.DATA_NOT_COLLECTED, // TANF Transportation
            DisabilityType.DATA_NOT_COLLECTED, // Other TANF
            DisabilityType.DATA_NOT_COLLECTED, // Other Benefits
            
            incomeRecord.getInformationDate(), // DateCreated matches information date
            incomeRecord.getInformationDate(), // DateUpdated matches information date
            userId,
            null, // Not deleted
            exportId
        );
    }
    
    /**
     * Create projection from ProgramSpecificDataElements (legacy/fallback)
     * @deprecated Use fromIncomeBenefitsRecord for HMIS compliance
     */
    @Deprecated
    public static HmisIncomeBenefitsProjection fromProgramSpecificData(
            String enrollmentId,
            HmisPersonalId personalId,
            ProgramSpecificDataElements psde,
            String userId,
            String exportId) {
        
        if (psde == null) {
            return createEmptyProjection(enrollmentId, personalId, userId, exportId);
        }
        
        return new HmisIncomeBenefitsProjection(
            generateIncomeBenefitsId(enrollmentId),
            enrollmentId,
            personalId,
            psde.getIncomeInformationDate() != null ? psde.getIncomeInformationDate() : psde.getNonCashBenefitInformationDate(),
            
            // Income data - no per-source amounts in legacy PSDE
            psde.getTotalMonthlyIncome(),
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.EARNED_INCOME),
            0, // Legacy approach has no per-source amounts
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.UNEMPLOYMENT_INSURANCE),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.SUPPLEMENTAL_SECURITY_INCOME),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.SOCIAL_SECURITY_DISABILITY_INSURANCE),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.VA_DISABILITY_SERVICE_CONNECTED),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.VA_DISABILITY_NON_SERVICE_CONNECTED),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.PRIVATE_DISABILITY),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.WORKERS_COMPENSATION),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.TANF),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.GENERAL_ASSISTANCE),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.RETIREMENT_SOCIAL_SECURITY),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.PENSION_RETIREMENT_FROM_FORMER_JOB),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.CHILD_SUPPORT),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.ALIMONY),
            0,
            mapIncomeSource(psde.getIncomeSources(), IncomeSource.OTHER_SOURCE),
            0,
            
            // Benefits data
            mapNonCashBenefit(psde.getNonCashBenefits(), NonCashBenefit.SUPPLEMENTAL_NUTRITION_ASSISTANCE_PROGRAM),
            mapNonCashBenefit(psde.getNonCashBenefits(), NonCashBenefit.SPECIAL_SUPPLEMENTAL_NUTRITION_PROGRAM_WIC),
            mapNonCashBenefit(psde.getNonCashBenefits(), NonCashBenefit.TANF_CHILD_CARE_SERVICES),
            mapNonCashBenefit(psde.getNonCashBenefits(), NonCashBenefit.TANF_TRANSPORTATION_SERVICES),
            mapNonCashBenefit(psde.getNonCashBenefits(), NonCashBenefit.OTHER_TANF_SERVICES),
            mapNonCashBenefit(psde.getNonCashBenefits(), NonCashBenefit.OTHER_SOURCE),
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    private static HmisIncomeBenefitsProjection createEmptyProjection(
            String enrollmentId,
            HmisPersonalId personalId,
            String userId,
            String exportId) {
        
        return new HmisIncomeBenefitsProjection(
            generateIncomeBenefitsId(enrollmentId),
            enrollmentId,
            personalId,
            LocalDate.now(),
            null, // No income data
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED, 0,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    private static DisabilityType mapIncomeSource(List<IncomeSource> sources, IncomeSource targetSource) {
        return sources.contains(targetSource) ? DisabilityType.YES : DisabilityType.NO;
    }
    
    private static DisabilityType mapNonCashBenefit(List<NonCashBenefit> benefits, NonCashBenefit targetBenefit) {
        return benefits.contains(targetBenefit) ? DisabilityType.YES : DisabilityType.NO;
    }
    
    private static String generateIncomeBenefitsId(String enrollmentId) {
        return "IB_" + enrollmentId;
    }
    
    /**
     * Convert to CSV row format
     */
    public String toCsvRow() {
        return String.join(",",
            quote(incomeBenefitsId),
            quote(enrollmentId),
            quote(personalId.value()),
            quote(formatDate(informationDate)),
            formatInteger(totalMonthlyIncome),
            String.valueOf(earnedIncome.getHmisValue()),
            formatInteger(earnedIncomeAmount),
            String.valueOf(unemploymentIncome.getHmisValue()),
            formatInteger(unemploymentIncomeAmount),
            String.valueOf(ssiIncome.getHmisValue()),
            formatInteger(ssiIncomeAmount),
            String.valueOf(ssdiIncome.getHmisValue()),
            formatInteger(ssdiIncomeAmount),
            String.valueOf(vaDisabilityServiceIncome.getHmisValue()),
            formatInteger(vaDisabilityServiceIncomeAmount),
            String.valueOf(vaDisabilityNonServiceIncome.getHmisValue()),
            formatInteger(vaDisabilityNonServiceIncomeAmount),
            String.valueOf(privateDisabilityIncome.getHmisValue()),
            formatInteger(privateDisabilityIncomeAmount),
            String.valueOf(workersCompIncome.getHmisValue()),
            formatInteger(workersCompIncomeAmount),
            String.valueOf(tanfIncome.getHmisValue()),
            formatInteger(tanfIncomeAmount),
            String.valueOf(gaIncome.getHmisValue()),
            formatInteger(gaIncomeAmount),
            String.valueOf(socialSecurityRetirementIncome.getHmisValue()),
            formatInteger(socialSecurityRetirementIncomeAmount),
            String.valueOf(pensionIncome.getHmisValue()),
            formatInteger(pensionIncomeAmount),
            String.valueOf(childSupportIncome.getHmisValue()),
            formatInteger(childSupportIncomeAmount),
            String.valueOf(alimonyIncome.getHmisValue()),
            formatInteger(alimonyIncomeAmount),
            String.valueOf(otherIncomeSource.getHmisValue()),
            formatInteger(otherIncomeAmount),
            String.valueOf(snap.getHmisValue()),
            String.valueOf(wic.getHmisValue()),
            String.valueOf(tanfChildCare.getHmisValue()),
            String.valueOf(tanfTransportation.getHmisValue()),
            String.valueOf(otherTanf.getHmisValue()),
            String.valueOf(otherBenefitsSource.getHmisValue()),
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }
    
    private String quote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    
    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "";
    }
    
    private String formatInteger(Integer value) {
        return value != null ? value.toString() : "";
    }
}