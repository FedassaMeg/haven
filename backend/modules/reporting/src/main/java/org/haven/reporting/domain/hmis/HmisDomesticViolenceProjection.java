package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HMIS Domestic Violence CSV projection
 * Represents domestic violence data for HMIS CSV export per FY2024 Data Standards UDE 4.11
 * Implements enhanced privacy and security considerations for sensitive DV data
 */
public record HmisDomesticViolenceProjection(
    String domesticViolenceId,
    String enrollmentId,
    HmisPersonalId personalId,
    LocalDate informationDate,
    String dataCollectionStage,
    
    // Domestic Violence Elements (UDE 4.11)
    DomesticViolence domesticViolenceVictim,        // 4.11.1 DV History
    DomesticViolenceRecency whenOccurred,          // 4.11.2 When DV Experienced  
    DomesticViolence currentlyFleeing,             // 4.11.3 Currently Fleeing DV
    
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId,
    
    // Additional metadata for enhanced security
    boolean requiresSpecialHandling,               // Flag for high-risk cases
    String dataClassificationLevel                 // SENSITIVE, RESTRICTED, etc.
) {
    
    /**
     * Create projection from real DV domain record (recommended approach)
     */
    public static HmisDomesticViolenceProjection fromDvRecord(
            String enrollmentId,
            HmisPersonalId personalId,
            DvRecord dvRecord,
            String userId,
            String exportId) {
        
        if (dvRecord == null) {
            return createEmptyProjection(enrollmentId, personalId, DataCollectionStage.PROJECT_START, userId, exportId);
        }
        
        return new HmisDomesticViolenceProjection(
            generateDvId(enrollmentId, dvRecord.getStage()),
            enrollmentId,
            personalId,
            dvRecord.getInformationDate(),
            mapDataCollectionStageToHmisValue(dvRecord.getStage()),
            
            // Map from domain record to HMIS values
            mapHmisFivePointToDomesticViolence(dvRecord.getDvHistory()),
            dvRecord.getWhenExperienced(), // Direct mapping - enum values align
            mapHmisFivePointToDomesticViolence(dvRecord.getCurrentlyFleeing()),
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId,
            
            // Security metadata
            dvRecord.requiresEnhancedSafety(),
            determineDataClassificationLevel(dvRecord)
        );
    }
    
    /**
     * Create projection from ProgramEnrollment aggregate
     */
    public static HmisDomesticViolenceProjection fromProgramEnrollment(
            ProgramEnrollment enrollment,
            DataCollectionStage stage,
            String userId,
            String exportId) {
        
        DvRecord dvRecord = enrollment.getDvRecord(stage);
        
        return fromDvRecord(
            enrollment.getId().value().toString(),
            HmisPersonalId.of(enrollment.getClientId().value().toString()),
            dvRecord,
            userId,
            exportId
        );
    }
    
    /**
     * Create multiple projections for all stages of an enrollment
     */
    public static List<HmisDomesticViolenceProjection> createAllStagesForEnrollment(
            ProgramEnrollment enrollment,
            String userId,
            String exportId) {
        
        return List.of(DataCollectionStage.values()).stream()
            .filter(stage -> enrollment.getDvRecord(stage) != null)
            .map(stage -> fromProgramEnrollment(enrollment, stage, userId, exportId))
            .toList();
    }
    
    /**
     * Create projection with enhanced privacy protection
     */
    public static HmisDomesticViolenceProjection fromDvRecordWithPrivacyProtection(
            String enrollmentId,
            HmisPersonalId personalId,
            DvRecord dvRecord,
            String userId,
            String exportId,
            boolean allowSensitiveExport) {
        
        if (dvRecord == null) {
            return createEmptyProjection(enrollmentId, personalId, DataCollectionStage.PROJECT_START, userId, exportId);
        }
        
        // If sensitive export not allowed and record is high-risk, sanitize data
        if (!allowSensitiveExport && dvRecord.requiresEnhancedSafety()) {
            return createSanitizedProjection(enrollmentId, personalId, dvRecord, userId, exportId);
        }
        
        return fromDvRecord(enrollmentId, personalId, dvRecord, userId, exportId);
    }
    
    private static HmisDomesticViolenceProjection createEmptyProjection(
            String enrollmentId,
            HmisPersonalId personalId,
            DataCollectionStage stage,
            String userId,
            String exportId) {
        
        return new HmisDomesticViolenceProjection(
            generateDvId(enrollmentId, stage),
            enrollmentId,
            personalId,
            LocalDate.now(),
            mapDataCollectionStageToHmisValue(stage),
            
            // All DV data as data not collected
            DomesticViolence.DATA_NOT_COLLECTED,
            null, // When occurred not applicable
            DomesticViolence.DATA_NOT_COLLECTED,
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId,
            
            false, // Not requiring special handling
            "STANDARD"
        );
    }
    
    private static HmisDomesticViolenceProjection createSanitizedProjection(
            String enrollmentId,
            HmisPersonalId personalId,
            DvRecord dvRecord,
            String userId,
            String exportId) {
        
        return new HmisDomesticViolenceProjection(
            generateDvId(enrollmentId, dvRecord.getStage()),
            enrollmentId,
            personalId,
            dvRecord.getInformationDate(),
            mapDataCollectionStageToHmisValue(dvRecord.getStage()),
            
            // Sanitized DV data - only show basic status, no details
            dvRecord.hasDvHistory() ? DomesticViolence.YES : DomesticViolence.NO,
            null, // Sensitive timing information removed
            DomesticViolence.DATA_NOT_COLLECTED, // Currently fleeing information removed for safety
            
            LocalDate.now(),
            LocalDate.now(),
            userId + "_SANITIZED",
            null,
            exportId,
            
            true, // Requires special handling
            "SANITIZED"
        );
    }
    
    /**
     * Map HmisFivePoint to DomesticViolence enum for CSV export
     */
    private static DomesticViolence mapHmisFivePointToDomesticViolence(HmisFivePoint response) {
        if (response == null) {
            return DomesticViolence.DATA_NOT_COLLECTED;
        }
        
        return switch (response) {
            case YES -> DomesticViolence.YES;
            case NO -> DomesticViolence.NO;
            case CLIENT_DOESNT_KNOW -> DomesticViolence.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED -> DomesticViolence.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED -> DomesticViolence.DATA_NOT_COLLECTED;
        };
    }
    
    private static String mapDataCollectionStageToHmisValue(DataCollectionStage stage) {
        return switch (stage) {
            case PROJECT_START -> "1";
            case UPDATE -> "2"; 
            case PROJECT_EXIT -> "3";
        };
    }
    
    private static String determineDataClassificationLevel(DvRecord dvRecord) {
        if (dvRecord.requiresEnhancedSafety()) {
            return "RESTRICTED";
        }
        if (dvRecord.hasDvHistory()) {
            return "SENSITIVE";
        }
        return "STANDARD";
    }
    
    private static String generateDvId(String enrollmentId, DataCollectionStage stage) {
        return "DV_" + enrollmentId + "_" + stage.name();
    }
    
    /**
     * Get HMIS CSV header
     */
    public static String getCsvHeader() {
        return String.join(",",
            "DomesticViolenceID",
            "EnrollmentID", 
            "PersonalID",
            "InformationDate",
            "DataCollectionStage",
            "DomesticViolenceVictim",
            "WhenOccurred",
            "CurrentlyFleeing",
            "DateCreated",
            "DateUpdated",
            "UserID",
            "DateDeleted",
            "ExportID"
        );
    }
    
    /**
     * Convert to CSV row format with security considerations
     */
    public String toCsvRow() {
        return String.join(",",
            quote(domesticViolenceId),
            quote(enrollmentId),
            quote(personalId.value()),
            quote(formatDate(informationDate)),
            quote(dataCollectionStage),
            String.valueOf(domesticViolenceVictim.getHmisValue()),
            whenOccurred != null ? String.valueOf(whenOccurred.getHmisValue()) : "",
            String.valueOf(currentlyFleeing.getHmisValue()),
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }
    
    /**
     * Convert to CSV row with enhanced privacy protection
     */
    public String toCsvRowWithPrivacyProtection(boolean allowSensitiveExport) {
        if (!allowSensitiveExport && requiresSpecialHandling) {
            // Return sanitized version
            return String.join(",",
                quote(domesticViolenceId),
                quote(enrollmentId),
                quote(personalId.value()),
                quote(formatDate(informationDate)),
                quote(dataCollectionStage),
                String.valueOf(domesticViolenceVictim.getHmisValue()),
                "", // Remove timing information
                String.valueOf(DomesticViolence.DATA_NOT_COLLECTED.getHmisValue()), // Remove fleeing status
                quote(formatDate(dateCreated)),
                quote(formatDate(dateUpdated)),
                quote(userId + "_SANITIZED"),
                quote(formatDateTime(dateDeleted)),
                quote(exportId)
            );
        }
        
        return toCsvRow();
    }
    
    /**
     * Safety and risk assessment methods
     */
    
    public boolean indicatesCurrentDanger() {
        return currentlyFleeing == DomesticViolence.YES;
    }
    
    public boolean hasRecentDvExperience() {
        return whenOccurred == DomesticViolenceRecency.WITHIN_3_MONTHS ||
               whenOccurred == DomesticViolenceRecency.THREE_TO_SIX_MONTHS;
    }
    
    public boolean hasVeryRecentDvExperience() {
        return whenOccurred == DomesticViolenceRecency.WITHIN_3_MONTHS;
    }
    
    public RiskLevel assessRiskLevel() {
        if (indicatesCurrentDanger()) {
            return RiskLevel.CRITICAL;
        }
        if (hasVeryRecentDvExperience()) {
            return RiskLevel.HIGH;
        }
        if (hasRecentDvExperience()) {
            return RiskLevel.MEDIUM;
        }
        if (domesticViolenceVictim == DomesticViolence.YES) {
            return RiskLevel.LOW;
        }
        return RiskLevel.NONE;
    }
    
    /**
     * Data quality validation
     */
    public List<String> validateDataQuality() {
        List<String> issues = new java.util.ArrayList<>();
        
        // Business rule: If DV history is YES, when occurred should be specified
        if (domesticViolenceVictim == DomesticViolence.YES && whenOccurred == null) {
            issues.add("When DV occurred must be specified if DV history is YES");
        }
        
        // Business rule: If currently fleeing is YES, DV history should be YES
        if (currentlyFleeing == DomesticViolence.YES && 
            domesticViolenceVictim != DomesticViolence.YES &&
            domesticViolenceVictim != DomesticViolence.DATA_NOT_COLLECTED) {
            issues.add("If currently fleeing DV, should have DV history or DV history should be DATA_NOT_COLLECTED");
        }
        
        // Information date validation
        if (informationDate != null && informationDate.isAfter(LocalDate.now())) {
            issues.add("Information date cannot be in the future");
        }
        
        return issues;
    }
    
    public enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private String quote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    
    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "";
    }
}