package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for HMIS Physical Disability data compliance checking and validation
 * Ensures adherence to HMIS FY2024 UDE 3.08 requirements
 */
@Service
@Transactional(readOnly = true)
public class PhysicalDisabilityComplianceService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaPhysicalDisabilityRepository physicalDisabilityRepository;
    
    public PhysicalDisabilityComplianceService(
            @Lazy JpaProgramEnrollmentRepository enrollmentRepository,
            @Lazy JpaPhysicalDisabilityRepository physicalDisabilityRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.physicalDisabilityRepository = physicalDisabilityRepository;
    }
    
    /**
     * Comprehensive compliance check for a single enrollment
     */
    public ComplianceCheckResult checkEnrollmentCompliance(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        List<ValidationIssue> issues = new java.util.ArrayList<>();
        
        // Check PROJECT_START requirement
        if (!physicalDisabilityRepository.existsByEnrollmentIdAndStage(enrollmentId, "PROJECT_START")) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "MISSING_PROJECT_START",
                "Missing required PROJECT_START physical disability record",
                "HMIS requires a PROJECT_START physical disability record for all enrollments"
            ));
        }
        
        // Check PROJECT_EXIT requirement for exited enrollments
        if (enrollment.hasExited() && 
            !physicalDisabilityRepository.existsByEnrollmentIdAndStage(enrollmentId, "PROJECT_EXIT")) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "MISSING_PROJECT_EXIT",
                "Missing required PROJECT_EXIT physical disability record",
                "HMIS requires a PROJECT_EXIT physical disability record when enrollment exits"
            ));
        }
        
        // Check data quality of all records
        List<JpaPhysicalDisabilityEntity> records = physicalDisabilityRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        for (JpaPhysicalDisabilityEntity entity : records) {
            PhysicalDisabilityRecord record = entity.toDomainObject();
            if (!record.meetsDataQuality()) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING,
                    "DATA_QUALITY_ISSUE",
                    "Physical disability record has data quality issues",
                    String.format("Record %s does not meet HMIS data quality standards", record.getRecordId())
                ));
            }
            
            // Check specific validation rules
            validateRecordSpecificRules(record, issues);
        }
        
        // Check for logical consistency
        validateLogicalConsistency(records.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .collect(Collectors.toList()), issues);
        
        boolean isCompliant = issues.stream().noneMatch(i -> i.severity() == ValidationIssue.Severity.ERROR);
        
        return new ComplianceCheckResult(
            enrollmentId,
            isCompliant,
            issues,
            records.size(),
            LocalDate.now()
        );
    }
    
    /**
     * Bulk compliance check for multiple enrollments
     */
    public List<ComplianceCheckResult> checkBulkCompliance(List<UUID> enrollmentIds) {
        return enrollmentIds.parallelStream()
            .map(this::checkEnrollmentCompliance)
            .collect(Collectors.toList());
    }
    
    /**
     * Find all enrollments with compliance issues
     */
    public List<ComplianceCheckResult> findEnrollmentsWithComplianceIssues() {
        // Find enrollments missing PROJECT_START records
        List<UUID> missingStart = physicalDisabilityRepository.findEnrollmentsMissingProjectStartRecord();
        
        // Find enrollments missing PROJECT_EXIT records
        List<UUID> missingExit = physicalDisabilityRepository.findEnrollmentsMissingProjectExitRecord();
        
        // Find records with data quality issues
        List<JpaPhysicalDisabilityEntity> dataQualityIssues = 
            physicalDisabilityRepository.findRecordsWithDataQualityIssues();
        List<UUID> dataQualityEnrollments = dataQualityIssues.stream()
            .map(JpaPhysicalDisabilityEntity::getEnrollmentId)
            .distinct()
            .collect(Collectors.toList());
        
        // Combine all problematic enrollment IDs
        List<UUID> allProblematicEnrollments = new java.util.ArrayList<>();
        allProblematicEnrollments.addAll(missingStart);
        allProblematicEnrollments.addAll(missingExit);
        allProblematicEnrollments.addAll(dataQualityEnrollments);
        
        // Remove duplicates and check compliance
        return allProblematicEnrollments.stream()
            .distinct()
            .map(this::checkEnrollmentCompliance)
            .filter(result -> !result.isCompliant())
            .collect(Collectors.toList());
    }
    
    /**
     * Get compliance statistics for reporting
     */
    public ComplianceStatistics getComplianceStatistics(LocalDate startDate, LocalDate endDate) {
        // Get all enrollments in date range
        List<JpaProgramEnrollmentEntity> enrollments = enrollmentRepository.findAllByEnrollmentDateBetween(startDate, endDate);
        
        long totalEnrollments = enrollments.size();
        long compliantEnrollments = 0;
        long missingProjectStart = 0;
        long missingProjectExit = 0;
        long dataQualityIssues = 0;
        
        for (JpaProgramEnrollmentEntity enrollment : enrollments) {
            ComplianceCheckResult result = checkEnrollmentCompliance(enrollment.getId());
            if (result.isCompliant()) {
                compliantEnrollments++;
            } else {
                // Count specific issue types
                if (result.issues().stream().anyMatch(i -> "MISSING_PROJECT_START".equals(i.code()))) {
                    missingProjectStart++;
                }
                if (result.issues().stream().anyMatch(i -> "MISSING_PROJECT_EXIT".equals(i.code()))) {
                    missingProjectExit++;
                }
                if (result.issues().stream().anyMatch(i -> "DATA_QUALITY_ISSUE".equals(i.code()))) {
                    dataQualityIssues++;
                }
            }
        }
        
        double complianceRate = totalEnrollments > 0 ? (double) compliantEnrollments / totalEnrollments : 0.0;
        
        return new ComplianceStatistics(
            totalEnrollments,
            compliantEnrollments,
            totalEnrollments - compliantEnrollments,
            complianceRate,
            missingProjectStart,
            missingProjectExit,
            dataQualityIssues,
            startDate,
            endDate
        );
    }
    
    /**
     * Validate record-specific business rules
     */
    private void validateRecordSpecificRules(PhysicalDisabilityRecord record, List<ValidationIssue> issues) {
        // Rule: If physical_disability = YES, physical_expected_long_term should be collected
        if (record.getPhysicalDisability() == HmisFivePointResponse.YES) {
            if (record.getPhysicalExpectedLongTerm() == null || 
                record.getPhysicalExpectedLongTerm() == HmisFivePointResponse.DATA_NOT_COLLECTED) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING,
                    "MISSING_EXPECTED_LONG_TERM",
                    "Missing physical expected long-term response when physical disability = YES",
                    "When physical disability is YES, expected long-term should be collected for complete assessment"
                ));
            }
        }
        
        // Rule: If physical_disability = NO, physical_expected_long_term should not be collected
        if (record.getPhysicalDisability() == HmisFivePointResponse.NO) {
            if (record.getPhysicalExpectedLongTerm() != null && 
                record.getPhysicalExpectedLongTerm() != HmisFivePointResponse.DATA_NOT_COLLECTED) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING,
                    "UNNECESSARY_EXPECTED_LONG_TERM",
                    "Unexpected physical expected long-term response when physical disability = NO",
                    "When physical disability is NO, expected long-term should not be collected"
                ));
            }
        }
        
        // Rule: Information date should not be in the future
        if (record.getInformationDate().isAfter(LocalDate.now())) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.ERROR,
                "FUTURE_INFORMATION_DATE",
                "Information date is in the future",
                "Information date cannot be after current date"
            ));
        }
        
        // Rule: Correction records should reference valid original records
        if (record.isCorrection() && record.getCorrectsRecordId() != null) {
            if (!physicalDisabilityRepository.existsById(record.getCorrectsRecordId())) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.ERROR,
                    "INVALID_CORRECTION_REFERENCE",
                    "Correction record references non-existent original record",
                    "Correction records must reference valid original records"
                ));
            }
        }
    }
    
    /**
     * Validate logical consistency across multiple records
     */
    private void validateLogicalConsistency(List<PhysicalDisabilityRecord> records, List<ValidationIssue> issues) {
        if (records.size() < 2) return;
        
        // Sort by information date
        List<PhysicalDisabilityRecord> sortedRecords = records.stream()
            .filter(r -> !r.isCorrection()) // Exclude corrections from consistency checks
            .sorted((r1, r2) -> r1.getInformationDate().compareTo(r2.getInformationDate()))
            .collect(Collectors.toList());
        
        // Check for reasonable progression of physical disability status
        for (int i = 1; i < sortedRecords.size(); i++) {
            PhysicalDisabilityRecord prev = sortedRecords.get(i - 1);
            PhysicalDisabilityRecord current = sortedRecords.get(i);
            
            // Flag sudden changes from NO to YES (might need documentation)
            if (prev.getPhysicalDisability() == HmisFivePointResponse.NO && 
                current.getPhysicalDisability() == HmisFivePointResponse.YES) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.INFO,
                    "DISABILITY_STATUS_CHANGE",
                    "Physical disability status changed from NO to YES",
                    "Sudden change in disability status may require additional documentation"
                ));
            }
        }
    }
    
    // Data Transfer Objects
    
    public record ComplianceCheckResult(
        UUID enrollmentId,
        boolean isCompliant,
        List<ValidationIssue> issues,
        int totalRecords,
        LocalDate checkedDate
    ) {}
    
    public record ValidationIssue(
        Severity severity,
        String code,
        String message,
        String description
    ) {
        public enum Severity {
            ERROR,   // Must be fixed for compliance
            WARNING, // Should be fixed for best practice
            INFO     // Informational, no action required
        }
    }
    
    public record ComplianceStatistics(
        long totalEnrollments,
        long compliantEnrollments,
        long nonCompliantEnrollments,
        double complianceRate,
        long missingProjectStartRecords,
        long missingProjectExitRecords,
        long dataQualityIssues,
        LocalDate reportStartDate,
        LocalDate reportEndDate
    ) {}
}
