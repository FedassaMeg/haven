package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application Service for HMIS-compliant Domestic Violence lifecycle management
 * Handles UDE 4.11 Domestic Violence data collection per HMIS FY2024 standards
 * Includes enhanced security and privacy considerations for DV records
 */
@Service
@Transactional
public class DvLifecycleService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaDvRepository dvRepository;
    
    public DvLifecycleService(
            JpaProgramEnrollmentRepository enrollmentRepository,
            JpaDvRepository dvRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.dvRepository = dvRepository;
    }
    
    /**
     * Create required PROJECT_START DV record when client enrolls
     */
    public DvRecord createProjectStartRecord(
            UUID enrollmentId,
            HmisFivePoint dvHistory,
            DomesticViolenceRecency whenExperienced,
            HmisFivePoint currentlyFleeing,
            String collectedBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create PROJECT_START record through aggregate
        enrollment.createStartDvRecord(dvHistory, whenExperienced, currentlyFleeing, collectedBy);
        
        // Get the created record
        DvRecord startRecord = enrollment.getDvRecord(DataCollectionStage.PROJECT_START);
        
        // Persist the record
        JpaDvEntity dvEntity = new JpaDvEntity(startRecord);
        dvRepository.save(dvEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return startRecord;
    }
    
    /**
     * Create DV update record due to change in circumstances
     */
    public DvRecord createUpdateRecord(
            UUID enrollmentId,
            LocalDate changeDate,
            HmisFivePoint dvHistory,
            DomesticViolenceRecency whenExperienced,
            HmisFivePoint currentlyFleeing,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create UPDATE record
        enrollment.createDvUpdateRecord(changeDate, dvHistory, whenExperienced, currentlyFleeing, collectedBy);
        
        // Get the most recent record (should be the update we just created)
        DvRecord updateRecord = enrollment.getMostRecentDvRecord();
        
        // Persist the record
        JpaDvEntity dvEntity = new JpaDvEntity(updateRecord);
        dvRepository.save(dvEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return updateRecord;
    }
    
    /**
     * Create required PROJECT_EXIT DV record when client exits
     */
    public DvRecord createProjectExitRecord(
            UUID enrollmentId,
            LocalDate exitDate,
            HmisFivePoint dvHistory,
            DomesticViolenceRecency whenExperienced,
            HmisFivePoint currentlyFleeing,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create PROJECT_EXIT record
        enrollment.createExitDvRecord(exitDate, dvHistory, whenExperienced, currentlyFleeing, collectedBy);
        
        // Get the exit record
        DvRecord exitRecord = enrollment.getDvRecord(DataCollectionStage.PROJECT_EXIT);
        
        // Persist the record
        JpaDvEntity dvEntity = new JpaDvEntity(exitRecord);
        dvRepository.save(dvEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return exitRecord;
    }
    
    /**
     * Create correction record for an existing DV record
     */
    public DvRecord createCorrectionRecord(
            UUID originalRecordId,
            HmisFivePoint dvHistory,
            DomesticViolenceRecency whenExperienced,
            HmisFivePoint currentlyFleeing,
            String collectedBy) {
        
        JpaDvEntity originalEntity = dvRepository.findById(originalRecordId)
            .orElseThrow(() -> new IllegalArgumentException("DV record not found: " + originalRecordId));
        
        DvRecord originalRecord = originalEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(originalRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + originalRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create correction record
        enrollment.correctDvRecord(originalRecord, dvHistory, whenExperienced, currentlyFleeing, collectedBy);
        
        // Get the correction record (should be the most recent one)
        DvRecord correctionRecord = enrollment.getDvRecords().stream()
            .filter(record -> record.isCorrection() && record.getCorrectsRecordId().equals(originalRecordId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Correction record not found after creation"));
        
        // Persist the correction record
        JpaDvEntity correctionEntity = new JpaDvEntity(correctionRecord);
        dvRepository.save(correctionEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return correctionRecord;
    }
    
    /**
     * Get all DV records for an enrollment
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getDvRecordsForEnrollment(UUID enrollmentId) {
        List<JpaDvEntity> entities = dvRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get the most recent DV record for an enrollment
     */
    @Transactional(readOnly = true)
    public DvRecord getMostRecentDvRecord(UUID enrollmentId) {
        return dvRepository.findFirstByEnrollmentIdOrderByInformationDateDesc(enrollmentId)
            .map(JpaDvEntity::toDomainObject)
            .orElse(null);
    }
    
    /**
     * Get latest effective DV record (accounting for corrections)
     */
    @Transactional(readOnly = true)
    public DvRecord getLatestEffectiveDvRecord(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getLatestEffectiveDvRecord();
    }
    
    /**
     * Check if enrollment has any DV history
     */
    @Transactional(readOnly = true)
    public boolean hasDvHistory(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.hasDvHistory();
    }
    
    /**
     * Check if client is currently fleeing DV (highest risk)
     */
    @Transactional(readOnly = true)
    public boolean isCurrentlyFleeingDv(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.isCurrentlyFleeingDv();
    }
    
    /**
     * Check if enrollment requires enhanced safety protocols
     */
    @Transactional(readOnly = true)
    public boolean requiresEnhancedSafety(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.requiresEnhancedSafety();
    }
    
    /**
     * Get DV safety assessment for an enrollment
     */
    @Transactional(readOnly = true)
    public DvSafetyAssessment getDvSafetyAssessment(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getDvSafetyAssessment();
    }
    
    /**
     * Find records with DV history (high sensitivity)
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getRecordsWithDvHistory(LocalDate startDate, LocalDate endDate) {
        List<JpaDvEntity> entities = dvRepository.findRecordsWithDvHistory(startDate, endDate);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Find records with clients currently fleeing DV (highest risk)
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getRecordsCurrentlyFleeingDv(LocalDate startDate, LocalDate endDate) {
        List<JpaDvEntity> entities = dvRepository.findRecordsCurrentlyFleeingDv(startDate, endDate);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Find recent DV records (within 6 months)
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getRecentDvRecords(LocalDate startDate, LocalDate endDate) {
        List<JpaDvEntity> entities = dvRepository.findRecentDvRecords(startDate, endDate);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Find enrollments missing PROJECT_START DV records
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingProjectStartRecord() {
        return dvRepository.findEnrollmentsMissingProjectStartRecord();
    }
    
    /**
     * Find enrollments that exited but don't have PROJECT_EXIT records
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingProjectExitRecord() {
        return dvRepository.findEnrollmentsMissingProjectExitRecord();
    }
    
    /**
     * Find records with data quality issues that may need correction
     */
    @Transactional(readOnly = true)
    public List<DvRecord> findRecordsWithDataQualityIssues() {
        List<JpaDvEntity> entities = dvRepository.findRecordsWithDataQualityIssues();
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Check if enrollment has required PROJECT_START record
     */
    @Transactional(readOnly = true)
    public boolean hasProjectStartRecord(UUID enrollmentId) {
        return dvRepository.existsByEnrollmentIdAndStage(enrollmentId, "PROJECT_START");
    }
    
    /**
     * Check if enrollment has PROJECT_EXIT record
     */
    @Transactional(readOnly = true)
    public boolean hasProjectExitRecord(UUID enrollmentId) {
        return dvRepository.existsByEnrollmentIdAndStage(enrollmentId, "PROJECT_EXIT");
    }
    
    /**
     * Check HMIS compliance for DV data collection
     */
    @Transactional(readOnly = true)
    public boolean meetsDvDataCompliance(UUID enrollmentId) {
        // Must have PROJECT_START record
        if (!hasProjectStartRecord(enrollmentId)) {
            return false;
        }
        
        // Check if enrollment is exited and needs PROJECT_EXIT record
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElse(null);
        
        if (enrollmentEntity != null) {
            ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
            if (enrollment.hasExited() && !hasProjectExitRecord(enrollmentId)) {
                return false;
            }
        }
        
        // Check data quality of all records
        List<JpaDvEntity> records = dvRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return records.stream()
            .map(JpaDvEntity::toDomainObject)
            .allMatch(DvRecord::meetsDataQuality);
    }
    
    /**
     * Get DV records for HMIS CSV export (with privacy considerations)
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getDvRecordsForHmisExport(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDvEntity> entities = dvRepository
            .findForHmisExport(startDate, endDate);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get latest DV record per enrollment for reporting
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getLatestDvRecordPerEnrollment(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDvEntity> entities = dvRepository
            .findLatestRecordPerEnrollment(startDate, endDate);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Count high-risk DV cases for reporting
     */
    @Transactional(readOnly = true)
    public Long countHighRiskCases(LocalDate startDate, LocalDate endDate) {
        return dvRepository.countHighRiskCases(startDate, endDate);
    }
    
    /**
     * Get correction records for a specific original record
     */
    @Transactional(readOnly = true)
    public List<DvRecord> getCorrectionRecords(UUID originalRecordId) {
        List<JpaDvEntity> entities = dvRepository
            .findByCorrectsRecordIdOrderByCreatedAtDesc(originalRecordId);
        
        return entities.stream()
            .map(JpaDvEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get high-risk clients requiring immediate safety intervention
     */
    @Transactional(readOnly = true)
    public List<UUID> getHighRiskClientsRequiringIntervention() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate today = LocalDate.now();
        
        List<JpaDvEntity> highRiskRecords = dvRepository.findRecordsCurrentlyFleeingDv(thirtyDaysAgo, today);
        
        return highRiskRecords.stream()
            .map(entity -> entity.toDomainObject().getClientId().value())
            .distinct()
            .toList();
    }
    
    /**
     * Get enrollments requiring DV safety protocol activation
     */
    @Transactional(readOnly = true)
    public List<UUID> getEnrollmentsRequiringSafetyProtocols() {
        return enrollmentRepository.findAll().stream()
            .map(JpaProgramEnrollmentEntity::toDomainObject)
            .filter(ProgramEnrollment::requiresEnhancedSafety)
            .map(enrollment -> enrollment.getId().value())
            .toList();
    }
    
    /**
     * Create safety alert for enrollment requiring enhanced protocols
     */
    public void createSafetyAlert(UUID enrollmentId, String alertDetails, String createdBy) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        if (!enrollment.requiresEnhancedSafety()) {
            throw new IllegalStateException("Enrollment does not require enhanced safety protocols");
        }
        
        // Create safety alert (could be expanded to include alert domain model)
        // For now, this could trigger notifications or flag records for enhanced security
        
        // Update enrollment with safety alert flag if needed
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
}