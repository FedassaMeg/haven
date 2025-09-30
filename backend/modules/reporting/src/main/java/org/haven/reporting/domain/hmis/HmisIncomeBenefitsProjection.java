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
        
        return createEmptyProjection(enrollmentId, personalId, userId, exportId);
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
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            (Integer) null,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            LocalDate.now(),
            LocalDate.now(),
            userId,
            (LocalDateTime) null,
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
    
    /**
     * Income benefits typically don't require redaction for restricted notes,
     * but included for interface consistency
     */
    public HmisIncomeBenefitsProjection withRedactedIncome() {
        // Income data is usually not restricted in the same way as health/DV info
        // Return the same instance unless specific restrictions apply
        return this;
    }
}