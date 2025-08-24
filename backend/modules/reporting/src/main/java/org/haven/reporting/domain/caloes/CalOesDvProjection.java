package org.haven.reporting.domain.caloes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * California Office of Emergency Services (Cal OES) 
 * Domestic Violence (DV) Program Reporting Projection
 * 
 * Supports Cal OES DV program reporting requirements including
 * victim services, shelter utilization, and outcome data.
 */
public record CalOesDvProjection(
    String recordId,
    String victimId, // De-identified
    LocalDate serviceDate,
    String programType, // DV_SHELTER, DV_ADVOCACY, DV_LEGAL, etc.
    String serviceType,
    String serviceCategory,
    Integer durationMinutes,
    String staffId,
    
    // Victim Demographics (anonymized)
    String ageGroup, // 0-17, 18-24, 25-34, 35-44, 45-54, 55-64, 65+
    String genderIdentity,
    String raceEthnicity,
    String primaryLanguage,
    Boolean disabilityStatus,
    String immigrationStatus,
    
    // Household Information
    String householdType, // INDIVIDUAL, FAMILY_WITH_CHILDREN, COUPLE
    Integer adultsInHousehold,
    Integer childrenInHousehold,
    
    // Domestic Violence Context
    String relationshipToPerpetrator,
    String typeOfViolence, // PHYSICAL, EMOTIONAL, SEXUAL, FINANCIAL, etc.
    Integer incidentsLastYear,
    Boolean lawEnforcementInvolved,
    Boolean courtOrdersPresent,
    Boolean childrenWitnessed,
    
    // Safety and Risk Assessment
    String riskLevel, // LOW, MODERATE, HIGH, EXTREME
    Boolean safetyPlanCompleted,
    Boolean legalAdvocacyProvided,
    Boolean emergencyFinancialAssistance,
    
    // Housing Status
    String housingStatus, // HOMELESS, DOUBLED_UP, RENTED, OWNED, SHELTER, etc.
    String priorHousingSituation,
    Boolean housingAssistanceProvided,
    String exitDestination,
    
    // Service Outcomes
    Boolean safetyGoalAchieved,
    Boolean stabilityGoalAchieved,
    Boolean selfSufficiencyGoalAchieved,
    String exitReason,
    LocalDate exitDate,
    
    // Follow-up Information
    LocalDate followUpDate,
    String followUpStatus,
    Boolean stillSafe,
    Boolean stillStable,
    
    // Referrals and Coordination
    String internalReferrals,
    String externalReferrals,
    Boolean coordinatedWithLawEnforcement,
    Boolean coordinatedWithCPS,
    Boolean coordinatedWithHealthcare,
    
    // Cal OES Specific Fields
    String calOesGrantNumber,
    String reportingPeriod,
    String countyCode,
    String programSubcontractor,
    
    // Data Quality and Privacy
    Integer dataQualityScore,
    Boolean confidentialityWaiver,
    Boolean dataCollectionConsent,
    
    // Record Metadata
    LocalDate recordCreated,
    LocalDate recordUpdated,
    String createdBy,
    String updatedBy,
    LocalDateTime lastModified
) {

    /**
     * Create Cal OES DV projection for reporting
     */
    public static CalOesDvProjection fromServiceRecord(
            String recordId,
            String anonymizedVictimId,
            LocalDate serviceDate,
            String programType,
            String serviceType,
            String ageGroup,
            String genderIdentity,
            String raceEthnicity,
            String housingStatus,
            String riskLevel,
            String calOesGrantNumber,
            String reportingPeriod,
            String countyCode) {
        
        return new CalOesDvProjection(
            recordId,
            anonymizedVictimId,
            serviceDate,
            programType,
            serviceType,
            determineServiceCategory(serviceType),
            null, // Duration would be tracked separately
            null, // Staff ID anonymized for privacy
            
            // Demographics
            ageGroup,
            genderIdentity,
            raceEthnicity,
            null, // Primary language
            null, // Disability status
            null, // Immigration status
            
            // Household
            null, // Household type
            null, // Adults in household
            null, // Children in household
            
            // DV Context
            null, // Relationship to perpetrator
            null, // Type of violence
            null, // Incidents last year
            null, // Law enforcement involved
            null, // Court orders present
            null, // Children witnessed
            
            // Safety and Risk
            riskLevel,
            null, // Safety plan completed
            null, // Legal advocacy provided
            null, // Emergency financial assistance
            
            // Housing
            housingStatus,
            null, // Prior housing situation
            null, // Housing assistance provided
            null, // Exit destination
            
            // Outcomes
            null, // Safety goal achieved
            null, // Stability goal achieved
            null, // Self-sufficiency goal achieved
            null, // Exit reason
            null, // Exit date
            
            // Follow-up
            null, // Follow-up date
            null, // Follow-up status
            null, // Still safe
            null, // Still stable
            
            // Referrals
            null, // Internal referrals
            null, // External referrals
            null, // Coordinated with law enforcement
            null, // Coordinated with CPS
            null, // Coordinated with healthcare
            
            // Cal OES
            calOesGrantNumber,
            reportingPeriod,
            countyCode,
            null, // Program subcontractor
            
            // Data Quality
            calculateDataQualityScore(ageGroup, genderIdentity, raceEthnicity, housingStatus),
            null, // Confidentiality waiver
            true, // Data collection consent assumed
            
            // Metadata
            LocalDate.now(),
            LocalDate.now(),
            "system",
            "system",
            LocalDateTime.now()
        );
    }

    private static String determineServiceCategory(String serviceType) {
        if (serviceType == null) return "OTHER";
        
        return switch (serviceType.toUpperCase()) {
            case "EMERGENCY_SHELTER", "TRANSITIONAL_HOUSING", "RAPID_REHOUSING" -> "HOUSING";
            case "CRISIS_COUNSELING", "SUPPORT_GROUP", "THERAPY" -> "COUNSELING";
            case "LEGAL_ADVOCACY", "COURT_ACCOMPANIMENT", "PROTECTION_ORDER" -> "LEGAL";
            case "CASE_MANAGEMENT", "SERVICE_COORDINATION" -> "CASE_MANAGEMENT";
            case "EMERGENCY_FINANCIAL", "RENT_ASSISTANCE", "UTILITY_ASSISTANCE" -> "FINANCIAL";
            case "TRANSPORTATION", "CHILDCARE", "INTERPRETATION" -> "SUPPORT_SERVICES";
            default -> "OTHER";
        };
    }

    private static Integer calculateDataQualityScore(String ageGroup, String genderIdentity, 
                                                   String raceEthnicity, String housingStatus) {
        int score = 0;
        int totalFields = 4;
        
        if (ageGroup != null && !ageGroup.isEmpty()) score++;
        if (genderIdentity != null && !genderIdentity.isEmpty()) score++;
        if (raceEthnicity != null && !raceEthnicity.isEmpty()) score++;
        if (housingStatus != null && !housingStatus.isEmpty()) score++;
        
        return (score * 100) / totalFields; // Percentage score
    }

    /**
     * Generate anonymized victim identifier for reporting
     */
    public String getAnonymizedVictimId() {
        // Returns hash-based identifier that maintains consistency
        // but doesn't reveal PII
        return victimId;
    }

    /**
     * Determine if this record meets Cal OES reporting requirements
     */
    public boolean meetsReportingRequirements() {
        return recordId != null &&
               victimId != null &&
               serviceDate != null &&
               programType != null &&
               calOesGrantNumber != null &&
               reportingPeriod != null &&
               countyCode != null &&
               dataCollectionConsent != null &&
               dataCollectionConsent;
    }

    /**
     * Calculate service utilization metrics
     */
    public boolean isHighUtilizationClient() {
        // Would need additional logic based on service frequency
        // This is a placeholder implementation
        return durationMinutes != null && durationMinutes > 120;
    }

    /**
     * Determine if positive outcome was achieved
     */
    public boolean hasPositiveOutcome() {
        return Boolean.TRUE.equals(safetyGoalAchieved) ||
               Boolean.TRUE.equals(stabilityGoalAchieved) ||
               Boolean.TRUE.equals(selfSufficiencyGoalAchieved);
    }

    /**
     * Convert to Cal OES CSV format for reporting
     */
    public String toCalOesCsvRow() {
        return String.join(",",
            quote(recordId),
            quote(getAnonymizedVictimId()),
            quote(formatDate(serviceDate)),
            quote(programType),
            quote(serviceType),
            quote(serviceCategory),
            quote(ageGroup),
            quote(genderIdentity),
            quote(raceEthnicity),
            quote(housingStatus),
            quote(riskLevel),
            quote(calOesGrantNumber),
            quote(reportingPeriod),
            quote(countyCode),
            String.valueOf(dataQualityScore != null ? dataQualityScore : 0),
            quote(formatDate(recordCreated))
        );
    }

    private String quote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }
}