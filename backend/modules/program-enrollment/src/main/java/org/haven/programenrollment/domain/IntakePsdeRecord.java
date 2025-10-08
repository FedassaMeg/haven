package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Enhanced PSDE Record for Intake & Assessment Workflows
 * Represents comprehensive PSDE data collection per HUD/VAWA guidance
 * with enhanced privacy controls and conditional validation
 */
public class IntakePsdeRecord {

    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate informationDate;
    private IntakeDataCollectionStage collectionStage;

    // Income & Benefits (4.02-4.03) with enhanced detail
    private Integer totalMonthlyIncome;
    private IncomeFromAnySource incomeFromAnySource;
    private Boolean isEarnedIncomeImputed;
    private Boolean isOtherIncomeImputed;

    // Health Insurance (4.04) with VAWA enhancements
    private CoveredByHealthInsurance coveredByHealthInsurance;
    private HopwaNoInsuranceReason noInsuranceReason;
    private Boolean hasVawaProtectedHealthInfo;

    // Disability Information (4.05-4.10) with redaction flags
    private DisabilityType physicalDisability;
    private DisabilityType developmentalDisability;
    private DisabilityType chronicHealthCondition;
    private DisabilityType hivAids;
    private DisabilityType mentalHealthDisorder;
    private DisabilityType substanceUseDisorder;
    private Boolean hasDisabilityRelatedVawaInfo;

    // Domestic Violence (4.11) with enhanced VAWA protections
    private DomesticViolence domesticViolence;
    private DomesticViolenceRecency domesticViolenceRecency;
    private HmisFivePoint currentlyFleeingDomesticViolence;
    private DvRedactionFlag dvRedactionLevel;
    private Boolean vawaConfidentialityRequested;

    // RRH Move-in specifics
    private LocalDate residentialMoveInDate;
    private ResidentialMoveInDateType moveInType;
    private Boolean isSubsidizedByRrh;

    // Data quality and audit fields
    private String collectedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isCorrection;
    private UUID correctsRecordId;

    // Lifecycle management fields
    private Instant effectiveStart;
    private Instant effectiveEnd;
    private Integer version;
    private String updatedBy;
    private String lifecycleStatus; // Will map to IntakePsdeLifecycleStatus enum
    private Instant supersededAt;
    private String supersededBy;
    private UUID supersedes;
    private Boolean isBackdated;
    private String backdatingReason;
    private Instant correctedAt;
    private String correctedBy;
    private String correctionReason; // Will map to CorrectionReason enum
    private String idempotencyKey;

    public IntakePsdeRecord() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isCorrection = false;
        this.dvRedactionLevel = DvRedactionFlag.NO_REDACTION;
        this.vawaConfidentialityRequested = false;
        this.hasVawaProtectedHealthInfo = false;
        this.hasDisabilityRelatedVawaInfo = false;
    }

    /**
     * Create PSDE record for intake assessment
     */
    public static IntakePsdeRecord createForIntake(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate assessmentDate,
            String collectedBy) {

        IntakePsdeRecord record = new IntakePsdeRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = assessmentDate;
        record.collectionStage = IntakeDataCollectionStage.INITIAL_INTAKE;
        record.collectedBy = collectedBy;

        return record;
    }

    /**
     * Create comprehensive assessment record
     */
    public static IntakePsdeRecord createForComprehensiveAssessment(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate assessmentDate,
            String collectedBy) {

        IntakePsdeRecord record = new IntakePsdeRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = assessmentDate;
        record.collectionStage = IntakeDataCollectionStage.COMPREHENSIVE_ASSESSMENT;
        record.collectedBy = collectedBy;

        return record;
    }

    /**
     * Create PSDE record for lifecycle management
     */
    public static IntakePsdeRecord createForLifecycle(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate informationDate,
            IntakeDataCollectionStage collectionStage,
            String collectedBy) {

        IntakePsdeRecord record = new IntakePsdeRecord();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.informationDate = informationDate;
        record.collectionStage = collectionStage;
        record.collectedBy = collectedBy;
        record.version = 1;
        record.isBackdated = false;

        return record;
    }

    /**
     * Create a deep copy of this record for versioning
     */
    public IntakePsdeRecord createCopy() {
        IntakePsdeRecord copy = new IntakePsdeRecord();

        // Copy domain data
        copy.enrollmentId = this.enrollmentId;
        copy.clientId = this.clientId;
        copy.informationDate = this.informationDate;
        copy.collectionStage = this.collectionStage;

        // Copy PSDE data
        copy.totalMonthlyIncome = this.totalMonthlyIncome;
        copy.incomeFromAnySource = this.incomeFromAnySource;
        copy.isEarnedIncomeImputed = this.isEarnedIncomeImputed;
        copy.isOtherIncomeImputed = this.isOtherIncomeImputed;
        copy.coveredByHealthInsurance = this.coveredByHealthInsurance;
        copy.noInsuranceReason = this.noInsuranceReason;
        copy.hasVawaProtectedHealthInfo = this.hasVawaProtectedHealthInfo;
        copy.physicalDisability = this.physicalDisability;
        copy.developmentalDisability = this.developmentalDisability;
        copy.chronicHealthCondition = this.chronicHealthCondition;
        copy.hivAids = this.hivAids;
        copy.mentalHealthDisorder = this.mentalHealthDisorder;
        copy.substanceUseDisorder = this.substanceUseDisorder;
        copy.hasDisabilityRelatedVawaInfo = this.hasDisabilityRelatedVawaInfo;
        copy.domesticViolence = this.domesticViolence;
        copy.domesticViolenceRecency = this.domesticViolenceRecency;
        copy.currentlyFleeingDomesticViolence = this.currentlyFleeingDomesticViolence;
        copy.dvRedactionLevel = this.dvRedactionLevel;
        copy.vawaConfidentialityRequested = this.vawaConfidentialityRequested;
        copy.residentialMoveInDate = this.residentialMoveInDate;
        copy.moveInType = this.moveInType;
        copy.isSubsidizedByRrh = this.isSubsidizedByRrh;

        // Copy audit fields (original values)
        copy.collectedBy = this.collectedBy;
        copy.createdAt = this.createdAt;
        copy.isCorrection = this.isCorrection;
        copy.correctsRecordId = this.correctsRecordId;

        return copy;
    }

    /**
     * Update income information with HMIS validations
     */
    public void updateIncomeInformation(
            Integer totalMonthlyIncome,
            IncomeFromAnySource incomeFromAnySource,
            Boolean isEarnedIncomeImputed,
            Boolean isOtherIncomeImputed) {

        this.totalMonthlyIncome = totalMonthlyIncome;
        this.incomeFromAnySource = incomeFromAnySource;
        this.isEarnedIncomeImputed = isEarnedIncomeImputed;
        this.isOtherIncomeImputed = isOtherIncomeImputed;
        this.updatedAt = Instant.now();
    }

    /**
     * Update health insurance with VAWA considerations
     */
    public void updateHealthInsurance(
            CoveredByHealthInsurance covered,
            HopwaNoInsuranceReason noInsuranceReason,
            Boolean hasVawaProtectedInfo) {

        this.coveredByHealthInsurance = covered;
        this.noInsuranceReason = noInsuranceReason;
        this.hasVawaProtectedHealthInfo = hasVawaProtectedInfo;
        this.updatedAt = Instant.now();
    }

    /**
     * Update disability information with VAWA protections
     */
    public void updateDisabilityInformation(
            DisabilityType physicalDisability,
            DisabilityType developmentalDisability,
            DisabilityType chronicHealthCondition,
            DisabilityType hivAids,
            DisabilityType mentalHealthDisorder,
            DisabilityType substanceUseDisorder,
            Boolean hasVawaRelatedInfo) {

        this.physicalDisability = physicalDisability;
        this.developmentalDisability = developmentalDisability;
        this.chronicHealthCondition = chronicHealthCondition;
        this.hivAids = hivAids;
        this.mentalHealthDisorder = mentalHealthDisorder;
        this.substanceUseDisorder = substanceUseDisorder;
        this.hasDisabilityRelatedVawaInfo = hasVawaRelatedInfo;
        this.updatedAt = Instant.now();
    }

    /**
     * Update domestic violence information with enhanced VAWA protections
     */
    public void updateDomesticViolenceInformation(
            DomesticViolence dvHistory,
            DomesticViolenceRecency recency,
            HmisFivePoint currentlyFleeing,
            DvRedactionFlag redactionLevel,
            Boolean confidentialityRequested) {

        this.domesticViolence = dvHistory;
        this.domesticViolenceRecency = recency;
        this.currentlyFleeingDomesticViolence = currentlyFleeing;
        this.dvRedactionLevel = redactionLevel;
        this.vawaConfidentialityRequested = confidentialityRequested;
        this.updatedAt = Instant.now();
    }

    /**
     * Update RRH move-in information
     */
    public void updateRrhMoveInInformation(
            LocalDate moveInDate,
            ResidentialMoveInDateType moveInType,
            Boolean isSubsidized) {

        this.residentialMoveInDate = moveInDate;
        this.moveInType = moveInType;
        this.isSubsidizedByRrh = isSubsidized;
        this.updatedAt = Instant.now();
    }

    /**
     * Validate HMIS data quality requirements
     */
    public boolean meetsHmisDataQuality() {
        // Income data quality
        if (incomeFromAnySource == null || !incomeFromAnySource.isKnownResponse()) {
            return false;
        }

        // Disability data quality
        if (physicalDisability == null || !physicalDisability.isKnownResponse() ||
            developmentalDisability == null || !developmentalDisability.isKnownResponse() ||
            chronicHealthCondition == null || !chronicHealthCondition.isKnownResponse() ||
            hivAids == null || !hivAids.isKnownResponse() ||
            mentalHealthDisorder == null || !mentalHealthDisorder.isKnownResponse() ||
            substanceUseDisorder == null || !substanceUseDisorder.isKnownResponse()) {
            return false;
        }

        // DV data quality with conditional requirements
        if (domesticViolence == null || !domesticViolence.isKnownResponse()) {
            return false;
        }

        // If DV = YES, recency is required
        if (domesticViolence.hasHistory()) {
            if (domesticViolenceRecency == null || !domesticViolenceRecency.isKnownResponse()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if DV data requires special handling
     */
    public boolean requiresDvRedaction() {
        return dvRedactionLevel != DvRedactionFlag.NO_REDACTION ||
               vawaConfidentialityRequested ||
               (domesticViolence != null && domesticViolence.requiresConfidentialHandling());
    }

    /**
     * Check if this is high-sensitivity DV case
     */
    public boolean isHighSensitivityDvCase() {
        return (currentlyFleeingDomesticViolence != null && currentlyFleeingDomesticViolence.isYes()) ||
               (domesticViolenceRecency != null && domesticViolenceRecency.isVeryRecent()) ||
               vawaConfidentialityRequested;
    }

    /**
     * Validate conditional logic for DV fields
     */
    public boolean hasDvConditionalLogicErrors() {
        // If DV = NO, recency and fleeing should not be collected
        if (domesticViolence != null && domesticViolence.noHistory()) {
            if ((domesticViolenceRecency != null && domesticViolenceRecency != DomesticViolenceRecency.DATA_NOT_COLLECTED) ||
                (currentlyFleeingDomesticViolence != null && currentlyFleeingDomesticViolence != HmisFivePoint.DATA_NOT_COLLECTED)) {
                return true;
            }
        }

        // If DV = YES, recency should be collected
        if (domesticViolence != null && domesticViolence.hasHistory()) {
            if (domesticViolenceRecency == null || domesticViolenceRecency == DomesticViolenceRecency.DATA_NOT_COLLECTED) {
                return true;
            }
        }

        return false;
    }

    // Getters and setters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getInformationDate() { return informationDate; }
    public IntakeDataCollectionStage getCollectionStage() { return collectionStage; }
    public Integer getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public IncomeFromAnySource getIncomeFromAnySource() { return incomeFromAnySource; }
    public CoveredByHealthInsurance getCoveredByHealthInsurance() { return coveredByHealthInsurance; }
    public DisabilityType getPhysicalDisability() { return physicalDisability; }
    public DisabilityType getDevelopmentalDisability() { return developmentalDisability; }
    public DisabilityType getChronicHealthCondition() { return chronicHealthCondition; }
    public DisabilityType getHivAids() { return hivAids; }
    public DisabilityType getMentalHealthDisorder() { return mentalHealthDisorder; }
    public DisabilityType getSubstanceUseDisorder() { return substanceUseDisorder; }
    public DomesticViolence getDomesticViolence() { return domesticViolence; }
    public DomesticViolenceRecency getDomesticViolenceRecency() { return domesticViolenceRecency; }
    public HmisFivePoint getCurrentlyFleeingDomesticViolence() { return currentlyFleeingDomesticViolence; }
    public DvRedactionFlag getDvRedactionLevel() { return dvRedactionLevel; }
    public Boolean getVawaConfidentialityRequested() { return vawaConfidentialityRequested; }
    public LocalDate getResidentialMoveInDate() { return residentialMoveInDate; }
    public ResidentialMoveInDateType getMoveInType() { return moveInType; }
    public Boolean getIsSubsidizedByRrh() { return isSubsidizedByRrh; }
    public Boolean getHasVawaProtectedHealthInfo() { return hasVawaProtectedHealthInfo; }
    public Boolean getHasDisabilityRelatedVawaInfo() { return hasDisabilityRelatedVawaInfo; }
    public Boolean getIsEarnedIncomeImputed() { return isEarnedIncomeImputed; }
    public Boolean getIsOtherIncomeImputed() { return isOtherIncomeImputed; }
    public HopwaNoInsuranceReason getNoInsuranceReason() { return noInsuranceReason; }
    public String getCollectedBy() { return collectedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Boolean getIsCorrection() { return isCorrection; }
    public void setIsCorrection(Boolean isCorrection) { this.isCorrection = isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public void setCorrectsRecordId(UUID correctsRecordId) { this.correctsRecordId = correctsRecordId; }

    // Lifecycle management getters and setters
    public Instant getEffectiveStart() { return effectiveStart; }
    public void setEffectiveStart(Instant effectiveStart) { this.effectiveStart = effectiveStart; }

    public Instant getEffectiveEnd() { return effectiveEnd; }
    public void setEffectiveEnd(Instant effectiveEnd) { this.effectiveEnd = effectiveEnd; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }

    public Instant getSupersededAt() { return supersededAt; }
    public void setSupersededAt(Instant supersededAt) { this.supersededAt = supersededAt; }

    public String getSupersededBy() { return supersededBy; }
    public void setSupersededBy(String supersededBy) { this.supersededBy = supersededBy; }

    public UUID getSupersedes() { return supersedes; }
    public void setSupersedes(UUID supersedes) { this.supersedes = supersedes; }

    public Boolean getIsBackdated() { return isBackdated; }
    public void setIsBackdated(Boolean isBackdated) { this.isBackdated = isBackdated; }

    public String getBackdatingReason() { return backdatingReason; }
    public void setBackdatingReason(String backdatingReason) { this.backdatingReason = backdatingReason; }

    public Instant getCorrectedAt() { return correctedAt; }
    public void setCorrectedAt(Instant correctedAt) { this.correctedAt = correctedAt; }

    public String getCorrectedBy() { return correctedBy; }
    public void setCorrectedBy(String correctedBy) { this.correctedBy = correctedBy; }

    public String getCorrectionReason() { return correctionReason; }
    public void setCorrectionReason(String correctionReason) { this.correctionReason = correctionReason; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public void setRecordId(UUID recordId) { this.recordId = recordId; }
    public void setInformationDate(LocalDate informationDate) { this.informationDate = informationDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntakePsdeRecord that = (IntakePsdeRecord) o;
        return Objects.equals(recordId, that.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}