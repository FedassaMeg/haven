package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.shared.vo.hmis.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates updates to be applied to an IntakePsdeRecord
 * Supports partial updates and tracks which fields are being changed
 */
public class IntakePsdeUpdateRequest {

    // Income & Benefits updates
    private Integer totalMonthlyIncome;
    private IncomeFromAnySource incomeFromAnySource;
    private Boolean isEarnedIncomeImputed;
    private Boolean isOtherIncomeImputed;

    // Health Insurance updates
    private CoveredByHealthInsurance coveredByHealthInsurance;
    private HopwaNoInsuranceReason noInsuranceReason;
    private Boolean hasVawaProtectedHealthInfo;

    // Disability updates
    private DisabilityType physicalDisability;
    private DisabilityType developmentalDisability;
    private DisabilityType chronicHealthCondition;
    private DisabilityType hivAids;
    private DisabilityType mentalHealthDisorder;
    private DisabilityType substanceUseDisorder;
    private Boolean hasDisabilityRelatedVawaInfo;

    // Domestic Violence updates
    private DomesticViolence domesticViolence;
    private DomesticViolenceRecency domesticViolenceRecency;
    private HmisFivePoint currentlyFleeingDomesticViolence;
    private DvRedactionFlag dvRedactionLevel;
    private Boolean vawaConfidentialityRequested;

    // RRH Move-in updates
    private LocalDate residentialMoveInDate;
    private ResidentialMoveInDateType moveInType;
    private Boolean isSubsidizedByRrh;

    // Meta fields
    private LocalDate informationDate;
    private String updateReason;

    // Track which fields are being updated
    private final List<String> changedFields = new ArrayList<>();

    public IntakePsdeUpdateRequest() {}

    /**
     * Apply updates to the target record
     */
    public void applyTo(IntakePsdeRecord record) {
        if (totalMonthlyIncome != null) {
            record.updateIncomeInformation(
                totalMonthlyIncome,
                incomeFromAnySource != null ? incomeFromAnySource : record.getIncomeFromAnySource(),
                isEarnedIncomeImputed != null ? isEarnedIncomeImputed : record.getIsEarnedIncomeImputed(),
                isOtherIncomeImputed != null ? isOtherIncomeImputed : record.getIsOtherIncomeImputed()
            );
        }

        if (coveredByHealthInsurance != null || noInsuranceReason != null || hasVawaProtectedHealthInfo != null) {
            record.updateHealthInsurance(
                coveredByHealthInsurance != null ? coveredByHealthInsurance : record.getCoveredByHealthInsurance(),
                noInsuranceReason != null ? noInsuranceReason : record.getNoInsuranceReason(),
                hasVawaProtectedHealthInfo != null ? hasVawaProtectedHealthInfo : record.getHasVawaProtectedHealthInfo()
            );
        }

        if (hasAnyDisabilityUpdate()) {
            record.updateDisabilityInformation(
                physicalDisability != null ? physicalDisability : record.getPhysicalDisability(),
                developmentalDisability != null ? developmentalDisability : record.getDevelopmentalDisability(),
                chronicHealthCondition != null ? chronicHealthCondition : record.getChronicHealthCondition(),
                hivAids != null ? hivAids : record.getHivAids(),
                mentalHealthDisorder != null ? mentalHealthDisorder : record.getMentalHealthDisorder(),
                substanceUseDisorder != null ? substanceUseDisorder : record.getSubstanceUseDisorder(),
                hasDisabilityRelatedVawaInfo != null ? hasDisabilityRelatedVawaInfo : record.getHasDisabilityRelatedVawaInfo()
            );
        }

        if (hasAnyDvUpdate()) {
            record.updateDomesticViolenceInformation(
                domesticViolence != null ? domesticViolence : record.getDomesticViolence(),
                domesticViolenceRecency != null ? domesticViolenceRecency : record.getDomesticViolenceRecency(),
                currentlyFleeingDomesticViolence != null ? currentlyFleeingDomesticViolence : record.getCurrentlyFleeingDomesticViolence(),
                dvRedactionLevel != null ? dvRedactionLevel : record.getDvRedactionLevel(),
                vawaConfidentialityRequested != null ? vawaConfidentialityRequested : record.getVawaConfidentialityRequested()
            );
        }

        if (hasAnyRrhUpdate()) {
            record.updateRrhMoveInInformation(
                residentialMoveInDate != null ? residentialMoveInDate : record.getResidentialMoveInDate(),
                moveInType != null ? moveInType : record.getMoveInType(),
                isSubsidizedByRrh != null ? isSubsidizedByRrh : record.getIsSubsidizedByRrh()
            );
        }

        if (informationDate != null) {
            record.setInformationDate(informationDate);
        }
    }

    /**
     * Get list of fields that are being changed
     */
    public String[] getChangedFields() {
        return changedFields.toArray(new String[0]);
    }

    /**
     * Add a field to the changed fields list
     */
    private void addChangedField(String fieldName) {
        if (!changedFields.contains(fieldName)) {
            changedFields.add(fieldName);
        }
    }

    // Helper methods
    private boolean hasAnyDisabilityUpdate() {
        return physicalDisability != null ||
               developmentalDisability != null ||
               chronicHealthCondition != null ||
               hivAids != null ||
               mentalHealthDisorder != null ||
               substanceUseDisorder != null ||
               hasDisabilityRelatedVawaInfo != null;
    }

    private boolean hasAnyDvUpdate() {
        return domesticViolence != null ||
               domesticViolenceRecency != null ||
               currentlyFleeingDomesticViolence != null ||
               dvRedactionLevel != null ||
               vawaConfidentialityRequested != null;
    }

    private boolean hasAnyRrhUpdate() {
        return residentialMoveInDate != null ||
               moveInType != null ||
               isSubsidizedByRrh != null;
    }

    // Builder pattern for easier construction
    public static class Builder {
        private final IntakePsdeUpdateRequest request = new IntakePsdeUpdateRequest();

        public Builder withIncome(Integer totalMonthlyIncome, IncomeFromAnySource incomeFromAnySource) {
            request.totalMonthlyIncome = totalMonthlyIncome;
            request.incomeFromAnySource = incomeFromAnySource;
            request.addChangedField("income");
            return this;
        }

        public Builder withIncomeImputation(Boolean isEarnedIncomeImputed, Boolean isOtherIncomeImputed) {
            request.isEarnedIncomeImputed = isEarnedIncomeImputed;
            request.isOtherIncomeImputed = isOtherIncomeImputed;
            request.addChangedField("income_imputation");
            return this;
        }

        public Builder withHealthInsurance(CoveredByHealthInsurance covered, HopwaNoInsuranceReason noInsuranceReason) {
            request.coveredByHealthInsurance = covered;
            request.noInsuranceReason = noInsuranceReason;
            request.addChangedField("health_insurance");
            return this;
        }

        public Builder withVawaHealthProtection(Boolean hasVawaProtectedHealthInfo) {
            request.hasVawaProtectedHealthInfo = hasVawaProtectedHealthInfo;
            request.addChangedField("vawa_health_protection");
            return this;
        }

        public Builder withPhysicalDisability(DisabilityType physicalDisability) {
            request.physicalDisability = physicalDisability;
            request.addChangedField("physical_disability");
            return this;
        }

        public Builder withDevelopmentalDisability(DisabilityType developmentalDisability) {
            request.developmentalDisability = developmentalDisability;
            request.addChangedField("developmental_disability");
            return this;
        }

        public Builder withChronicHealthCondition(DisabilityType chronicHealthCondition) {
            request.chronicHealthCondition = chronicHealthCondition;
            request.addChangedField("chronic_health_condition");
            return this;
        }

        public Builder withHivAids(DisabilityType hivAids) {
            request.hivAids = hivAids;
            request.addChangedField("hiv_aids");
            return this;
        }

        public Builder withMentalHealthDisorder(DisabilityType mentalHealthDisorder) {
            request.mentalHealthDisorder = mentalHealthDisorder;
            request.addChangedField("mental_health_disorder");
            return this;
        }

        public Builder withSubstanceUseDisorder(DisabilityType substanceUseDisorder) {
            request.substanceUseDisorder = substanceUseDisorder;
            request.addChangedField("substance_use_disorder");
            return this;
        }

        public Builder withVawaDisabilityProtection(Boolean hasDisabilityRelatedVawaInfo) {
            request.hasDisabilityRelatedVawaInfo = hasDisabilityRelatedVawaInfo;
            request.addChangedField("vawa_disability_protection");
            return this;
        }

        public Builder withDomesticViolence(DomesticViolence domesticViolence, DomesticViolenceRecency recency) {
            request.domesticViolence = domesticViolence;
            request.domesticViolenceRecency = recency;
            request.addChangedField("domestic_violence");
            return this;
        }

        public Builder withCurrentlyFleeingDv(HmisFivePoint currentlyFleeingDomesticViolence) {
            request.currentlyFleeingDomesticViolence = currentlyFleeingDomesticViolence;
            request.addChangedField("currently_fleeing_dv");
            return this;
        }

        public Builder withDvRedaction(DvRedactionFlag dvRedactionLevel) {
            request.dvRedactionLevel = dvRedactionLevel;
            request.addChangedField("dv_redaction_level");
            return this;
        }

        public Builder withVawaConfidentiality(Boolean vawaConfidentialityRequested) {
            request.vawaConfidentialityRequested = vawaConfidentialityRequested;
            request.addChangedField("vawa_confidentiality");
            return this;
        }

        public Builder withRrhMoveIn(LocalDate residentialMoveInDate, ResidentialMoveInDateType moveInType) {
            request.residentialMoveInDate = residentialMoveInDate;
            request.moveInType = moveInType;
            request.addChangedField("rrh_move_in");
            return this;
        }

        public Builder withRrhSubsidy(Boolean isSubsidizedByRrh) {
            request.isSubsidizedByRrh = isSubsidizedByRrh;
            request.addChangedField("rrh_subsidy");
            return this;
        }

        public Builder withInformationDate(LocalDate informationDate) {
            request.informationDate = informationDate;
            request.addChangedField("information_date");
            return this;
        }

        public Builder withUpdateReason(String updateReason) {
            request.updateReason = updateReason;
            request.addChangedField("update_reason");
            return this;
        }

        public IntakePsdeUpdateRequest build() {
            return request;
        }
    }

    // Getters and setters
    public Integer getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public void setTotalMonthlyIncome(Integer totalMonthlyIncome) {
        this.totalMonthlyIncome = totalMonthlyIncome;
        addChangedField("total_monthly_income");
    }

    public IncomeFromAnySource getIncomeFromAnySource() { return incomeFromAnySource; }
    public void setIncomeFromAnySource(IncomeFromAnySource incomeFromAnySource) {
        this.incomeFromAnySource = incomeFromAnySource;
        addChangedField("income_from_any_source");
    }

    public Boolean getIsEarnedIncomeImputed() { return isEarnedIncomeImputed; }
    public void setIsEarnedIncomeImputed(Boolean isEarnedIncomeImputed) {
        this.isEarnedIncomeImputed = isEarnedIncomeImputed;
        addChangedField("is_earned_income_imputed");
    }

    public Boolean getIsOtherIncomeImputed() { return isOtherIncomeImputed; }
    public void setIsOtherIncomeImputed(Boolean isOtherIncomeImputed) {
        this.isOtherIncomeImputed = isOtherIncomeImputed;
        addChangedField("is_other_income_imputed");
    }

    public CoveredByHealthInsurance getCoveredByHealthInsurance() { return coveredByHealthInsurance; }
    public void setCoveredByHealthInsurance(CoveredByHealthInsurance coveredByHealthInsurance) {
        this.coveredByHealthInsurance = coveredByHealthInsurance;
        addChangedField("covered_by_health_insurance");
    }

    public HopwaNoInsuranceReason getNoInsuranceReason() { return noInsuranceReason; }
    public void setNoInsuranceReason(HopwaNoInsuranceReason noInsuranceReason) {
        this.noInsuranceReason = noInsuranceReason;
        addChangedField("no_insurance_reason");
    }

    public Boolean getHasVawaProtectedHealthInfo() { return hasVawaProtectedHealthInfo; }
    public void setHasVawaProtectedHealthInfo(Boolean hasVawaProtectedHealthInfo) {
        this.hasVawaProtectedHealthInfo = hasVawaProtectedHealthInfo;
        addChangedField("has_vawa_protected_health_info");
    }

    public DisabilityType getPhysicalDisability() { return physicalDisability; }
    public void setPhysicalDisability(DisabilityType physicalDisability) {
        this.physicalDisability = physicalDisability;
        addChangedField("physical_disability");
    }

    public DisabilityType getDevelopmentalDisability() { return developmentalDisability; }
    public void setDevelopmentalDisability(DisabilityType developmentalDisability) {
        this.developmentalDisability = developmentalDisability;
        addChangedField("developmental_disability");
    }

    public DisabilityType getChronicHealthCondition() { return chronicHealthCondition; }
    public void setChronicHealthCondition(DisabilityType chronicHealthCondition) {
        this.chronicHealthCondition = chronicHealthCondition;
        addChangedField("chronic_health_condition");
    }

    public DisabilityType getHivAids() { return hivAids; }
    public void setHivAids(DisabilityType hivAids) {
        this.hivAids = hivAids;
        addChangedField("hiv_aids");
    }

    public DisabilityType getMentalHealthDisorder() { return mentalHealthDisorder; }
    public void setMentalHealthDisorder(DisabilityType mentalHealthDisorder) {
        this.mentalHealthDisorder = mentalHealthDisorder;
        addChangedField("mental_health_disorder");
    }

    public DisabilityType getSubstanceUseDisorder() { return substanceUseDisorder; }
    public void setSubstanceUseDisorder(DisabilityType substanceUseDisorder) {
        this.substanceUseDisorder = substanceUseDisorder;
        addChangedField("substance_use_disorder");
    }

    public Boolean getHasDisabilityRelatedVawaInfo() { return hasDisabilityRelatedVawaInfo; }
    public void setHasDisabilityRelatedVawaInfo(Boolean hasDisabilityRelatedVawaInfo) {
        this.hasDisabilityRelatedVawaInfo = hasDisabilityRelatedVawaInfo;
        addChangedField("has_disability_related_vawa_info");
    }

    public DomesticViolence getDomesticViolence() { return domesticViolence; }
    public void setDomesticViolence(DomesticViolence domesticViolence) {
        this.domesticViolence = domesticViolence;
        addChangedField("domestic_violence");
    }

    public DomesticViolenceRecency getDomesticViolenceRecency() { return domesticViolenceRecency; }
    public void setDomesticViolenceRecency(DomesticViolenceRecency domesticViolenceRecency) {
        this.domesticViolenceRecency = domesticViolenceRecency;
        addChangedField("domestic_violence_recency");
    }

    public HmisFivePoint getCurrentlyFleeingDomesticViolence() { return currentlyFleeingDomesticViolence; }
    public void setCurrentlyFleeingDomesticViolence(HmisFivePoint currentlyFleeingDomesticViolence) {
        this.currentlyFleeingDomesticViolence = currentlyFleeingDomesticViolence;
        addChangedField("currently_fleeing_domestic_violence");
    }

    public DvRedactionFlag getDvRedactionLevel() { return dvRedactionLevel; }
    public void setDvRedactionLevel(DvRedactionFlag dvRedactionLevel) {
        this.dvRedactionLevel = dvRedactionLevel;
        addChangedField("dv_redaction_level");
    }

    public Boolean getVawaConfidentialityRequested() { return vawaConfidentialityRequested; }
    public void setVawaConfidentialityRequested(Boolean vawaConfidentialityRequested) {
        this.vawaConfidentialityRequested = vawaConfidentialityRequested;
        addChangedField("vawa_confidentiality_requested");
    }

    public LocalDate getResidentialMoveInDate() { return residentialMoveInDate; }
    public void setResidentialMoveInDate(LocalDate residentialMoveInDate) {
        this.residentialMoveInDate = residentialMoveInDate;
        addChangedField("residential_move_in_date");
    }

    public ResidentialMoveInDateType getMoveInType() { return moveInType; }
    public void setMoveInType(ResidentialMoveInDateType moveInType) {
        this.moveInType = moveInType;
        addChangedField("move_in_type");
    }

    public Boolean getIsSubsidizedByRrh() { return isSubsidizedByRrh; }
    public void setIsSubsidizedByRrh(Boolean isSubsidizedByRrh) {
        this.isSubsidizedByRrh = isSubsidizedByRrh;
        addChangedField("is_subsidized_by_rrh");
    }

    public LocalDate getInformationDate() { return informationDate; }
    public void setInformationDate(LocalDate informationDate) {
        this.informationDate = informationDate;
        addChangedField("information_date");
    }

    public String getUpdateReason() { return updateReason; }
    public void setUpdateReason(String updateReason) {
        this.updateReason = updateReason;
        addChangedField("update_reason");
    }
}