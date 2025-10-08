package org.haven.programenrollment.application.validation;

import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation service for Intake PSDE data
 * Implements HUD consistency checks and conditional logic validation
 */
@Service
public class IntakePsdeValidationService {

    /**
     * Validate complete PSDE record with all conditional logic
     */
    public ValidationResult validateIntakePsdeRecord(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        // Basic field validation
        errors.addAll(validateBasicFields(record));

        // Conditional logic validation
        errors.addAll(validateDomesticViolenceConditionalLogic(record));
        errors.addAll(validateHealthInsuranceConditionalLogic(record));
        errors.addAll(validateRrhMoveInConditionalLogic(record));
        errors.addAll(validateIncomeConditionalLogic(record));

        // HMIS data quality validation
        errors.addAll(validateHmisDataQuality(record));

        // VAWA-specific validation
        errors.addAll(validateVawaRequirements(record));

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private List<ValidationError> validateBasicFields(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        if (record.getInformationDate() == null) {
            errors.add(new ValidationError("informationDate", "Information date is required"));
        }

        if (record.getCollectionStage() == null) {
            errors.add(new ValidationError("collectionStage", "Collection stage is required"));
        }

        if (record.getCollectedBy() == null || record.getCollectedBy().trim().isEmpty()) {
            errors.add(new ValidationError("collectedBy", "Collected by is required"));
        }

        return errors;
    }

    private List<ValidationError> validateDomesticViolenceConditionalLogic(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        DomesticViolence dv = record.getDomesticViolence();
        DomesticViolenceRecency recency = record.getDomesticViolenceRecency();
        HmisFivePoint fleeing = record.getCurrentlyFleeingDomesticViolence();

        if (dv != null) {
            // If DV = NO, recency and fleeing should not be collected
            if (dv.noHistory()) {
                if (recency != null && recency != DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                    errors.add(new ValidationError("domesticViolenceRecency",
                        "DV recency should not be collected when DV history is 'No'"));
                }
                if (fleeing != null && fleeing != HmisFivePoint.DATA_NOT_COLLECTED) {
                    errors.add(new ValidationError("currentlyFleeingDomesticViolence",
                        "Currently fleeing should not be collected when DV history is 'No'"));
                }
            }

            // If DV = YES, recency is required
            if (dv.hasHistory()) {
                if (recency == null || recency == DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                    errors.add(new ValidationError("domesticViolenceRecency",
                        "DV recency is required when DV history is 'Yes'"));
                }
                // Currently fleeing is recommended but not required
            }

            // If DV = Client doesn't know/refused, recency and fleeing should not be collected
            if (dv == DomesticViolence.CLIENT_DOESNT_KNOW || dv == DomesticViolence.CLIENT_REFUSED) {
                if (recency != null && recency != DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                    errors.add(new ValidationError("domesticViolenceRecency",
                        "DV recency should not be collected when DV history is unknown/refused"));
                }
                if (fleeing != null && fleeing != HmisFivePoint.DATA_NOT_COLLECTED) {
                    errors.add(new ValidationError("currentlyFleeingDomesticViolence",
                        "Currently fleeing should not be collected when DV history is unknown/refused"));
                }
            }
        }

        return errors;
    }

    private List<ValidationError> validateHealthInsuranceConditionalLogic(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        CoveredByHealthInsurance covered = record.getCoveredByHealthInsurance();
        HopwaNoInsuranceReason noInsuranceReason = record.getNoInsuranceReason();

        if (covered != null) {
            // If covered = YES, no insurance reason should not be provided
            if (covered == CoveredByHealthInsurance.YES) {
                if (noInsuranceReason != null && noInsuranceReason != HopwaNoInsuranceReason.DATA_NOT_COLLECTED) {
                    errors.add(new ValidationError("noInsuranceReason",
                        "No insurance reason should not be provided when covered by health insurance"));
                }
            }

            // If covered = NO, no insurance reason should be provided for HOPWA programs
            if (covered == CoveredByHealthInsurance.NO) {
                // Note: noInsuranceReason is only required for HOPWA programs
                // For other programs, it's optional
            }
        }

        return errors;
    }

    private List<ValidationError> validateRrhMoveInConditionalLogic(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        var moveInDate = record.getResidentialMoveInDate();
        var moveInType = record.getMoveInType();

        // If move-in date is provided, move-in type is required
        if (moveInDate != null) {
            if (moveInType == null || moveInType == ResidentialMoveInDateType.DATA_NOT_COLLECTED) {
                errors.add(new ValidationError("moveInType",
                    "Move-in type is required when move-in date is provided"));
            }
        }

        // If move-in type is provided, move-in date is required
        if (moveInType != null && moveInType != ResidentialMoveInDateType.DATA_NOT_COLLECTED) {
            if (moveInDate == null) {
                errors.add(new ValidationError("residentialMoveInDate",
                    "Move-in date is required when move-in type is provided"));
            }
        }

        // Move-in date cannot be before program entry date (would need enrollment context)
        // This validation would be performed at the service layer with enrollment data

        return errors;
    }

    private List<ValidationError> validateIncomeConditionalLogic(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        Integer totalIncome = record.getTotalMonthlyIncome();
        IncomeFromAnySource incomeFromAny = record.getIncomeFromAnySource();

        if (incomeFromAny != null) {
            // If income from any source = NO, total income should be 0 or null
            if (incomeFromAny == IncomeFromAnySource.NO) {
                if (totalIncome != null && totalIncome > 0) {
                    errors.add(new ValidationError("totalMonthlyIncome",
                        "Total monthly income should be 0 when 'Income from any source' is 'No'"));
                }
            }

            // If income from any source = YES, total income should be > 0
            if (incomeFromAny == IncomeFromAnySource.YES) {
                if (totalIncome == null || totalIncome <= 0) {
                    errors.add(new ValidationError("totalMonthlyIncome",
                        "Total monthly income should be greater than 0 when 'Income from any source' is 'Yes'"));
                }
            }
        }

        return errors;
    }

    private List<ValidationError> validateHmisDataQuality(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        // Only validate data quality if this is a comprehensive assessment
        if (record.getCollectionStage() != null &&
            record.getCollectionStage().requiresPsdeData()) {

            // Income from any source is required
            if (record.getIncomeFromAnySource() == null ||
                !record.getIncomeFromAnySource().isKnownResponse()) {
                errors.add(new ValidationError("incomeFromAnySource",
                    "Income from any source must be a known response for HMIS data quality"));
            }

            // All disability fields must be known responses
            if (record.getPhysicalDisability() == null || !record.getPhysicalDisability().isKnownResponse()) {
                errors.add(new ValidationError("physicalDisability",
                    "Physical disability must be a known response for HMIS data quality"));
            }
            if (record.getDevelopmentalDisability() == null || !record.getDevelopmentalDisability().isKnownResponse()) {
                errors.add(new ValidationError("developmentalDisability",
                    "Developmental disability must be a known response for HMIS data quality"));
            }
            if (record.getChronicHealthCondition() == null || !record.getChronicHealthCondition().isKnownResponse()) {
                errors.add(new ValidationError("chronicHealthCondition",
                    "Chronic health condition must be a known response for HMIS data quality"));
            }
            if (record.getHivAids() == null || !record.getHivAids().isKnownResponse()) {
                errors.add(new ValidationError("hivAids",
                    "HIV/AIDS must be a known response for HMIS data quality"));
            }
            if (record.getMentalHealthDisorder() == null || !record.getMentalHealthDisorder().isKnownResponse()) {
                errors.add(new ValidationError("mentalHealthDisorder",
                    "Mental health disorder must be a known response for HMIS data quality"));
            }
            if (record.getSubstanceUseDisorder() == null || !record.getSubstanceUseDisorder().isKnownResponse()) {
                errors.add(new ValidationError("substanceUseDisorder",
                    "Substance use disorder must be a known response for HMIS data quality"));
            }

            // DV history must be a known response
            if (record.getDomesticViolence() == null || !record.getDomesticViolence().isKnownResponse()) {
                errors.add(new ValidationError("domesticViolence",
                    "Domestic violence history must be a known response for HMIS data quality"));
            }
        }

        return errors;
    }

    private List<ValidationError> validateVawaRequirements(IntakePsdeRecord record) {
        List<ValidationError> errors = new ArrayList<>();

        // If VAWA confidentiality is requested, ensure appropriate redaction level
        if (record.getVawaConfidentialityRequested() != null &&
            record.getVawaConfidentialityRequested()) {

            if (record.getDvRedactionLevel() == null ||
                record.getDvRedactionLevel() == DvRedactionFlag.NO_REDACTION) {
                errors.add(new ValidationError("dvRedactionLevel",
                    "Appropriate redaction level required when VAWA confidentiality is requested"));
            }
        }

        // If currently fleeing DV, recommend enhanced redaction
        if (record.getCurrentlyFleeingDomesticViolence() != null &&
            record.getCurrentlyFleeingDomesticViolence().isYes()) {

            if (record.getDvRedactionLevel() == null ||
                record.getDvRedactionLevel() == DvRedactionFlag.NO_REDACTION) {
                // This is a warning, not an error
                // Could be logged for data quality review
            }
        }

        return errors;
    }

    /**
     * Validation result container
     */
    public record ValidationResult(boolean isValid, List<ValidationError> errors) {

        public boolean hasErrors() {
            return !isValid;
        }

        public String getErrorSummary() {
            if (isValid) return "No errors";

            return String.format("Found %d validation errors: %s",
                errors.size(),
                errors.stream()
                    .map(ValidationError::message)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown errors"));
        }
    }

    /**
     * Individual validation error
     */
    public record ValidationError(String field, String message) {}
}