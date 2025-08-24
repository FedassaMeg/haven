package org.haven.reporting.domain.sage;

import java.time.LocalDate;
import java.util.Map;

/**
 * SAGE (HMIS Aggregate Data) for federally required reporting
 * Provides summarized data without personally identifiable information
 * Used for Annual Homeless Assessment Report (AHAR) and other HUD reports
 */
public record SageAggregateData(
    String reportId,
    LocalDate reportingPeriodStart,
    LocalDate reportingPeriodEnd,
    String cocCode,
    String projectId,
    String projectType,
    
    // Client Demographics Aggregates
    Integer totalUniqueClients,
    Integer totalAdults,
    Integer totalChildren,
    Integer totalUnaccompaniedYouth,
    Integer totalFamilies,
    Integer totalIndividuals,
    
    // Gender Aggregates
    Map<String, Integer> genderBreakdown,
    
    // Race/Ethnicity Aggregates  
    Map<String, Integer> raceBreakdown,
    Map<String, Integer> ethnicityBreakdown,
    
    // Veteran Status
    Integer totalVeterans,
    Integer veteransWithDisablingCondition,
    
    // Disability Status
    Integer clientsWithDisablingCondition,
    
    // Chronic Homelessness
    Integer chronicallyHomelessIndividuals,
    Integer chronicallyHomelessFamilies,
    Integer chronicallyHomelessPersons,
    
    // Housing Outcomes
    Integer exitsToPermanentHousing,
    Integer exitsToTemporaryHousing,
    Integer returnsToHomelessness,
    
    // Length of Stay Aggregates
    Map<String, Integer> lengthOfStayBreakdown,
    
    // Prior Living Situation
    Map<String, Integer> priorLivingSituationBreakdown,
    
    // Exit Destinations
    Map<String, Integer> exitDestinationBreakdown,
    
    // Service Utilization
    Integer totalServiceEpisodes,
    Map<String, Integer> serviceTypeBreakdown,
    
    // Income and Benefits (if collected)
    Integer clientsWithIncome,
    Integer clientsWithBenefits,
    
    // Data Quality Metrics
    Double nameDataQualityScore,
    Double ssnDataQualityScore,
    Double dobDataQualityScore,
    Double destinationDataQualityScore,
    
    // Report Metadata
    LocalDate reportGeneratedDate,
    String reportGeneratedBy,
    String softwareVersion
) {

    /**
     * Calculate overall data quality score
     */
    public Double calculateOverallDataQuality() {
        if (nameDataQualityScore == null || ssnDataQualityScore == null || 
            dobDataQualityScore == null || destinationDataQualityScore == null) {
            return null;
        }
        
        return (nameDataQualityScore + ssnDataQualityScore + 
                dobDataQualityScore + destinationDataQualityScore) / 4.0;
    }

    /**
     * Calculate percentage of exits to permanent housing
     */
    public Double getPermanentHousingExitRate() {
        if (exitDestinationBreakdown == null || exitDestinationBreakdown.isEmpty()) {
            return null;
        }
        
        int totalExits = exitDestinationBreakdown.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
        
        if (totalExits == 0) {
            return null;
        }
        
        return (exitsToPermanentHousing.doubleValue() / totalExits) * 100.0;
    }

    /**
     * Calculate percentage of chronically homeless clients
     */
    public Double getChronicallyHomelessRate() {
        if (totalUniqueClients == null || totalUniqueClients == 0) {
            return null;
        }
        
        return (chronicallyHomelessPersons.doubleValue() / totalUniqueClients) * 100.0;
    }

    /**
     * Calculate percentage of veterans served
     */
    public Double getVeteranRate() {
        if (totalAdults == null || totalAdults == 0) {
            return null;
        }
        
        return (totalVeterans.doubleValue() / totalAdults) * 100.0;
    }

    /**
     * Calculate average length of stay (in days)
     * Based on length of stay breakdown
     */
    public Double getAverageLengthOfStay() {
        if (lengthOfStayBreakdown == null || lengthOfStayBreakdown.isEmpty()) {
            return null;
        }
        
        int totalPersons = 0;
        int totalDays = 0;
        
        for (Map.Entry<String, Integer> entry : lengthOfStayBreakdown.entrySet()) {
            String category = entry.getKey();
            Integer count = entry.getValue();
            
            // Estimate days based on category
            int estimatedDays = estimateDaysFromCategory(category);
            totalPersons += count;
            totalDays += (count * estimatedDays);
        }
        
        return totalPersons > 0 ? (double) totalDays / totalPersons : null;
    }

    private int estimateDaysFromCategory(String category) {
        return switch (category.toLowerCase()) {
            case "one_night_or_less" -> 1;
            case "two_to_six_nights" -> 4;
            case "one_week_to_less_than_one_month" -> 18;
            case "one_month_to_less_than_three_months" -> 60;
            case "three_months_to_less_than_one_year" -> 180;
            case "one_year_or_longer" -> 450;
            default -> 0;
        };
    }

    /**
     * Get family composition breakdown
     */
    public String getFamilyComposition() {
        if (totalFamilies == null || totalIndividuals == null) {
            return "Data not available";
        }
        
        int total = totalFamilies + totalIndividuals;
        if (total == 0) {
            return "No clients served";
        }
        
        double familyPct = (totalFamilies.doubleValue() / total) * 100.0;
        double individualPct = (totalIndividuals.doubleValue() / total) * 100.0;
        
        return String.format("Families: %.1f%%, Individuals: %.1f%%", familyPct, individualPct);
    }

    /**
     * Validate required SAGE data elements
     */
    public boolean isValidSageReport() {
        return reportId != null &&
               reportingPeriodStart != null &&
               reportingPeriodEnd != null &&
               cocCode != null &&
               totalUniqueClients != null &&
               totalUniqueClients >= 0;
    }
}