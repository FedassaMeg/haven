package org.haven.api.enrollments.dto;

import jakarta.validation.constraints.*;
import org.haven.shared.vo.hmis.*;
import java.time.LocalDate;

/**
 * REST DTO for creating/updating Intake PSDE records
 * Includes comprehensive validation with conditional logic support
 */
public record IntakePsdeRequest(

    // Assessment metadata
    @NotNull(message = "Information date is required")
    @PastOrPresent(message = "Information date cannot be in the future")
    LocalDate informationDate,

    @NotNull(message = "Collection stage is required")
    IntakeDataCollectionStage collectionStage,

    // Income information
    @Min(value = 0, message = "Total monthly income cannot be negative")
    @Max(value = 999999, message = "Total monthly income exceeds maximum allowed")
    Integer totalMonthlyIncome,

    IncomeFromAnySource incomeFromAnySource,
    Boolean isEarnedIncomeImputed,
    Boolean isOtherIncomeImputed,

    // Health insurance
    CoveredByHealthInsurance coveredByHealthInsurance,
    HopwaNoInsuranceReason noInsuranceReason,
    Boolean hasVawaProtectedHealthInfo,

    // Disability information
    DisabilityType physicalDisability,
    DisabilityType developmentalDisability,
    DisabilityType chronicHealthCondition,
    DisabilityType hivAids,
    DisabilityType mentalHealthDisorder,
    DisabilityType substanceUseDisorder,
    Boolean hasDisabilityRelatedVawaInfo,

    // Domestic violence information
    DomesticViolence domesticViolence,
    DomesticViolenceRecency domesticViolenceRecency,
    HmisFivePoint currentlyFleeingDomesticViolence,
    DvRedactionFlag dvRedactionLevel,
    Boolean vawaConfidentialityRequested,

    // RRH move-in specifics
    @PastOrPresent(message = "Residential move-in date cannot be in the future")
    LocalDate residentialMoveInDate,
    ResidentialMoveInDateType moveInType,
    Boolean isSubsidizedByRrh,

    // Audit information
    @NotBlank(message = "Collected by is required")
    @Size(max = 255, message = "Collected by must not exceed 255 characters")
    String collectedBy

) {

    /**
     * Validate conditional logic for domestic violence fields
     */
    public boolean isValidDvConditionalLogic() {
        // If DV = NO, recency and fleeing should not be collected
        if (domesticViolence != null && domesticViolence.noHistory()) {
            if (domesticViolenceRecency != null && domesticViolenceRecency != DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                return false;
            }
            if (currentlyFleeingDomesticViolence != null && currentlyFleeingDomesticViolence != HmisFivePoint.DATA_NOT_COLLECTED) {
                return false;
            }
        }

        // If DV = YES, recency should be provided
        if (domesticViolence != null && domesticViolence.hasHistory()) {
            if (domesticViolenceRecency == null || domesticViolenceRecency == DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate RRH move-in conditional logic
     */
    public boolean isValidRrhMoveInLogic() {
        // If move-in date is provided, move-in type is required
        if (residentialMoveInDate != null) {
            if (moveInType == null || moveInType == ResidentialMoveInDateType.DATA_NOT_COLLECTED) {
                return false;
            }
        }

        // If move-in type is provided, move-in date is required
        if (moveInType != null && moveInType != ResidentialMoveInDateType.DATA_NOT_COLLECTED) {
            if (residentialMoveInDate == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate health insurance conditional logic
     */
    public boolean isValidHealthInsuranceLogic() {
        // If covered = NO, no insurance reason should be provided
        if (coveredByHealthInsurance != null && coveredByHealthInsurance == CoveredByHealthInsurance.NO) {
            // No insurance reason is expected but not required for basic validation
        }

        return true;
    }

    /**
     * Check if all required HMIS fields are provided for data quality
     */
    public boolean meetsHmisDataQualityRequirements() {
        // Income from any source is required
        if (incomeFromAnySource == null || !incomeFromAnySource.isKnownResponse()) {
            return false;
        }

        // All disability fields are required to be known responses
        if (physicalDisability == null || !physicalDisability.isKnownResponse() ||
            developmentalDisability == null || !developmentalDisability.isKnownResponse() ||
            chronicHealthCondition == null || !chronicHealthCondition.isKnownResponse() ||
            hivAids == null || !hivAids.isKnownResponse() ||
            mentalHealthDisorder == null || !mentalHealthDisorder.isKnownResponse() ||
            substanceUseDisorder == null || !substanceUseDisorder.isKnownResponse()) {
            return false;
        }

        // DV history is required
        if (domesticViolence == null || !domesticViolence.isKnownResponse()) {
            return false;
        }

        return true;
    }

    /**
     * Check if this request contains sensitive information requiring special handling
     */
    public boolean containsSensitiveInformation() {
        return (domesticViolence != null && domesticViolence.hasHistory()) ||
               (vawaConfidentialityRequested != null && vawaConfidentialityRequested) ||
               (hasVawaProtectedHealthInfo != null && hasVawaProtectedHealthInfo) ||
               (hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo);
    }

    /**
     * Get appropriate redaction level based on data sensitivity
     */
    public DvRedactionFlag getRecommendedRedactionLevel() {
        if (vawaConfidentialityRequested != null && vawaConfidentialityRequested) {
            return DvRedactionFlag.VICTIM_REQUESTED_CONFIDENTIALITY;
        }

        if (domesticViolence != null && domesticViolence.hasHistory()) {
            if (currentlyFleeingDomesticViolence != null && currentlyFleeingDomesticViolence.isYes()) {
                return DvRedactionFlag.REDACT_FOR_NON_DV_SPECIALISTS;
            }
            if (domesticViolenceRecency != null && domesticViolenceRecency.isVeryRecent()) {
                return DvRedactionFlag.REDACT_FOR_NON_DV_SPECIALISTS;
            }
            return DvRedactionFlag.REDACT_FOR_GENERAL_STAFF;
        }

        return DvRedactionFlag.NO_REDACTION;
    }
}