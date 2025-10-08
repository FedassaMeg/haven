package org.haven.reporting.application.transformers;

import org.haven.programenrollment.domain.IncomeBenefitsRecord;
import org.haven.reporting.domain.hmis.HmisIncomeBenefitsProjection;
import org.haven.shared.vo.hmis.DisabilityType;
import org.haven.shared.vo.hmis.HmisPersonalId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * ETL Transformer for IncomeBenefitsRecord → HmisIncomeBenefitsProjection
 *
 * Implements version-aware transformation logic for HMIS CSV IncomeBenefits.csv export
 * per HUD FY2024 Data Standards (4.02 Income and Sources, 4.03 Non-Cash Benefits)
 *
 * Version Support:
 * - FY2022: Base fields (45 fields)
 * - FY2024: + DataCollectionStage, enhanced validation
 */
@Component
public class IncomeBenefitsETLTransformer {

    private final HmisExportVersionService versionService;

    public IncomeBenefitsETLTransformer(HmisExportVersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * Transform IncomeBenefitsRecord to HMIS CSV projection
     *
     * @param record Source domain record
     * @param enrollmentId Enrollment ID for CSV foreign key
     * @param personalId HMIS Personal ID
     * @param userId User who collected the data
     * @param exportId Export batch ID
     * @return HmisIncomeBenefitsProjection ready for CSV export
     */
    public HmisIncomeBenefitsProjection transform(
            IncomeBenefitsRecord record,
            String enrollmentId,
            HmisPersonalId personalId,
            String userId,
            String exportId) {

        if (record == null) {
            return createEmptyProjection(enrollmentId, personalId, userId, exportId);
        }

        // Calculate data collection stage (FY2024+)
        Integer dataCollectionStage = versionService.supportsFY2024()
                ? HmisValueConverter.toDataCollectionStage(record.getRecordType())
                : null;

        // Convert HmisFivePointResponse to Integer for CSV
        Integer incomeFromAnySource = HmisValueConverter.toInteger(record.getIncomeFromAnySource());
        Integer benefitsFromAnySource = HmisValueConverter.toInteger(record.getBenefitsFromAnySource());

        // Transform benefits from HmisFivePointResponse to DisabilityType for CSV compatibility
        DisabilityType snap = HmisValueConverter.toDisabilityType(record.getSnap());
        DisabilityType wic = HmisValueConverter.toDisabilityType(record.getWic());
        DisabilityType tanfChildCare = HmisValueConverter.toDisabilityType(record.getTanfChildCare());
        DisabilityType tanfTransportation = HmisValueConverter.toDisabilityType(record.getTanfTransportation());
        DisabilityType otherTanf = HmisValueConverter.toDisabilityType(record.getOtherTanf());
        DisabilityType otherBenefitsSource = HmisValueConverter.toDisabilityType(record.getOtherBenefitsSource());

        return new HmisIncomeBenefitsProjection(
                generateIncomeBenefitsId(enrollmentId, dataCollectionStage),
                enrollmentId,
                personalId,
                record.getInformationDate(),
                dataCollectionStage,
                incomeFromAnySource,
                benefitsFromAnySource,

                // Income fields (4.02.2 - 4.02.16)
                record.getTotalMonthlyIncome(),
                record.getEarnedIncome(),
                record.getEarnedIncomeAmount(),
                record.getUnemploymentIncome(),
                record.getUnemploymentIncomeAmount(),
                record.getSupplementalSecurityIncome(),
                record.getSupplementalSecurityIncomeAmount(),
                record.getSocialSecurityDisabilityIncome(),
                record.getSocialSecurityDisabilityIncomeAmount(),
                record.getVaDisabilityServiceConnected(),
                record.getVaDisabilityServiceConnectedAmount(),
                record.getVaDisabilityNonServiceConnected(),
                record.getVaDisabilityNonServiceConnectedAmount(),
                record.getPrivateDisabilityIncome(),
                record.getPrivateDisabilityIncomeAmount(),
                record.getWorkersCompensation(),
                record.getWorkersCompensationAmount(),
                record.getTanfIncome(),
                record.getTanfIncomeAmount(),
                record.getGeneralAssistance(),
                record.getGeneralAssistanceAmount(),
                record.getSocialSecurityRetirement(),
                record.getSocialSecurityRetirementAmount(),
                record.getPensionFromFormerJob(),
                record.getPensionFromFormerJobAmount(),
                record.getChildSupport(),
                record.getChildSupportAmount(),
                record.getAlimony(),
                record.getAlimonyAmount(),
                record.getOtherIncomeSource(),
                record.getOtherIncomeAmount(),
                record.getOtherIncomeSourceIdentify(),

                // Benefits fields (4.03.2 - 4.03.7)
                snap,
                wic,
                tanfChildCare,
                tanfTransportation,
                otherTanf,
                otherBenefitsSource,
                record.getOtherBenefitsSpecify(),

                // Audit fields
                LocalDate.now(),
                LocalDate.now(),
                userId,
                null, // dateDeleted
                exportId
        );
    }

    /**
     * Transform multiple records for different data collection stages
     *
     * @param records List of income/benefits records for an enrollment
     * @param enrollmentId Enrollment ID
     * @param personalId HMIS Personal ID
     * @param userId User ID
     * @param exportId Export ID
     * @return List of projections (one per record/stage)
     */
    public java.util.List<HmisIncomeBenefitsProjection> transformAll(
            java.util.List<IncomeBenefitsRecord> records,
            String enrollmentId,
            HmisPersonalId personalId,
            String userId,
            String exportId) {

        if (records == null || records.isEmpty()) {
            // Per HMIS spec, enrollment without income/benefits data should still export
            // with DATA_NOT_COLLECTED values
            return java.util.List.of(createEmptyProjection(enrollmentId, personalId, userId, exportId));
        }

        return records.stream()
                .map(record -> transform(record, enrollmentId, personalId, userId, exportId))
                .toList();
    }

    /**
     * Create empty projection with all DATA_NOT_COLLECTED values
     * Used when no income/benefits data exists for an enrollment
     */
    private HmisIncomeBenefitsProjection createEmptyProjection(
            String enrollmentId,
            HmisPersonalId personalId,
            String userId,
            String exportId) {

        Integer dataCollectionStage = versionService.supportsFY2024() ? 1 : null; // Default to project start

        return new HmisIncomeBenefitsProjection(
                generateIncomeBenefitsId(enrollmentId, dataCollectionStage),
                enrollmentId,
                personalId,
                LocalDate.now(),
                dataCollectionStage,
                99, // incomeFromAnySource = DATA_NOT_COLLECTED
                99, // benefitsFromAnySource = DATA_NOT_COLLECTED

                // All income fields as DATA_NOT_COLLECTED
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                DisabilityType.DATA_NOT_COLLECTED,
                null,
                null,

                // All benefits fields as DATA_NOT_COLLECTED
                DisabilityType.DATA_NOT_COLLECTED,
                DisabilityType.DATA_NOT_COLLECTED,
                DisabilityType.DATA_NOT_COLLECTED,
                DisabilityType.DATA_NOT_COLLECTED,
                DisabilityType.DATA_NOT_COLLECTED,
                DisabilityType.DATA_NOT_COLLECTED,
                null,

                // Audit fields
                LocalDate.now(),
                LocalDate.now(),
                userId,
                null,
                exportId
        );
    }

    /**
     * Generate IncomeBenefitsID composite key
     * Format: IB_{enrollmentId}_{stage} (FY2024+) or IB_{enrollmentId} (FY2022)
     */
    private String generateIncomeBenefitsId(String enrollmentId, Integer dataCollectionStage) {
        if (versionService.supportsFY2024() && dataCollectionStage != null) {
            return "IB_" + enrollmentId + "_" + dataCollectionStage;
        }
        return "IB_" + enrollmentId;
    }

    /**
     * Validate projection meets HMIS data quality standards
     *
     * @param projection Projection to validate
     * @return Validation errors (empty if valid)
     */
    public java.util.List<String> validate(HmisIncomeBenefitsProjection projection) {
        java.util.List<String> errors = new java.util.ArrayList<>();

        // Validation Rule 1: If IncomeFromAnySource=Yes, TotalMonthlyIncome must be > 0
        if (projection.incomeFromAnySource() != null && projection.incomeFromAnySource() == 1) {
            if (projection.totalMonthlyIncome() == null || projection.totalMonthlyIncome() <= 0) {
                errors.add("IncomeFromAnySource=Yes requires TotalMonthlyIncome > 0");
            }

            // At least one income source should be marked Yes
            boolean hasIncomeSource = hasAtLeastOneIncomeSource(projection);
            if (!hasIncomeSource) {
                errors.add("IncomeFromAnySource=Yes requires at least one income source marked as Yes");
            }
        }

        // Validation Rule 2: If IncomeFromAnySource=No, TotalMonthlyIncome should be 0 or null
        if (projection.incomeFromAnySource() != null && projection.incomeFromAnySource() == 0) {
            if (projection.totalMonthlyIncome() != null && projection.totalMonthlyIncome() > 0) {
                errors.add("IncomeFromAnySource=No should have TotalMonthlyIncome=0 or null");
            }
        }

        // Validation Rule 3: If BenefitsFromAnySource=Yes, at least one benefit should be Yes
        if (projection.benefitsFromAnySource() != null && projection.benefitsFromAnySource() == 1) {
            boolean hasBenefit = hasAtLeastOneBenefit(projection);
            if (!hasBenefit) {
                errors.add("BenefitsFromAnySource=Yes requires at least one benefit marked as Yes");
            }
        }

        // Validation Rule 4: OtherIncomeSourceIdentify required if OtherIncomeSource=Yes
        if (projection.otherIncomeSource() != null &&
            projection.otherIncomeSource() == DisabilityType.YES &&
            (projection.otherIncomeSourceIdentify() == null || projection.otherIncomeSourceIdentify().isBlank())) {
            errors.add("OtherIncomeSourceIdentify required when OtherIncomeSource=Yes");
        }

        // Validation Rule 5: OtherBenefitsSourceIdentify required if OtherBenefitsSource=Yes
        if (projection.otherBenefitsSource() != null &&
            projection.otherBenefitsSource() == DisabilityType.YES &&
            (projection.otherBenefitsSourceIdentify() == null || projection.otherBenefitsSourceIdentify().isBlank())) {
            errors.add("OtherBenefitsSourceIdentify required when OtherBenefitsSource=Yes");
        }

        return errors;
    }

    private boolean hasAtLeastOneIncomeSource(HmisIncomeBenefitsProjection p) {
        return (p.earnedIncome() != null && p.earnedIncome() == DisabilityType.YES) ||
               (p.unemploymentIncome() != null && p.unemploymentIncome() == DisabilityType.YES) ||
               (p.ssiIncome() != null && p.ssiIncome() == DisabilityType.YES) ||
               (p.ssdiIncome() != null && p.ssdiIncome() == DisabilityType.YES) ||
               (p.vaDisabilityServiceIncome() != null && p.vaDisabilityServiceIncome() == DisabilityType.YES) ||
               (p.vaDisabilityNonServiceIncome() != null && p.vaDisabilityNonServiceIncome() == DisabilityType.YES) ||
               (p.privateDisabilityIncome() != null && p.privateDisabilityIncome() == DisabilityType.YES) ||
               (p.workersCompIncome() != null && p.workersCompIncome() == DisabilityType.YES) ||
               (p.tanfIncome() != null && p.tanfIncome() == DisabilityType.YES) ||
               (p.gaIncome() != null && p.gaIncome() == DisabilityType.YES) ||
               (p.socialSecurityRetirementIncome() != null && p.socialSecurityRetirementIncome() == DisabilityType.YES) ||
               (p.pensionIncome() != null && p.pensionIncome() == DisabilityType.YES) ||
               (p.childSupportIncome() != null && p.childSupportIncome() == DisabilityType.YES) ||
               (p.alimonyIncome() != null && p.alimonyIncome() == DisabilityType.YES) ||
               (p.otherIncomeSource() != null && p.otherIncomeSource() == DisabilityType.YES);
    }

    private boolean hasAtLeastOneBenefit(HmisIncomeBenefitsProjection p) {
        return (p.snap() != null && p.snap() == DisabilityType.YES) ||
               (p.wic() != null && p.wic() == DisabilityType.YES) ||
               (p.tanfChildCare() != null && p.tanfChildCare() == DisabilityType.YES) ||
               (p.tanfTransportation() != null && p.tanfTransportation() == DisabilityType.YES) ||
               (p.otherTanf() != null && p.otherTanf() == DisabilityType.YES) ||
               (p.otherBenefitsSource() != null && p.otherBenefitsSource() == DisabilityType.YES);
    }
}
