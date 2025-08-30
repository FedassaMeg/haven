package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HMIS Program Specific Data Elements (PSDE) - FY2024 Data Standards
 * Contains all program-specific data elements collected at enrollment
 * and updated during the enrollment period per HMIS requirements.
 */
public class ProgramSpecificDataElements {
    
    // Income and Sources (4.02)
    private Integer totalMonthlyIncome;
    private List<IncomeSource> incomeSources;
    
    // Non-cash Benefits (4.03)
    private List<NonCashBenefit> nonCashBenefits;
    
    // Health Insurance (4.04)
    private List<HealthInsurance> healthInsurances;
    
    // Disability Information (4.05-4.10)
    private DisabilityType physicalDisability;
    private DisabilityType developmentalDisability;
    private DisabilityType chronicHealthCondition;
    private DisabilityType hivAids;
    private DisabilityType mentalHealthDisorder;
    private DisabilityType substanceUseDisorder;
    
    // Domestic Violence (4.11)
    private DomesticViolence domesticViolence;
    
    // Current Living Situation (4.12)
    private CurrentLivingSituation currentLivingSituation;
    
    // Program-specific dates
    private LocalDate dateOfEngagement;
    private LocalDate bedNightDate;
    
    // Information dates for tracking when data was collected
    private LocalDate incomeInformationDate;
    private LocalDate nonCashBenefitInformationDate;
    private LocalDate healthInsuranceInformationDate;
    private LocalDate disabilityInformationDate;
    private LocalDate domesticViolenceInformationDate;
    
    public ProgramSpecificDataElements() {
        this.incomeSources = new ArrayList<>();
        this.nonCashBenefits = new ArrayList<>();
        this.healthInsurances = new ArrayList<>();
    }
    
    // Income and Sources methods
    public void updateIncomeInformation(Integer totalMonthlyIncome, 
                                      List<IncomeSource> incomeSources,
                                      LocalDate informationDate) {
        this.totalMonthlyIncome = totalMonthlyIncome;
        this.incomeSources = new ArrayList<>(incomeSources);
        this.incomeInformationDate = informationDate;
    }
    
    public void addIncomeSource(IncomeSource source) {
        if (!incomeSources.contains(source)) {
            incomeSources.add(source);
        }
    }
    
    public boolean hasEarnedIncome() {
        return incomeSources.stream().anyMatch(IncomeSource::isEarnedIncome);
    }
    
    public boolean hasBenefitIncome() {
        return incomeSources.stream().anyMatch(IncomeSource::isBenefitIncome);
    }
    
    public boolean hasAnyIncome() {
        return totalMonthlyIncome != null && totalMonthlyIncome > 0;
    }
    
    // Non-cash Benefits methods
    public void updateNonCashBenefits(List<NonCashBenefit> benefits, LocalDate informationDate) {
        this.nonCashBenefits = new ArrayList<>(benefits);
        this.nonCashBenefitInformationDate = informationDate;
    }
    
    public void addNonCashBenefit(NonCashBenefit benefit) {
        if (!nonCashBenefits.contains(benefit)) {
            nonCashBenefits.add(benefit);
        }
    }
    
    public boolean hasNutritionBenefits() {
        return nonCashBenefits.stream().anyMatch(NonCashBenefit::isNutritionBenefit);
    }
    
    public boolean hasTanfBenefits() {
        return nonCashBenefits.stream().anyMatch(NonCashBenefit::isTanfBenefit);
    }
    
    // Health Insurance methods
    public void updateHealthInsurance(List<HealthInsurance> insurances, LocalDate informationDate) {
        this.healthInsurances = new ArrayList<>(insurances);
        this.healthInsuranceInformationDate = informationDate;
    }
    
    public void addHealthInsurance(HealthInsurance insurance) {
        if (!healthInsurances.contains(insurance)) {
            healthInsurances.add(insurance);
        }
    }
    
    public boolean hasHealthInsurance() {
        return !healthInsurances.isEmpty() && 
               healthInsurances.stream().anyMatch(HealthInsurance::isKnownInsurance);
    }
    
    public boolean hasGovernmentInsurance() {
        return healthInsurances.stream().anyMatch(HealthInsurance::isGovernmentInsurance);
    }
    
    public boolean hasPrivateInsurance() {
        return healthInsurances.stream().anyMatch(HealthInsurance::isPrivateInsurance);
    }
    
    // Disability methods
    public void updateDisabilityInformation(DisabilityType physicalDisability,
                                          DisabilityType developmentalDisability,
                                          DisabilityType chronicHealthCondition,
                                          DisabilityType hivAids,
                                          DisabilityType mentalHealthDisorder,
                                          DisabilityType substanceUseDisorder,
                                          LocalDate informationDate) {
        this.physicalDisability = physicalDisability;
        this.developmentalDisability = developmentalDisability;
        this.chronicHealthCondition = chronicHealthCondition;
        this.hivAids = hivAids;
        this.mentalHealthDisorder = mentalHealthDisorder;
        this.substanceUseDisorder = substanceUseDisorder;
        this.disabilityInformationDate = informationDate;
    }
    
    public boolean hasAnyDisability() {
        return (physicalDisability != null && physicalDisability.isAffirmative()) ||
               (developmentalDisability != null && developmentalDisability.isAffirmative()) ||
               (chronicHealthCondition != null && chronicHealthCondition.isAffirmative()) ||
               (hivAids != null && hivAids.isAffirmative()) ||
               (mentalHealthDisorder != null && mentalHealthDisorder.isAffirmative()) ||
               (substanceUseDisorder != null && substanceUseDisorder.isAffirmative());
    }
    
    public boolean hasDisablingCondition() {
        // Per HMIS standards, having any disability that limits major life activity
        return hasAnyDisability();
    }
    
    // Domestic Violence methods
    public void updateDomesticViolence(DomesticViolence domesticViolence, 
                                      LocalDate informationDate) {
        this.domesticViolence = domesticViolence;
        this.domesticViolenceInformationDate = informationDate;
    }
    
    public boolean hasDomesticViolenceHistory() {
        return domesticViolence != null && domesticViolence.hasHistory();
    }
    
    public boolean requiresConfidentialHandling() {
        return domesticViolence != null && domesticViolence.requiresConfidentialHandling();
    }
    
    // Current Living Situation methods
    public void updateCurrentLivingSituation(CurrentLivingSituation situation) {
        this.currentLivingSituation = situation;
    }
    
    public boolean isCurrentlyLiterallyHomeless() {
        return currentLivingSituation != null && currentLivingSituation.isLiterallyHomeless();
    }
    
    public boolean isCurrentlyTemporarilyHomeless() {
        return currentLivingSituation != null && currentLivingSituation.isTemporarilyHomeless();
    }
    
    public boolean isCurrentlyInPermanentHousing() {
        return currentLivingSituation != null && currentLivingSituation.isPermanentHousing();
    }
    
    // Program-specific date methods
    public void updateDateOfEngagement(LocalDate dateOfEngagement) {
        this.dateOfEngagement = dateOfEngagement;
    }
    
    public void updateBedNightDate(LocalDate bedNightDate) {
        this.bedNightDate = bedNightDate;
    }
    
    // Data quality methods
    public boolean hasCompleteIncomeInformation() {
        return totalMonthlyIncome != null && 
               !incomeSources.isEmpty() && 
               incomeInformationDate != null;
    }
    
    public boolean hasCompleteDisabilityInformation() {
        return physicalDisability != null && physicalDisability.isKnownResponse() &&
               developmentalDisability != null && developmentalDisability.isKnownResponse() &&
               chronicHealthCondition != null && chronicHealthCondition.isKnownResponse() &&
               hivAids != null && hivAids.isKnownResponse() &&
               mentalHealthDisorder != null && mentalHealthDisorder.isKnownResponse() &&
               substanceUseDisorder != null && substanceUseDisorder.isKnownResponse() &&
               disabilityInformationDate != null;
    }
    
    public boolean meetsHmisDataQuality() {
        return hasCompleteIncomeInformation() &&
               hasCompleteDisabilityInformation() &&
               domesticViolence != null && domesticViolence.isKnownResponse() &&
               currentLivingSituation != null && currentLivingSituation.isKnownSituation();
    }
    
    // Getters
    public Integer getTotalMonthlyIncome() { return totalMonthlyIncome; }
    public List<IncomeSource> getIncomeSources() { return List.copyOf(incomeSources); }
    public List<NonCashBenefit> getNonCashBenefits() { return List.copyOf(nonCashBenefits); }
    public List<HealthInsurance> getHealthInsurances() { return List.copyOf(healthInsurances); }
    public DisabilityType getPhysicalDisability() { return physicalDisability; }
    public DisabilityType getDevelopmentalDisability() { return developmentalDisability; }
    public DisabilityType getChronicHealthCondition() { return chronicHealthCondition; }
    public DisabilityType getHivAids() { return hivAids; }
    public DisabilityType getMentalHealthDisorder() { return mentalHealthDisorder; }
    public DisabilityType getSubstanceUseDisorder() { return substanceUseDisorder; }
    public DomesticViolence getDomesticViolence() { return domesticViolence; }
    public CurrentLivingSituation getCurrentLivingSituation() { return currentLivingSituation; }
    public LocalDate getDateOfEngagement() { return dateOfEngagement; }
    public LocalDate getBedNightDate() { return bedNightDate; }
    public LocalDate getIncomeInformationDate() { return incomeInformationDate; }
    public LocalDate getNonCashBenefitInformationDate() { return nonCashBenefitInformationDate; }
    public LocalDate getHealthInsuranceInformationDate() { return healthInsuranceInformationDate; }
    public LocalDate getDisabilityInformationDate() { return disabilityInformationDate; }
    public LocalDate getDomesticViolenceInformationDate() { return domesticViolenceInformationDate; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProgramSpecificDataElements that = (ProgramSpecificDataElements) obj;
        return Objects.equals(totalMonthlyIncome, that.totalMonthlyIncome) &&
               Objects.equals(incomeSources, that.incomeSources) &&
               Objects.equals(nonCashBenefits, that.nonCashBenefits) &&
               Objects.equals(healthInsurances, that.healthInsurances) &&
               physicalDisability == that.physicalDisability &&
               developmentalDisability == that.developmentalDisability &&
               chronicHealthCondition == that.chronicHealthCondition &&
               hivAids == that.hivAids &&
               mentalHealthDisorder == that.mentalHealthDisorder &&
               substanceUseDisorder == that.substanceUseDisorder &&
               domesticViolence == that.domesticViolence &&
               currentLivingSituation == that.currentLivingSituation &&
               Objects.equals(dateOfEngagement, that.dateOfEngagement) &&
               Objects.equals(bedNightDate, that.bedNightDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(totalMonthlyIncome, incomeSources, nonCashBenefits, 
                          healthInsurances, physicalDisability, developmentalDisability,
                          chronicHealthCondition, hivAids, mentalHealthDisorder,
                          substanceUseDisorder, domesticViolence, currentLivingSituation,
                          dateOfEngagement, bedNightDate);
    }
}