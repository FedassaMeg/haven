package org.haven.reporting.domain.caloes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * California Office of Emergency Services (Cal OES)
 * Rape Crisis Program (RCP) Reporting Projection
 * 
 * Supports Cal OES RCP program reporting requirements including
 * crisis intervention, counseling services, and victim advocacy.
 */
public record CalOesRcpProjection(
    String recordId,
    String survivorId, // De-identified
    LocalDate serviceDate,
    String programType, // RCP_HOTLINE, RCP_COUNSELING, RCP_ADVOCACY, etc.
    String serviceType,
    String serviceMode, // IN_PERSON, PHONE, TEXT, EMAIL, CHAT
    Integer durationMinutes,
    String staffId, // Anonymized
    
    // Survivor Demographics (anonymized)
    String ageGroup, // 0-12, 13-17, 18-24, 25-34, 35-44, 45-54, 55-64, 65+
    String genderIdentity,
    String sexualOrientation,
    String raceEthnicity,
    String primaryLanguage,
    Boolean disabilityStatus,
    String immigrationStatus,
    
    // Assault Information
    String assaultType, // RAPE, SEXUAL_ASSAULT, INCEST, CHILD_SEXUAL_ABUSE, etc.
    String assaultTimeframe, // RECENT_72_HOURS, WITHIN_WEEK, WITHIN_MONTH, OVER_MONTH, HISTORICAL
    String relationshipToPerpetrator, // STRANGER, ACQUAINTANCE, INTIMATE_PARTNER, FAMILY, etc.
    String assaultLocation, // HOME, WORK, SCHOOL, PUBLIC, ONLINE, etc.
    Boolean multiplePerps,
    Boolean reportedToPolice,
    Boolean medicalAttentionSought,
    
    // Service Information
    String firstContactMethod, // HOTLINE, WALK_IN, REFERRAL, OUTREACH, etc.
    String referralSource,
    Boolean crisisIntervention,
    Boolean informationAndReferral,
    Boolean individualCounseling,
    Boolean groupCounseling,
    Boolean medicalAdvocacy,
    Boolean legalAdvocacy,
    Boolean systemsAdvocacy,
    Boolean accompanyToMedical,
    Boolean accompanyToLegal,
    Boolean accompanyToSocialServices,
    
    // Specialized Services
    Boolean traumaInformedCare,
    Boolean culturallySpecificServices,
    Boolean lgbtqAffirmativeServices,
    Boolean disabilityAccessibleServices,
    Boolean languageAccessServices,
    String interpreterLanguage,
    
    // Prevention and Education
    Boolean preventionEducationProvided,
    String preventionTarget, // GENERAL_PUBLIC, STUDENTS, PROFESSIONALS, etc.
    Integer preventionParticipants,
    
    // Follow-up and Outcomes
    Boolean ongoingServices,
    String serviceCompletionReason,
    LocalDate lastContactDate,
    Boolean stabilizationAchieved,
    Boolean safetyGoalsAchieved,
    Boolean selfAdvocacySkillsGained,
    Boolean knowledgeOfRightsIncreased,
    
    // Cal OES RCP Specific Fields
    String calOesRcpGrantNumber,
    String reportingPeriod,
    String countyCode,
    String programSubcontractor,
    Boolean title9Eligible, // Title IX eligible institution
    
    // Data Quality and Privacy
    Integer dataQualityScore,
    Boolean confidentialityMaintained,
    Boolean informedConsentObtained,
    Boolean mandatoryReportingRequired,
    Boolean mandatoryReportMade,
    
    // Record Metadata
    LocalDate recordCreated,
    LocalDate recordUpdated,
    String createdBy,
    String updatedBy,
    LocalDateTime lastModified
) {

    /**
     * Create Cal OES RCP projection for reporting
     */
    public static CalOesRcpProjection fromServiceRecord(
            String recordId,
            String anonymizedSurvivorId,
            LocalDate serviceDate,
            String programType,
            String serviceType,
            String serviceMode,
            String ageGroup,
            String genderIdentity,
            String assaultType,
            String assaultTimeframe,
            String calOesRcpGrantNumber,
            String reportingPeriod,
            String countyCode) {
        
        return new CalOesRcpProjection(
            recordId,
            anonymizedSurvivorId,
            serviceDate,
            programType,
            serviceType,
            serviceMode,
            null, // Duration would be tracked separately
            null, // Staff ID anonymized for privacy
            
            // Demographics
            ageGroup,
            genderIdentity,
            null, // Sexual orientation
            null, // Race/ethnicity
            null, // Primary language
            null, // Disability status
            null, // Immigration status
            
            // Assault Information
            assaultType,
            assaultTimeframe,
            null, // Relationship to perpetrator
            null, // Assault location
            null, // Multiple perpetrators
            null, // Reported to police
            null, // Medical attention sought
            
            // Service Information
            null, // First contact method
            null, // Referral source
            null, // Crisis intervention
            null, // Information and referral
            null, // Individual counseling
            null, // Group counseling
            null, // Medical advocacy
            null, // Legal advocacy
            null, // Systems advocacy
            null, // Accompany to medical
            null, // Accompany to legal
            null, // Accompany to social services
            
            // Specialized Services
            null, // Trauma informed care
            null, // Culturally specific services
            null, // LGBTQ affirmative services
            null, // Disability accessible services
            null, // Language access services
            null, // Interpreter language
            
            // Prevention and Education
            null, // Prevention education provided
            null, // Prevention target
            null, // Prevention participants
            
            // Follow-up and Outcomes
            null, // Ongoing services
            null, // Service completion reason
            null, // Last contact date
            null, // Stabilization achieved
            null, // Safety goals achieved
            null, // Self advocacy skills gained
            null, // Knowledge of rights increased
            
            // Cal OES RCP
            calOesRcpGrantNumber,
            reportingPeriod,
            countyCode,
            null, // Program subcontractor
            null, // Title IX eligible
            
            // Data Quality
            calculateDataQualityScore(ageGroup, genderIdentity, assaultType, serviceType),
            true, // Confidentiality maintained
            true, // Informed consent obtained
            null, // Mandatory reporting required
            null, // Mandatory report made
            
            // Metadata
            LocalDate.now(),
            LocalDate.now(),
            "system",
            "system",
            LocalDateTime.now()
        );
    }

    private static Integer calculateDataQualityScore(String ageGroup, String genderIdentity, 
                                                   String assaultType, String serviceType) {
        int score = 0;
        int totalFields = 4;
        
        if (ageGroup != null && !ageGroup.isEmpty()) score++;
        if (genderIdentity != null && !genderIdentity.isEmpty()) score++;
        if (assaultType != null && !assaultType.isEmpty()) score++;
        if (serviceType != null && !serviceType.isEmpty()) score++;
        
        return (score * 100) / totalFields; // Percentage score
    }

    /**
     * Generate anonymized survivor identifier for reporting
     */
    public String getAnonymizedSurvivorId() {
        return survivorId;
    }

    /**
     * Determine if this record meets Cal OES RCP reporting requirements
     */
    public boolean meetsRcpReportingRequirements() {
        return recordId != null &&
               survivorId != null &&
               serviceDate != null &&
               programType != null &&
               calOesRcpGrantNumber != null &&
               reportingPeriod != null &&
               countyCode != null &&
               Boolean.TRUE.equals(confidentialityMaintained) &&
               Boolean.TRUE.equals(informedConsentObtained);
    }

    /**
     * Determine if this is a crisis response service
     */
    public boolean isCrisisResponse() {
        return "RCP_HOTLINE".equals(programType) ||
               Boolean.TRUE.equals(crisisIntervention) ||
               "RECENT_72_HOURS".equals(assaultTimeframe);
    }

    /**
     * Determine if specialized/culturally specific services were provided
     */
    public boolean hasSpecializedServices() {
        return Boolean.TRUE.equals(culturallySpecificServices) ||
               Boolean.TRUE.equals(lgbtqAffirmativeServices) ||
               Boolean.TRUE.equals(disabilityAccessibleServices) ||
               Boolean.TRUE.equals(languageAccessServices);
    }

    /**
     * Determine if advocacy services were provided
     */
    public boolean hasAdvocacyServices() {
        return Boolean.TRUE.equals(medicalAdvocacy) ||
               Boolean.TRUE.equals(legalAdvocacy) ||
               Boolean.TRUE.equals(systemsAdvocacy) ||
               Boolean.TRUE.equals(accompanyToMedical) ||
               Boolean.TRUE.equals(accompanyToLegal) ||
               Boolean.TRUE.equals(accompanyToSocialServices);
    }

    /**
     * Determine if positive outcomes were achieved
     */
    public boolean hasPositiveOutcomes() {
        return Boolean.TRUE.equals(stabilizationAchieved) ||
               Boolean.TRUE.equals(safetyGoalsAchieved) ||
               Boolean.TRUE.equals(selfAdvocacySkillsGained) ||
               Boolean.TRUE.equals(knowledgeOfRightsIncreased);
    }

    /**
     * Determine if this involves a minor (mandatory reporting considerations)
     */
    public boolean involvesMinor() {
        return "0-12".equals(ageGroup) || "13-17".equals(ageGroup);
    }

    /**
     * Convert to Cal OES RCP CSV format for reporting
     */
    public String toCalOesRcpCsvRow() {
        return String.join(",",
            quote(recordId),
            quote(getAnonymizedSurvivorId()),
            quote(formatDate(serviceDate)),
            quote(programType),
            quote(serviceType),
            quote(serviceMode),
            quote(ageGroup),
            quote(genderIdentity),
            quote(assaultType),
            quote(assaultTimeframe),
            String.valueOf(Boolean.TRUE.equals(crisisIntervention) ? 1 : 0),
            String.valueOf(Boolean.TRUE.equals(individualCounseling) ? 1 : 0),
            String.valueOf(Boolean.TRUE.equals(medicalAdvocacy) ? 1 : 0),
            String.valueOf(Boolean.TRUE.equals(legalAdvocacy) ? 1 : 0),
            String.valueOf(Boolean.TRUE.equals(preventionEducationProvided) ? 1 : 0),
            quote(calOesRcpGrantNumber),
            quote(reportingPeriod),
            quote(countyCode),
            String.valueOf(dataQualityScore != null ? dataQualityScore : 0),
            quote(formatDate(recordCreated))
        );
    }

    /**
     * Generate statistical summary for RCP reporting
     */
    public String generateRcpStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("RCP Service Summary:\n");
        stats.append("- Service Type: ").append(serviceType).append("\n");
        stats.append("- Service Mode: ").append(serviceMode).append("\n");
        stats.append("- Crisis Response: ").append(isCrisisResponse() ? "Yes" : "No").append("\n");
        stats.append("- Advocacy Services: ").append(hasAdvocacyServices() ? "Yes" : "No").append("\n");
        stats.append("- Specialized Services: ").append(hasSpecializedServices() ? "Yes" : "No").append("\n");
        stats.append("- Positive Outcomes: ").append(hasPositiveOutcomes() ? "Yes" : "No").append("\n");
        stats.append("- Data Quality Score: ").append(dataQualityScore).append("%\n");
        
        return stats.toString();
    }

    private String quote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }
}