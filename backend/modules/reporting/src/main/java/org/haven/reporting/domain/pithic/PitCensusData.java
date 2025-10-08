package org.haven.reporting.domain.pithic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Point-in-Time (PIT) census data aggregation for HUD reporting.
 * Aggregates client counts by demographic and location categories without PII.
 */
public class PitCensusData {

    private final UUID censusId;
    private final LocalDate censusDate;
    private final String continuumCode;
    private final String organizationId;

    // Aggregated counts by household type
    private int totalHouseholds;
    private int householdsWithChildren;
    private int householdsWithoutChildren;
    private int householdsWithOnlyChildren;

    // Aggregated counts by age group
    private int totalPersons;
    private int personsUnder18;
    private int persons18To24;
    private int personsOver24;

    // Aggregated counts by gender
    private int personsMale;
    private int personsFemale;
    private int personsNonBinary;
    private int personsTransgender;
    private int personsQuestioning;

    // Aggregated counts by race/ethnicity
    private int personsWhite;
    private int personsBlackAfricanAmerican;
    private int personsAsian;
    private int personsAmericanIndianAlaskaNative;
    private int personsNativeHawaiianPacificIslander;
    private int personsMultipleRaces;
    private int personsHispanicLatino;

    // Aggregated counts by special populations
    private int veteranPersons;
    private int chronicallyHomelessPersons;
    private int personsWithDisabilities;
    private int personsFleeingDV;
    private int unaccompaniedYouth;
    private int parentingYouth;

    // Location-based counts
    private int shelteredEmergencyShelter;
    private int shelteredTransitionalHousing;
    private int shelteredSafeHaven;
    private int unshelteredPersons;

    // Data quality metrics
    private int recordsWithMissingData;
    private int recordsWithDataQualityIssues;
    private double dataCompletionRate;

    private final LocalDateTime generatedAt;
    private final String generatedBy;
    private final Map<String, Object> metadata;

    public PitCensusData(UUID censusId, LocalDate censusDate, String continuumCode,
                         String organizationId, String generatedBy) {
        this.censusId = censusId;
        this.censusDate = censusDate;
        this.continuumCode = continuumCode;
        this.organizationId = organizationId;
        this.generatedBy = generatedBy;
        this.generatedAt = LocalDateTime.now();
        this.metadata = new java.util.HashMap<>();
    }

    // Aggregate household data
    public void aggregateHouseholdData(HouseholdAggregation aggregation) {
        this.totalHouseholds = aggregation.totalHouseholds();
        this.householdsWithChildren = aggregation.householdsWithChildren();
        this.householdsWithoutChildren = aggregation.householdsWithoutChildren();
        this.householdsWithOnlyChildren = aggregation.householdsWithOnlyChildren();
    }

    // Aggregate demographic data
    public void aggregateDemographicData(DemographicAggregation aggregation) {
        // Age groups
        this.totalPersons = aggregation.totalPersons();
        this.personsUnder18 = aggregation.personsUnder18();
        this.persons18To24 = aggregation.persons18To24();
        this.personsOver24 = aggregation.personsOver24();

        // Gender
        this.personsMale = aggregation.personsMale();
        this.personsFemale = aggregation.personsFemale();
        this.personsNonBinary = aggregation.personsNonBinary();
        this.personsTransgender = aggregation.personsTransgender();
        this.personsQuestioning = aggregation.personsQuestioning();

        // Race/Ethnicity
        this.personsWhite = aggregation.personsWhite();
        this.personsBlackAfricanAmerican = aggregation.personsBlackAfricanAmerican();
        this.personsAsian = aggregation.personsAsian();
        this.personsAmericanIndianAlaskaNative = aggregation.personsAmericanIndianAlaskaNative();
        this.personsNativeHawaiianPacificIslander = aggregation.personsNativeHawaiianPacificIslander();
        this.personsMultipleRaces = aggregation.personsMultipleRaces();
        this.personsHispanicLatino = aggregation.personsHispanicLatino();
    }

    // Aggregate special population data
    public void aggregateSpecialPopulationData(SpecialPopulationAggregation aggregation) {
        this.veteranPersons = aggregation.veteranPersons();
        this.chronicallyHomelessPersons = aggregation.chronicallyHomelessPersons();
        this.personsWithDisabilities = aggregation.personsWithDisabilities();
        this.personsFleeingDV = aggregation.personsFleeingDV();
        this.unaccompaniedYouth = aggregation.unaccompaniedYouth();
        this.parentingYouth = aggregation.parentingYouth();
    }

    // Aggregate location data
    public void aggregateLocationData(LocationAggregation aggregation) {
        this.shelteredEmergencyShelter = aggregation.shelteredEmergencyShelter();
        this.shelteredTransitionalHousing = aggregation.shelteredTransitionalHousing();
        this.shelteredSafeHaven = aggregation.shelteredSafeHaven();
        this.unshelteredPersons = aggregation.unshelteredPersons();
    }

    // Update data quality metrics
    public void updateDataQualityMetrics(int missingData, int qualityIssues, double completionRate) {
        this.recordsWithMissingData = missingData;
        this.recordsWithDataQualityIssues = qualityIssues;
        this.dataCompletionRate = completionRate;
    }

    // Validate against HUD requirements
    public ValidationResult validateAgainstHudRequirements() {
        var errors = new java.util.ArrayList<String>();
        var warnings = new java.util.ArrayList<String>();

        // Check required fields
        if (totalPersons == 0) {
            errors.add("Total persons count cannot be zero");
        }

        // Check consistency
        int calculatedTotal = personsUnder18 + persons18To24 + personsOver24;
        if (calculatedTotal != totalPersons) {
            warnings.add(String.format("Age group totals (%d) don't match total persons (%d)",
                calculatedTotal, totalPersons));
        }

        int locationTotal = shelteredEmergencyShelter + shelteredTransitionalHousing +
                           shelteredSafeHaven + unshelteredPersons;
        if (locationTotal != totalPersons) {
            warnings.add(String.format("Location totals (%d) don't match total persons (%d)",
                locationTotal, totalPersons));
        }

        // Check data quality thresholds
        if (dataCompletionRate < 0.95) {
            warnings.add(String.format("Data completion rate (%.2f%%) below HUD threshold (95%%)",
                dataCompletionRate * 100));
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    // Value objects

    public record HouseholdAggregation(
        int totalHouseholds,
        int householdsWithChildren,
        int householdsWithoutChildren,
        int householdsWithOnlyChildren
    ) {}

    public record DemographicAggregation(
        int totalPersons,
        int personsUnder18,
        int persons18To24,
        int personsOver24,
        int personsMale,
        int personsFemale,
        int personsNonBinary,
        int personsTransgender,
        int personsQuestioning,
        int personsWhite,
        int personsBlackAfricanAmerican,
        int personsAsian,
        int personsAmericanIndianAlaskaNative,
        int personsNativeHawaiianPacificIslander,
        int personsMultipleRaces,
        int personsHispanicLatino
    ) {}

    public record SpecialPopulationAggregation(
        int veteranPersons,
        int chronicallyHomelessPersons,
        int personsWithDisabilities,
        int personsFleeingDV,
        int unaccompaniedYouth,
        int parentingYouth
    ) {}

    public record LocationAggregation(
        int shelteredEmergencyShelter,
        int shelteredTransitionalHousing,
        int shelteredSafeHaven,
        int unshelteredPersons
    ) {}

    public record ValidationResult(
        boolean isValid,
        java.util.List<String> errors,
        java.util.List<String> warnings
    ) {}

    // Getters

    public UUID getCensusId() { return censusId; }
    public LocalDate getCensusDate() { return censusDate; }
    public String getContinuumCode() { return continuumCode; }
    public String getOrganizationId() { return organizationId; }
    public int getTotalHouseholds() { return totalHouseholds; }
    public int getHouseholdsWithChildren() { return householdsWithChildren; }
    public int getHouseholdsWithoutChildren() { return householdsWithoutChildren; }
    public int getHouseholdsWithOnlyChildren() { return householdsWithOnlyChildren; }
    public int getTotalPersons() { return totalPersons; }
    public int getPersonsUnder18() { return personsUnder18; }
    public int getPersons18To24() { return persons18To24; }
    public int getPersonsOver24() { return personsOver24; }
    public int getPersonsMale() { return personsMale; }
    public int getPersonsFemale() { return personsFemale; }
    public int getPersonsNonBinary() { return personsNonBinary; }
    public int getPersonsTransgender() { return personsTransgender; }
    public int getPersonsQuestioning() { return personsQuestioning; }
    public int getPersonsWhite() { return personsWhite; }
    public int getPersonsBlackAfricanAmerican() { return personsBlackAfricanAmerican; }
    public int getPersonsAsian() { return personsAsian; }
    public int getPersonsAmericanIndianAlaskaNative() { return personsAmericanIndianAlaskaNative; }
    public int getPersonsNativeHawaiianPacificIslander() { return personsNativeHawaiianPacificIslander; }
    public int getPersonsMultipleRaces() { return personsMultipleRaces; }
    public int getPersonsHispanicLatino() { return personsHispanicLatino; }
    public int getVeteranPersons() { return veteranPersons; }
    public int getChronicallyHomelessPersons() { return chronicallyHomelessPersons; }
    public int getPersonsWithDisabilities() { return personsWithDisabilities; }
    public int getPersonsFleeingDV() { return personsFleeingDV; }
    public int getUnaccompaniedYouth() { return unaccompaniedYouth; }
    public int getParentingYouth() { return parentingYouth; }
    public int getShelteredEmergencyShelter() { return shelteredEmergencyShelter; }
    public int getShelteredTransitionalHousing() { return shelteredTransitionalHousing; }
    public int getShelteredSafeHaven() { return shelteredSafeHaven; }
    public int getUnshelteredPersons() { return unshelteredPersons; }
    public int getRecordsWithMissingData() { return recordsWithMissingData; }
    public int getRecordsWithDataQualityIssues() { return recordsWithDataQualityIssues; }
    public double getDataCompletionRate() { return dataCompletionRate; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
    public Map<String, Object> getMetadata() { return metadata; }
}