package org.haven.api.enrollments.services;

import org.haven.api.enrollments.dto.IntakePsdeRequest;
import org.haven.api.enrollments.dto.IntakePsdeResponse;
import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.hmis.DvRedactionFlag;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Mapper for converting between IntakePsde DTOs and domain objects
 * Handles the translation between API layer and domain layer
 */
@Component
public class IntakePsdeDtoMapper {

    /**
     * Convert request DTO to domain record
     */
    public IntakePsdeRecord requestToRecord(
            IntakePsdeRequest request,
            UUID enrollmentId,
            UUID clientId) {

        // Create new record
        IntakePsdeRecord record = IntakePsdeRecord.createForIntake(
            new ProgramEnrollmentId(enrollmentId),
            new ClientId(clientId),
            request.informationDate() != null ? request.informationDate() : LocalDate.now(),
            request.collectedBy()
        );

        // Update all fields from request
        if (request.totalMonthlyIncome() != null || request.incomeFromAnySource() != null) {
            record.updateIncomeInformation(
                request.totalMonthlyIncome(),
                request.incomeFromAnySource(),
                request.isEarnedIncomeImputed(),
                request.isOtherIncomeImputed()
            );
        }

        if (request.coveredByHealthInsurance() != null || request.hasVawaProtectedHealthInfo() != null) {
            record.updateHealthInsurance(
                request.coveredByHealthInsurance(),
                request.noInsuranceReason(),
                request.hasVawaProtectedHealthInfo()
            );
        }

        if (hasAnyDisabilityInfo(request)) {
            record.updateDisabilityInformation(
                request.physicalDisability(),
                request.developmentalDisability(),
                request.chronicHealthCondition(),
                request.hivAids(),
                request.mentalHealthDisorder(),
                request.substanceUseDisorder(),
                request.hasDisabilityRelatedVawaInfo()
            );
        }

        if (request.domesticViolence() != null) {
            record.updateDomesticViolenceInformation(
                request.domesticViolence(),
                request.domesticViolenceRecency(),
                request.currentlyFleeingDomesticViolence(),
                request.dvRedactionLevel() != null ? request.dvRedactionLevel() : DvRedactionFlag.NO_REDACTION,
                request.vawaConfidentialityRequested() != null ? request.vawaConfidentialityRequested() : false
            );
        }

        if (request.residentialMoveInDate() != null || request.moveInType() != null) {
            record.updateRrhMoveInInformation(
                request.residentialMoveInDate(),
                request.moveInType(),
                request.isSubsidizedByRrh()
            );
        }

        return record;
    }

    /**
     * Convert domain record to response DTO
     */
    public IntakePsdeResponse recordToResponse(IntakePsdeRecord record) {
        return new IntakePsdeResponse(
            record.getRecordId().toString(),
            record.getEnrollmentId().toString(),
            record.getClientId().toString(),
            record.getInformationDate(),
            record.getCollectionStage(),

            // Income information
            record.getTotalMonthlyIncome(),
            record.getIncomeFromAnySource(),
            record.getIsEarnedIncomeImputed(),
            record.getIsOtherIncomeImputed(),

            // Health insurance
            record.getCoveredByHealthInsurance(),
            record.getNoInsuranceReason(),
            record.getHasVawaProtectedHealthInfo(),

            // Disability information
            record.getPhysicalDisability(),
            record.getDevelopmentalDisability(),
            record.getChronicHealthCondition(),
            record.getHivAids(),
            record.getMentalHealthDisorder(),
            record.getSubstanceUseDisorder(),
            record.getHasDisabilityRelatedVawaInfo(),

            // Domestic violence
            record.getDomesticViolence(),
            record.getDomesticViolenceRecency(),
            record.getCurrentlyFleeingDomesticViolence(),
            record.getDvRedactionLevel(),
            record.getVawaConfidentialityRequested(),

            // RRH move-in
            record.getResidentialMoveInDate(),
            record.getMoveInType(),
            record.getIsSubsidizedByRrh(),

            // Audit fields
            record.getCollectedBy(),
            record.getCreatedAt(),
            record.getUpdatedAt(),
            record.getIsCorrection(),

            // Data quality indicators
            record.meetsHmisDataQuality(),
            record.requiresDvRedaction(),
            record.isHighSensitivityDvCase()
        );
    }

    /**
     * Apply updates from request to existing record
     */
    public void applyUpdatesToRecord(IntakePsdeRecord record, IntakePsdeRequest request) {
        // Update income information
        if (request.totalMonthlyIncome() != null || request.incomeFromAnySource() != null) {
            record.updateIncomeInformation(
                request.totalMonthlyIncome(),
                request.incomeFromAnySource(),
                request.isEarnedIncomeImputed(),
                request.isOtherIncomeImputed()
            );
        }

        // Update health insurance
        if (request.coveredByHealthInsurance() != null || request.hasVawaProtectedHealthInfo() != null) {
            record.updateHealthInsurance(
                request.coveredByHealthInsurance(),
                request.noInsuranceReason(),
                request.hasVawaProtectedHealthInfo()
            );
        }

        // Update disability information
        if (hasAnyDisabilityInfo(request)) {
            record.updateDisabilityInformation(
                request.physicalDisability(),
                request.developmentalDisability(),
                request.chronicHealthCondition(),
                request.hivAids(),
                request.mentalHealthDisorder(),
                request.substanceUseDisorder(),
                request.hasDisabilityRelatedVawaInfo()
            );
        }

        // Update domestic violence information
        if (request.domesticViolence() != null) {
            record.updateDomesticViolenceInformation(
                request.domesticViolence(),
                request.domesticViolenceRecency(),
                request.currentlyFleeingDomesticViolence(),
                request.dvRedactionLevel() != null ? request.dvRedactionLevel() : DvRedactionFlag.NO_REDACTION,
                request.vawaConfidentialityRequested() != null ? request.vawaConfidentialityRequested() : false
            );
        }

        // Update RRH move-in information
        if (request.residentialMoveInDate() != null || request.moveInType() != null) {
            record.updateRrhMoveInInformation(
                request.residentialMoveInDate(),
                request.moveInType(),
                request.isSubsidizedByRrh()
            );
        }
    }

    /**
     * Check if request has any disability information
     */
    private boolean hasAnyDisabilityInfo(IntakePsdeRequest request) {
        return request.physicalDisability() != null ||
               request.developmentalDisability() != null ||
               request.chronicHealthCondition() != null ||
               request.hivAids() != null ||
               request.mentalHealthDisorder() != null ||
               request.substanceUseDisorder() != null ||
               request.hasDisabilityRelatedVawaInfo() != null;
    }
}