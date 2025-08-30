package org.haven.reporting.domain.hmis;

import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HMIS Disabilities.csv projection
 * Represents all 6 disability types for HMIS CSV export per FY2024 Data Standards
 * Replaces legacy approach with real domain model integration
 */
public record HmisDisabilitiesProjection(
    String disabilitiesId,
    String enrollmentId,
    HmisPersonalId personalId,
    LocalDate informationDate,
    String dataCollectionStage,
    
    // Six disability types (UDE 3.09-3.13 + Physical 3.08)
    DisabilityType physicalDisability,
    DisabilityType developmentalDisability,
    DisabilityType chronicHealthCondition,
    DisabilityType hivAidsDisability,
    DisabilityType mentalHealthDisability,
    DisabilityType substanceAbuseDisability,
    
    LocalDate dateCreated,
    LocalDate dateUpdated,
    String userId,
    LocalDateTime dateDeleted,
    String exportId
) {
    
    /**
     * Create projection from real disability domain records (recommended approach)
     */
    public static HmisDisabilitiesProjection fromDisabilityRecords(
            String enrollmentId,
            HmisPersonalId personalId,
            List<DisabilityRecord> disabilityRecords,
            DataCollectionStage stage,
            String userId,
            String exportId) {
        
        if (disabilityRecords == null || disabilityRecords.isEmpty()) {
            return createEmptyProjection(enrollmentId, personalId, stage, userId, exportId);
        }
        
        // Filter records for the specific stage
        Map<DisabilityKind, DisabilityRecord> recordsByKind = disabilityRecords.stream()
            .filter(record -> record.getStage() == stage)
            .filter(record -> !record.isCorrection()) // Use only original records, not corrections
            .collect(Collectors.toMap(
                DisabilityRecord::getDisabilityKind,
                record -> record,
                (existing, replacement) -> {
                    // If multiple records exist, use the most recent
                    return existing.getInformationDate().isAfter(replacement.getInformationDate()) 
                        ? existing : replacement;
                }
            ));
        
        // Get the latest information date
        LocalDate informationDate = recordsByKind.values().stream()
            .map(DisabilityRecord::getInformationDate)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        return new HmisDisabilitiesProjection(
            generateDisabilitiesId(enrollmentId, stage),
            enrollmentId,
            personalId,
            informationDate,
            mapDataCollectionStageToHmisValue(stage),
            
            // Map each disability kind to HMIS DisabilityType
            mapDisabilityRecord(recordsByKind.get(DisabilityKind.PHYSICAL)),
            mapDisabilityRecord(recordsByKind.get(DisabilityKind.DEVELOPMENTAL)),
            mapDisabilityRecord(recordsByKind.get(DisabilityKind.CHRONIC_HEALTH)),
            mapDisabilityRecord(recordsByKind.get(DisabilityKind.HIV_AIDS)),
            mapDisabilityRecord(recordsByKind.get(DisabilityKind.MENTAL_HEALTH)),
            mapDisabilityRecord(recordsByKind.get(DisabilityKind.SUBSTANCE_USE)),
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    /**
     * Create projection from physical disability record (for backwards compatibility)
     */
    public static HmisDisabilitiesProjection fromPhysicalDisabilityRecord(
            String enrollmentId,
            HmisPersonalId personalId,
            PhysicalDisabilityRecord physicalRecord,
            DataCollectionStage stage,
            String userId,
            String exportId) {
        
        if (physicalRecord == null) {
            return createEmptyProjection(enrollmentId, personalId, stage, userId, exportId);
        }
        
        return new HmisDisabilitiesProjection(
            generateDisabilitiesId(enrollmentId, stage),
            enrollmentId,
            personalId,
            physicalRecord.getInformationDate(),
            mapDataCollectionStageToHmisValue(stage),
            
            // Physical disability from record
            mapPhysicalDisabilityRecord(physicalRecord),
            
            // Other disabilities as data not collected
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    /**
     * Create comprehensive disability projection from enrollment aggregate
     */
    public static HmisDisabilitiesProjection fromProgramEnrollment(
            ProgramEnrollment enrollment,
            DataCollectionStage stage,
            String userId,
            String exportId) {
        
        // Get all disability records for the stage
        List<DisabilityRecord> allDisabilityRecords = enrollment.getAllDisabilityRecords();
        
        // Also include physical disability records if available
        List<PhysicalDisabilityRecord> physicalRecords = enrollment.getPhysicalDisabilityRecords();
        
        return fromDisabilityRecords(
            enrollment.getId().value().toString(),
            HmisPersonalId.of(enrollment.getClientId().value().toString()),
            allDisabilityRecords,
            stage,
            userId,
            exportId
        );
    }
    
    /**
     * Create multiple projections for all stages of an enrollment
     */
    public static List<HmisDisabilitiesProjection> createAllStagesForEnrollment(
            ProgramEnrollment enrollment,
            String userId,
            String exportId) {
        
        return List.of(DataCollectionStage.values()).stream()
            .filter(stage -> enrollmentHasDataForStage(enrollment, stage))
            .map(stage -> fromProgramEnrollment(enrollment, stage, userId, exportId))
            .toList();
    }
    
    private static boolean enrollmentHasDataForStage(ProgramEnrollment enrollment, DataCollectionStage stage) {
        // Check if enrollment has any disability data for this stage
        boolean hasDisabilityData = enrollment.getAllDisabilityRecords().stream()
            .anyMatch(record -> record.getStage() == stage);
        
        boolean hasPhysicalDisabilityData = enrollment.getPhysicalDisabilityRecords().stream()
            .anyMatch(record -> record.getStage() == stage);
        
        return hasDisabilityData || hasPhysicalDisabilityData;
    }
    
    private static HmisDisabilitiesProjection createEmptyProjection(
            String enrollmentId,
            HmisPersonalId personalId,
            DataCollectionStage stage,
            String userId,
            String exportId) {
        
        return new HmisDisabilitiesProjection(
            generateDisabilitiesId(enrollmentId, stage),
            enrollmentId,
            personalId,
            LocalDate.now(),
            mapDataCollectionStageToHmisValue(stage),
            
            // All disabilities as data not collected
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            DisabilityType.DATA_NOT_COLLECTED,
            
            LocalDate.now(),
            LocalDate.now(),
            userId,
            null,
            exportId
        );
    }
    
    /**
     * Map DisabilityRecord to HMIS DisabilityType for CSV export
     */
    private static DisabilityType mapDisabilityRecord(DisabilityRecord record) {
        if (record == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        HmisFivePoint response = record.getHasDisability();
        return mapHmisFivePointToDisabilityType(response);
    }
    
    /**
     * Map PhysicalDisabilityRecord to HMIS DisabilityType for CSV export
     */
    private static DisabilityType mapPhysicalDisabilityRecord(PhysicalDisabilityRecord record) {
        if (record == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        HmisFivePointResponse response = record.getPhysicalDisability();
        if (response == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        // Convert HmisFivePointResponse to HmisFivePoint for mapping
        HmisFivePoint hmisFivePoint = switch (response) {
            case YES -> HmisFivePoint.YES;
            case NO -> HmisFivePoint.NO;
            case CLIENT_DOESNT_KNOW -> HmisFivePoint.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED -> HmisFivePoint.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED -> HmisFivePoint.DATA_NOT_COLLECTED;
        };
        
        return mapHmisFivePointToDisabilityType(hmisFivePoint);
    }
    
    private static DisabilityType mapHmisFivePointToDisabilityType(HmisFivePoint response) {
        if (response == null) {
            return DisabilityType.DATA_NOT_COLLECTED;
        }
        
        return switch (response) {
            case YES -> DisabilityType.YES;
            case NO -> DisabilityType.NO;
            case CLIENT_DOESNT_KNOW -> DisabilityType.CLIENT_DOESNT_KNOW;
            case CLIENT_REFUSED -> DisabilityType.CLIENT_REFUSED;
            case DATA_NOT_COLLECTED -> DisabilityType.DATA_NOT_COLLECTED;
        };
    }
    
    private static String mapDataCollectionStageToHmisValue(DataCollectionStage stage) {
        return switch (stage) {
            case PROJECT_START -> "1";
            case UPDATE -> "2"; 
            case PROJECT_EXIT -> "3";
        };
    }
    
    private static String generateDisabilitiesId(String enrollmentId, DataCollectionStage stage) {
        return "DIS_" + enrollmentId + "_" + stage.name();
    }
    
    /**
     * Get HMIS CSV header
     */
    public static String getCsvHeader() {
        return String.join(",",
            "DisabilitiesID",
            "EnrollmentID", 
            "PersonalID",
            "InformationDate",
            "DataCollectionStage",
            "PhysicalDisability",
            "DevelopmentalDisability", 
            "ChronicHealthCondition",
            "HIVAIDSDisability",
            "MentalHealthDisability",
            "SubstanceAbuseDisability",
            "DateCreated",
            "DateUpdated",
            "UserID",
            "DateDeleted",
            "ExportID"
        );
    }
    
    /**
     * Convert to CSV row format
     */
    public String toCsvRow() {
        return String.join(",",
            quote(disabilitiesId),
            quote(enrollmentId),
            quote(personalId.value()),
            quote(formatDate(informationDate)),
            quote(dataCollectionStage),
            String.valueOf(physicalDisability.getHmisValue()),
            String.valueOf(developmentalDisability.getHmisValue()),
            String.valueOf(chronicHealthCondition.getHmisValue()),
            String.valueOf(hivAidsDisability.getHmisValue()),
            String.valueOf(mentalHealthDisability.getHmisValue()),
            String.valueOf(substanceAbuseDisability.getHmisValue()),
            quote(formatDate(dateCreated)),
            quote(formatDate(dateUpdated)),
            quote(userId),
            quote(formatDateTime(dateDeleted)),
            quote(exportId)
        );
    }
    
    /**
     * Check if this projection indicates any disabling conditions
     */
    public boolean hasDisablingConditions() {
        return physicalDisability == DisabilityType.YES ||
               developmentalDisability == DisabilityType.YES ||
               chronicHealthCondition == DisabilityType.YES ||
               hivAidsDisability == DisabilityType.YES ||
               mentalHealthDisability == DisabilityType.YES ||
               substanceAbuseDisability == DisabilityType.YES;
    }
    
    /**
     * Count number of disabilities marked as YES
     */
    public int getDisabilityCount() {
        int count = 0;
        if (physicalDisability == DisabilityType.YES) count++;
        if (developmentalDisability == DisabilityType.YES) count++;
        if (chronicHealthCondition == DisabilityType.YES) count++;
        if (hivAidsDisability == DisabilityType.YES) count++;
        if (mentalHealthDisability == DisabilityType.YES) count++;
        if (substanceAbuseDisability == DisabilityType.YES) count++;
        return count;
    }
    
    /**
     * Check if has behavioral health disabilities (mental health or substance use)
     */
    public boolean hasBehavioralHealthDisabilities() {
        return mentalHealthDisability == DisabilityType.YES ||
               substanceAbuseDisability == DisabilityType.YES;
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