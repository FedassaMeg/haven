package org.haven.api.enrollments.dto;

import org.haven.shared.vo.hmis.*;
import java.time.LocalDate;
import java.time.Instant;

/**
 * REST DTO for Intake PSDE record responses
 * Supports role-based redaction for sensitive data
 */
public record IntakePsdeResponse(

    String recordId,
    String enrollmentId,
    String clientId,
    LocalDate informationDate,
    IntakeDataCollectionStage collectionStage,

    // Income information (generally not redacted)
    Integer totalMonthlyIncome,
    IncomeFromAnySource incomeFromAnySource,
    Boolean isEarnedIncomeImputed,
    Boolean isOtherIncomeImputed,

    // Health insurance (may be redacted for VAWA)
    CoveredByHealthInsurance coveredByHealthInsurance,
    HopwaNoInsuranceReason noInsuranceReason,
    Boolean hasVawaProtectedHealthInfo,

    // Disability information (may be redacted for VAWA)
    DisabilityType physicalDisability,
    DisabilityType developmentalDisability,
    DisabilityType chronicHealthCondition,
    DisabilityType hivAids,
    DisabilityType mentalHealthDisorder,
    DisabilityType substanceUseDisorder,
    Boolean hasDisabilityRelatedVawaInfo,

    // Domestic violence information (subject to redaction)
    DomesticViolence domesticViolence,
    DomesticViolenceRecency domesticViolenceRecency,
    HmisFivePoint currentlyFleeingDomesticViolence,
    DvRedactionFlag dvRedactionLevel,
    Boolean vawaConfidentialityRequested,

    // RRH move-in specifics
    LocalDate residentialMoveInDate,
    ResidentialMoveInDateType moveInType,
    Boolean isSubsidizedByRrh,

    // Audit and metadata
    String collectedBy,
    Instant createdAt,
    Instant updatedAt,
    Boolean isCorrection,

    // Data quality indicators
    Boolean meetsHmisDataQuality,
    Boolean requiresDvRedaction,
    Boolean isHighSensitivityDvCase

) {

    /**
     * Create redacted version for users without DV access
     */
    public IntakePsdeResponse withRedactedDvInformation() {
        return new IntakePsdeResponse(
            recordId,
            enrollmentId,
            clientId,
            informationDate,
            collectionStage,

            // Keep income information
            totalMonthlyIncome,
            incomeFromAnySource,
            isEarnedIncomeImputed,
            isOtherIncomeImputed,

            // Keep health insurance if not VAWA-protected
            hasVawaProtectedHealthInfo != null && hasVawaProtectedHealthInfo
                ? null : coveredByHealthInsurance,
            hasVawaProtectedHealthInfo != null && hasVawaProtectedHealthInfo
                ? null : noInsuranceReason,
            false,

            // Keep disability info if not VAWA-protected
            hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo
                ? DisabilityType.DATA_NOT_COLLECTED : physicalDisability,
            hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo
                ? DisabilityType.DATA_NOT_COLLECTED : developmentalDisability,
            hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo
                ? DisabilityType.DATA_NOT_COLLECTED : chronicHealthCondition,
            hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo
                ? DisabilityType.DATA_NOT_COLLECTED : hivAids,
            hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo
                ? DisabilityType.DATA_NOT_COLLECTED : mentalHealthDisorder,
            hasDisabilityRelatedVawaInfo != null && hasDisabilityRelatedVawaInfo
                ? DisabilityType.DATA_NOT_COLLECTED : substanceUseDisorder,
            false,

            // Redact all DV information
            DomesticViolence.DATA_NOT_COLLECTED,
            DomesticViolenceRecency.DATA_NOT_COLLECTED,
            HmisFivePoint.DATA_NOT_COLLECTED,
            DvRedactionFlag.REDACT_FOR_GENERAL_STAFF,
            false,

            // Keep RRH information (generally not sensitive)
            residentialMoveInDate,
            moveInType,
            isSubsidizedByRrh,

            // Keep audit information
            collectedBy,
            createdAt,
            updatedAt,
            isCorrection,

            // Update data quality flags
            false, // HMIS data quality affected by redaction
            true,  // Requires DV redaction
            false  // High sensitivity masked
        );
    }

    /**
     * Create version redacted for general staff (partial DV info visible)
     */
    public IntakePsdeResponse withPartialDvRedaction() {
        return new IntakePsdeResponse(
            recordId,
            enrollmentId,
            clientId,
            informationDate,
            collectionStage,

            // Keep all income information
            totalMonthlyIncome,
            incomeFromAnySource,
            isEarnedIncomeImputed,
            isOtherIncomeImputed,

            // Keep health insurance
            coveredByHealthInsurance,
            noInsuranceReason,
            hasVawaProtectedHealthInfo,

            // Keep disability info
            physicalDisability,
            developmentalDisability,
            chronicHealthCondition,
            hivAids,
            mentalHealthDisorder,
            substanceUseDisorder,
            hasDisabilityRelatedVawaInfo,

            // Show basic DV status only, redact details
            domesticViolence,
            DomesticViolenceRecency.DATA_NOT_COLLECTED, // Redact recency
            HmisFivePoint.DATA_NOT_COLLECTED,          // Redact fleeing status
            DvRedactionFlag.REDACT_FOR_GENERAL_STAFF,
            false, // Don't show confidentiality request status

            // Keep RRH information
            residentialMoveInDate,
            moveInType,
            isSubsidizedByRrh,

            // Keep audit information
            collectedBy,
            createdAt,
            updatedAt,
            isCorrection,

            // Update data quality flags
            false, // HMIS data quality affected by partial redaction
            true,  // Requires DV redaction
            false  // High sensitivity masked
        );
    }

    /**
     * Check if user has access to full record based on role
     */
    public static boolean canAccessFullRecord(String userRole, DvRedactionFlag redactionLevel) {
        if (redactionLevel == null) {
            return true;
        }

        return !redactionLevel.blocksAccessForRole(userRole);
    }

    /**
     * Create appropriate redacted version based on user role and redaction level
     */
    public IntakePsdeResponse redactForRole(String userRole) {
        if (dvRedactionLevel == null || !dvRedactionLevel.blocksAccessForRole(userRole)) {
            return this; // Full access
        }

        if (dvRedactionLevel == DvRedactionFlag.REDACT_FOR_GENERAL_STAFF) {
            return this.withPartialDvRedaction();
        }

        return this.withRedactedDvInformation();
    }
}