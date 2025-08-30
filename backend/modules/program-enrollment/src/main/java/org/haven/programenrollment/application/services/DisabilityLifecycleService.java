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
 * Application Service for HMIS-compliant Disability lifecycle management
 * Handles all 6 disability types per HMIS FY2024 UDE 3.09-3.13
 * Manages automatic derivation of disabling conditions when configured
 */
@Service
@Transactional
public class DisabilityLifecycleService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaDisabilityRepository disabilityRepository;
    
    public DisabilityLifecycleService(
            JpaProgramEnrollmentRepository enrollmentRepository,
            JpaDisabilityRepository disabilityRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.disabilityRepository = disabilityRepository;
    }
    
    /**
     * Create required PROJECT_START disability record when client enrolls
     */
    public DisabilityRecord createProjectStartRecord(
            UUID enrollmentId,
            DisabilityKind disabilityKind,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create PROJECT_START record through aggregate
        enrollment.createStartDisabilityRecord(disabilityKind, hasDisability, collectedBy);
        
        // Get the created record
        DisabilityRecord startRecord = enrollment.getDisabilityRecord(DataCollectionStage.PROJECT_START, disabilityKind);
        
        // Persist the record
        JpaDisabilityEntity disabilityEntity = new JpaDisabilityEntity(startRecord);
        disabilityRepository.save(disabilityEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return startRecord;
    }
    
    /**
     * Create disability update record due to change in circumstances
     */
    public DisabilityRecord createUpdateRecord(
            UUID enrollmentId,
            DisabilityKind disabilityKind,
            LocalDate changeDate,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create UPDATE record
        enrollment.createDisabilityUpdateRecord(disabilityKind, changeDate, hasDisability, collectedBy);
        
        // Get the most recent record (should be the update we just created)
        DisabilityRecord updateRecord = enrollment.getMostRecentDisabilityRecord(disabilityKind);
        
        // Persist the record
        JpaDisabilityEntity disabilityEntity = new JpaDisabilityEntity(updateRecord);
        disabilityRepository.save(disabilityEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return updateRecord;
    }
    
    /**
     * Create required PROJECT_EXIT disability record when client exits
     */
    public DisabilityRecord createProjectExitRecord(
            UUID enrollmentId,
            DisabilityKind disabilityKind,
            LocalDate exitDate,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create PROJECT_EXIT record
        enrollment.createExitDisabilityRecord(disabilityKind, exitDate, hasDisability, collectedBy);
        
        // Get the exit record
        DisabilityRecord exitRecord = enrollment.getDisabilityRecord(DataCollectionStage.PROJECT_EXIT, disabilityKind);
        
        // Persist the record
        JpaDisabilityEntity disabilityEntity = new JpaDisabilityEntity(exitRecord);
        disabilityRepository.save(disabilityEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return exitRecord;
    }
    
    /**
     * Create correction record for an existing disability record
     */
    public DisabilityRecord createCorrectionRecord(
            UUID originalRecordId,
            HmisFivePoint hasDisability,
            String collectedBy) {
        
        JpaDisabilityEntity originalEntity = disabilityRepository.findById(originalRecordId)
            .orElseThrow(() -> new IllegalArgumentException("Disability record not found: " + originalRecordId));
        
        DisabilityRecord originalRecord = originalEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(originalRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + originalRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create correction record
        enrollment.correctDisabilityRecord(originalRecord, hasDisability, collectedBy);
        
        // Get the correction record (should be the most recent one)
        DisabilityRecord correctionRecord = enrollment.getDisabilityRecords(originalRecord.getDisabilityKind()).stream()
            .filter(record -> record.isCorrection() && record.getCorrectsRecordId().equals(originalRecordId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Correction record not found after creation"));
        
        // Persist the correction record
        JpaDisabilityEntity correctionEntity = new JpaDisabilityEntity(correctionRecord);
        disabilityRepository.save(correctionEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return correctionRecord;
    }
    
    /**
     * Get all disability records for an enrollment
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getDisabilityRecordsForEnrollment(UUID enrollmentId) {
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get all disability records for a specific disability kind
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getDisabilityRecordsForEnrollment(UUID enrollmentId, DisabilityKind disabilityKind) {
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findByEnrollmentIdAndDisabilityKindOrderByInformationDateDesc(enrollmentId, disabilityKind.name());
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get the most recent disability record for a specific kind
     */
    @Transactional(readOnly = true)
    public DisabilityRecord getMostRecentDisabilityRecord(UUID enrollmentId, DisabilityKind disabilityKind) {
        return disabilityRepository.findFirstByEnrollmentIdAndDisabilityKindOrderByInformationDateDesc(
                enrollmentId, disabilityKind.name())
            .map(JpaDisabilityEntity::toDomainObject)
            .orElse(null);
    }
    
    /**
     * Get latest effective disability record (accounting for corrections)
     */
    @Transactional(readOnly = true)
    public DisabilityRecord getLatestEffectiveDisabilityRecord(UUID enrollmentId, DisabilityKind disabilityKind) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getLatestEffectiveDisabilityRecord(disabilityKind);
    }
    
    /**
     * Get all current disabling conditions for an enrollment
     */
    @Transactional(readOnly = true)
    public List<DisabilityKind> getCurrentDisablingConditions(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getCurrentDisablingConditions();
    }
    
    /**
     * Check if enrollment has any disabling conditions
     */
    @Transactional(readOnly = true)
    public boolean hasAnyDisablingCondition(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.hasAnyDisablingCondition();
    }
    
    /**
     * Check if enrollment has behavioral health disabilities (mental health or substance use)
     */
    @Transactional(readOnly = true)
    public boolean hasBehavioralHealthDisabilities(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.hasBehavioralHealthDisabilities();
    }
    
    /**
     * Check if enrollment has medical disabilities (non-behavioral health)
     */
    @Transactional(readOnly = true)
    public boolean hasMedicalDisabilities(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.hasMedicalDisabilities();
    }
    
    /**
     * Find enrollments missing PROJECT_START disability records for a specific kind
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingProjectStartRecord(DisabilityKind disabilityKind) {
        return disabilityRepository.findEnrollmentsMissingProjectStartRecord(disabilityKind.name());
    }
    
    /**
     * Find enrollments that exited but don't have PROJECT_EXIT records for a specific kind
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingProjectExitRecord(DisabilityKind disabilityKind) {
        return disabilityRepository.findEnrollmentsMissingProjectExitRecord(disabilityKind.name());
    }
    
    /**
     * Find records with data quality issues that may need correction
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> findRecordsWithDataQualityIssues() {
        List<JpaDisabilityEntity> entities = disabilityRepository.findRecordsWithDataQualityIssues();
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Check if enrollment has required PROJECT_START record for a disability kind
     */
    @Transactional(readOnly = true)
    public boolean hasProjectStartRecord(UUID enrollmentId, DisabilityKind disabilityKind) {
        return disabilityRepository.existsByEnrollmentIdAndDisabilityKindAndStage(
            enrollmentId, disabilityKind.name(), "PROJECT_START");
    }
    
    /**
     * Check if enrollment has PROJECT_EXIT record for a disability kind
     */
    @Transactional(readOnly = true)
    public boolean hasProjectExitRecord(UUID enrollmentId, DisabilityKind disabilityKind) {
        return disabilityRepository.existsByEnrollmentIdAndDisabilityKindAndStage(
            enrollmentId, disabilityKind.name(), "PROJECT_EXIT");
    }
    
    /**
     * Check HMIS compliance for all disability data collection
     */
    @Transactional(readOnly = true)
    public boolean meetsDisabilityDataCompliance(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElse(null);
        
        if (enrollmentEntity == null) {
            return false;
        }
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Check all 6 disability kinds for required records
        for (DisabilityKind kind : DisabilityKind.values()) {
            // Must have PROJECT_START record
            if (!hasProjectStartRecord(enrollmentId, kind)) {
                return false;
            }
            
            // Check if needs PROJECT_EXIT record
            if (enrollment.hasExited() && !hasProjectExitRecord(enrollmentId, kind)) {
                return false;
            }
        }
        
        // Check data quality of all records
        List<JpaDisabilityEntity> records = disabilityRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return records.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .allMatch(DisabilityRecord::meetsDataQuality);
    }
    
    /**
     * Get disability records for HMIS CSV export
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getDisabilityRecordsForHmisExport(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findForHmisExport(startDate, endDate);
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get latest disability record per enrollment for reporting
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getLatestDisabilityRecordPerEnrollment(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findLatestRecordPerEnrollment(startDate, endDate);
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get records indicating disabling conditions for a specific kind
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getRecordsWithDisablingConditions(
            DisabilityKind disabilityKind,
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findRecordsWithDisablingConditions(disabilityKind.name(), startDate, endDate);
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get behavioral health disability records
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getBehavioralHealthDisabilityRecords(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findBehavioralHealthDisabilityRecords(startDate, endDate);
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Count records with disabling conditions for reporting
     */
    @Transactional(readOnly = true)
    public Long countRecordsWithDisablingConditions(
            DisabilityKind disabilityKind, 
            LocalDate startDate, 
            LocalDate endDate) {
        return disabilityRepository.countRecordsWithDisablingConditions(
            disabilityKind.name(), startDate, endDate);
    }
    
    /**
     * Count behavioral health disability records
     */
    @Transactional(readOnly = true)
    public Long countBehavioralHealthDisabilityRecords(LocalDate startDate, LocalDate endDate) {
        return disabilityRepository.countBehavioralHealthDisabilityRecords(startDate, endDate);
    }
    
    /**
     * Get correction records for a specific original record
     */
    @Transactional(readOnly = true)
    public List<DisabilityRecord> getCorrectionRecords(UUID originalRecordId) {
        List<JpaDisabilityEntity> entities = disabilityRepository
            .findByCorrectsRecordIdOrderByCreatedAtDesc(originalRecordId);
        
        return entities.stream()
            .map(JpaDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Bulk create all PROJECT_START disability records for enrollment
     */
    public void createAllProjectStartRecords(
            UUID enrollmentId,
            HmisFivePoint physicalDisability,
            HmisFivePoint developmentalDisability,
            HmisFivePoint chronicHealthCondition,
            HmisFivePoint hivAids,
            HmisFivePoint mentalHealthDisorder,
            HmisFivePoint substanceUseDisorder,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create all 6 disability records
        enrollment.createAllStartDisabilityRecords(
            physicalDisability, developmentalDisability, chronicHealthCondition,
            hivAids, mentalHealthDisorder, substanceUseDisorder, collectedBy);
        
        // Persist all records
        for (DisabilityKind kind : DisabilityKind.values()) {
            DisabilityRecord record = enrollment.getDisabilityRecord(DataCollectionStage.PROJECT_START, kind);
            if (record != null) {
                JpaDisabilityEntity entity = new JpaDisabilityEntity(record);
                disabilityRepository.save(entity);
            }
        }
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
    
    /**
     * Bulk create all PROJECT_EXIT disability records for enrollment
     */
    public void createAllProjectExitRecords(
            UUID enrollmentId,
            LocalDate exitDate,
            HmisFivePoint physicalDisability,
            HmisFivePoint developmentalDisability,
            HmisFivePoint chronicHealthCondition,
            HmisFivePoint hivAids,
            HmisFivePoint mentalHealthDisorder,
            HmisFivePoint substanceUseDisorder,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create all 6 exit disability records
        enrollment.createAllExitDisabilityRecords(
            exitDate, physicalDisability, developmentalDisability, chronicHealthCondition,
            hivAids, mentalHealthDisorder, substanceUseDisorder, collectedBy);
        
        // Persist all records
        for (DisabilityKind kind : DisabilityKind.values()) {
            DisabilityRecord record = enrollment.getDisabilityRecord(DataCollectionStage.PROJECT_EXIT, kind);
            if (record != null) {
                JpaDisabilityEntity entity = new JpaDisabilityEntity(record);
                disabilityRepository.save(entity);
            }
        }
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
}