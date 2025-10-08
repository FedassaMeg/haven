package org.haven.reporting.domain.pithic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Housing Inventory Count (HIC) data aggregation for HUD reporting.
 * Aggregates housing inventory and bed capacity data without PII.
 */
public class HicInventoryData {

    private final UUID inventoryId;
    private final LocalDate inventoryDate;
    private final String continuumCode;
    private final String organizationId;

    // Emergency Shelter inventory
    private int emergencyShelterTotalBeds;
    private int emergencyShelterTotalUnits;
    private int emergencyShelterVeteranBeds;
    private int emergencyShelterYouthBeds;
    private int emergencyShelterFamilyBeds;
    private int emergencyShelterAdultOnlyBeds;
    private int emergencyShelterChronicBeds;
    private int emergencyShelterSeasonalBeds;
    private int emergencyShelterOverflowBeds;
    private int emergencyShelterVoucherBeds;

    // Transitional Housing inventory
    private int transitionalHousingTotalBeds;
    private int transitionalHousingTotalUnits;
    private int transitionalHousingVeteranBeds;
    private int transitionalHousingYouthBeds;
    private int transitionalHousingFamilyBeds;
    private int transitionalHousingAdultOnlyBeds;
    private int transitionalHousingChronicBeds;

    // Safe Haven inventory
    private int safeHavenTotalBeds;
    private int safeHavenTotalUnits;

    // Rapid Re-Housing inventory
    private int rapidRehousingTotalBeds;
    private int rapidRehousingTotalUnits;
    private int rapidRehousingVeteranBeds;
    private int rapidRehousingYouthBeds;
    private int rapidRehousingFamilyBeds;
    private int rapidRehousingAdultOnlyBeds;

    // Permanent Supportive Housing inventory
    private int permanentSupportiveTotalBeds;
    private int permanentSupportiveTotalUnits;
    private int permanentSupportiveVeteranBeds;
    private int permanentSupportiveYouthBeds;
    private int permanentSupportiveFamilyBeds;
    private int permanentSupportiveAdultOnlyBeds;
    private int permanentSupportiveChronicBeds;

    // Other Permanent Housing inventory
    private int otherPermanentHousingTotalBeds;
    private int otherPermanentHousingTotalUnits;

    // Utilization metrics
    private double emergencyShelterUtilizationRate;
    private double transitionalHousingUtilizationRate;
    private double safeHavenUtilizationRate;
    private double rapidRehousingUtilizationRate;
    private double permanentSupportiveUtilizationRate;

    // Point-in-Time occupancy (from census date)
    private int pitOccupiedEmergencyShelter;
    private int pitOccupiedTransitionalHousing;
    private int pitOccupiedSafeHaven;
    private int pitOccupiedRapidRehousing;
    private int pitOccupiedPermanentSupportive;

    // Data quality metrics
    private int projectsWithMissingInventory;
    private int projectsWithInconsistentData;
    private double inventoryDataCompletionRate;

    // Funding source breakdown
    private Map<String, Integer> bedsByFundingSource;
    private Map<String, Integer> unitsByFundingSource;

    private final LocalDateTime generatedAt;
    private final String generatedBy;
    private final Map<String, Object> metadata;

    public HicInventoryData(UUID inventoryId, LocalDate inventoryDate, String continuumCode,
                           String organizationId, String generatedBy) {
        this.inventoryId = inventoryId;
        this.inventoryDate = inventoryDate;
        this.continuumCode = continuumCode;
        this.organizationId = organizationId;
        this.generatedBy = generatedBy;
        this.generatedAt = LocalDateTime.now();
        this.metadata = new java.util.HashMap<>();
        this.bedsByFundingSource = new java.util.HashMap<>();
        this.unitsByFundingSource = new java.util.HashMap<>();
    }

    // Aggregate emergency shelter data
    public void aggregateEmergencyShelterData(EmergencyShelterAggregation aggregation) {
        this.emergencyShelterTotalBeds = aggregation.totalBeds();
        this.emergencyShelterTotalUnits = aggregation.totalUnits();
        this.emergencyShelterVeteranBeds = aggregation.veteranBeds();
        this.emergencyShelterYouthBeds = aggregation.youthBeds();
        this.emergencyShelterFamilyBeds = aggregation.familyBeds();
        this.emergencyShelterAdultOnlyBeds = aggregation.adultOnlyBeds();
        this.emergencyShelterChronicBeds = aggregation.chronicBeds();
        this.emergencyShelterSeasonalBeds = aggregation.seasonalBeds();
        this.emergencyShelterOverflowBeds = aggregation.overflowBeds();
        this.emergencyShelterVoucherBeds = aggregation.voucherBeds();
    }

    // Aggregate transitional housing data
    public void aggregateTransitionalHousingData(TransitionalHousingAggregation aggregation) {
        this.transitionalHousingTotalBeds = aggregation.totalBeds();
        this.transitionalHousingTotalUnits = aggregation.totalUnits();
        this.transitionalHousingVeteranBeds = aggregation.veteranBeds();
        this.transitionalHousingYouthBeds = aggregation.youthBeds();
        this.transitionalHousingFamilyBeds = aggregation.familyBeds();
        this.transitionalHousingAdultOnlyBeds = aggregation.adultOnlyBeds();
        this.transitionalHousingChronicBeds = aggregation.chronicBeds();
    }

    // Aggregate rapid rehousing data
    public void aggregateRapidRehousingData(RapidRehousingAggregation aggregation) {
        this.rapidRehousingTotalBeds = aggregation.totalBeds();
        this.rapidRehousingTotalUnits = aggregation.totalUnits();
        this.rapidRehousingVeteranBeds = aggregation.veteranBeds();
        this.rapidRehousingYouthBeds = aggregation.youthBeds();
        this.rapidRehousingFamilyBeds = aggregation.familyBeds();
        this.rapidRehousingAdultOnlyBeds = aggregation.adultOnlyBeds();
    }

    // Aggregate permanent supportive housing data
    public void aggregatePermanentSupportiveData(PermanentSupportiveAggregation aggregation) {
        this.permanentSupportiveTotalBeds = aggregation.totalBeds();
        this.permanentSupportiveTotalUnits = aggregation.totalUnits();
        this.permanentSupportiveVeteranBeds = aggregation.veteranBeds();
        this.permanentSupportiveYouthBeds = aggregation.youthBeds();
        this.permanentSupportiveFamilyBeds = aggregation.familyBeds();
        this.permanentSupportiveAdultOnlyBeds = aggregation.adultOnlyBeds();
        this.permanentSupportiveChronicBeds = aggregation.chronicBeds();
    }

    // Update utilization metrics
    public void updateUtilizationMetrics(UtilizationMetrics metrics) {
        this.emergencyShelterUtilizationRate = metrics.emergencyShelterRate();
        this.transitionalHousingUtilizationRate = metrics.transitionalHousingRate();
        this.safeHavenUtilizationRate = metrics.safeHavenRate();
        this.rapidRehousingUtilizationRate = metrics.rapidRehousingRate();
        this.permanentSupportiveUtilizationRate = metrics.permanentSupportiveRate();
    }

    // Update PIT occupancy
    public void updatePitOccupancy(PitOccupancy occupancy) {
        this.pitOccupiedEmergencyShelter = occupancy.emergencyShelter();
        this.pitOccupiedTransitionalHousing = occupancy.transitionalHousing();
        this.pitOccupiedSafeHaven = occupancy.safeHaven();
        this.pitOccupiedRapidRehousing = occupancy.rapidRehousing();
        this.pitOccupiedPermanentSupportive = occupancy.permanentSupportive();
    }

    // Update data quality metrics
    public void updateDataQualityMetrics(int missingInventory, int inconsistentData, double completionRate) {
        this.projectsWithMissingInventory = missingInventory;
        this.projectsWithInconsistentData = inconsistentData;
        this.inventoryDataCompletionRate = completionRate;
    }

    // Add funding source data
    public void addFundingSourceData(String fundingSource, int beds, int units) {
        this.bedsByFundingSource.put(fundingSource, beds);
        this.unitsByFundingSource.put(fundingSource, units);
    }

    // Calculate total inventory
    public InventoryTotals calculateTotals() {
        int totalBeds = emergencyShelterTotalBeds + transitionalHousingTotalBeds +
                       safeHavenTotalBeds + rapidRehousingTotalBeds +
                       permanentSupportiveTotalBeds + otherPermanentHousingTotalBeds;

        int totalUnits = emergencyShelterTotalUnits + transitionalHousingTotalUnits +
                        safeHavenTotalUnits + rapidRehousingTotalUnits +
                        permanentSupportiveTotalUnits + otherPermanentHousingTotalUnits;

        int totalOccupied = pitOccupiedEmergencyShelter + pitOccupiedTransitionalHousing +
                           pitOccupiedSafeHaven + pitOccupiedRapidRehousing +
                           pitOccupiedPermanentSupportive;

        double overallUtilization = totalBeds > 0 ?
            (double) totalOccupied / totalBeds * 100 : 0.0;

        return new InventoryTotals(totalBeds, totalUnits, totalOccupied, overallUtilization);
    }

    // Validate against HUD requirements
    public ValidationResult validateAgainstHudRequirements() {
        var errors = new java.util.ArrayList<String>();
        var warnings = new java.util.ArrayList<String>();

        // Check for negative values
        if (emergencyShelterTotalBeds < 0 || transitionalHousingTotalBeds < 0) {
            errors.add("Bed counts cannot be negative");
        }

        // Check consistency between beds and units
        if (emergencyShelterTotalUnits > emergencyShelterTotalBeds) {
            warnings.add("Emergency shelter units exceed beds");
        }

        // Check utilization rates
        if (emergencyShelterUtilizationRate > 100.0) {
            errors.add("Emergency shelter utilization exceeds 100%");
        }

        // Check data quality thresholds
        if (inventoryDataCompletionRate < 0.95) {
            warnings.add(String.format("Inventory data completion rate (%.2f%%) below HUD threshold (95%%)",
                inventoryDataCompletionRate * 100));
        }

        // Validate PIT occupancy doesn't exceed inventory
        if (pitOccupiedEmergencyShelter > emergencyShelterTotalBeds) {
            errors.add("PIT emergency shelter occupancy exceeds available beds");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    // Value objects

    public record EmergencyShelterAggregation(
        int totalBeds,
        int totalUnits,
        int veteranBeds,
        int youthBeds,
        int familyBeds,
        int adultOnlyBeds,
        int chronicBeds,
        int seasonalBeds,
        int overflowBeds,
        int voucherBeds
    ) {}

    public record TransitionalHousingAggregation(
        int totalBeds,
        int totalUnits,
        int veteranBeds,
        int youthBeds,
        int familyBeds,
        int adultOnlyBeds,
        int chronicBeds
    ) {}

    public record RapidRehousingAggregation(
        int totalBeds,
        int totalUnits,
        int veteranBeds,
        int youthBeds,
        int familyBeds,
        int adultOnlyBeds
    ) {}

    public record PermanentSupportiveAggregation(
        int totalBeds,
        int totalUnits,
        int veteranBeds,
        int youthBeds,
        int familyBeds,
        int adultOnlyBeds,
        int chronicBeds
    ) {}

    public record UtilizationMetrics(
        double emergencyShelterRate,
        double transitionalHousingRate,
        double safeHavenRate,
        double rapidRehousingRate,
        double permanentSupportiveRate
    ) {}

    public record PitOccupancy(
        int emergencyShelter,
        int transitionalHousing,
        int safeHaven,
        int rapidRehousing,
        int permanentSupportive
    ) {}

    public record InventoryTotals(
        int totalBeds,
        int totalUnits,
        int totalOccupied,
        double overallUtilizationRate
    ) {}

    public record ValidationResult(
        boolean isValid,
        java.util.List<String> errors,
        java.util.List<String> warnings
    ) {}

    // Getters

    public UUID getInventoryId() { return inventoryId; }
    public LocalDate getInventoryDate() { return inventoryDate; }
    public String getContinuumCode() { return continuumCode; }
    public String getOrganizationId() { return organizationId; }
    public int getEmergencyShelterTotalBeds() { return emergencyShelterTotalBeds; }
    public int getEmergencyShelterTotalUnits() { return emergencyShelterTotalUnits; }
    public int getTransitionalHousingTotalBeds() { return transitionalHousingTotalBeds; }
    public int getTransitionalHousingTotalUnits() { return transitionalHousingTotalUnits; }
    public int getSafeHavenTotalBeds() { return safeHavenTotalBeds; }
    public int getSafeHavenTotalUnits() { return safeHavenTotalUnits; }
    public int getRapidRehousingTotalBeds() { return rapidRehousingTotalBeds; }
    public int getRapidRehousingTotalUnits() { return rapidRehousingTotalUnits; }
    public int getPermanentSupportiveTotalBeds() { return permanentSupportiveTotalBeds; }
    public int getPermanentSupportiveTotalUnits() { return permanentSupportiveTotalUnits; }
    public double getEmergencyShelterUtilizationRate() { return emergencyShelterUtilizationRate; }
    public double getTransitionalHousingUtilizationRate() { return transitionalHousingUtilizationRate; }
    public double getSafeHavenUtilizationRate() { return safeHavenUtilizationRate; }
    public double getRapidRehousingUtilizationRate() { return rapidRehousingUtilizationRate; }
    public double getPermanentSupportiveUtilizationRate() { return permanentSupportiveUtilizationRate; }
    public int getProjectsWithMissingInventory() { return projectsWithMissingInventory; }
    public int getProjectsWithInconsistentData() { return projectsWithInconsistentData; }
    public double getInventoryDataCompletionRate() { return inventoryDataCompletionRate; }
    public Map<String, Integer> getBedsByFundingSource() { return bedsByFundingSource; }
    public Map<String, Integer> getUnitsByFundingSource() { return unitsByFundingSource; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
    public Map<String, Object> getMetadata() { return metadata; }
}