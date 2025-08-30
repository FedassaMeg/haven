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
 * Application Service for HMIS-compliant Physical Disability lifecycle management
 * Handles UDE 3.08 Physical Disability data collection per HMIS FY2024 standards
 * Manages automatic derivation of disabling conditions when configured
 */
@Service
@Transactional
public class PhysicalDisabilityLifecycleService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaPhysicalDisabilityRepository physicalDisabilityRepository;
    
    public PhysicalDisabilityLifecycleService(
            JpaProgramEnrollmentRepository enrollmentRepository,
            JpaPhysicalDisabilityRepository physicalDisabilityRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.physicalDisabilityRepository = physicalDisabilityRepository;
    }
    
    /**
     * Create required PROJECT_START physical disability record when client enrolls
     */
    public PhysicalDisabilityRecord createProjectStartRecord(
            UUID enrollmentId,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create PROJECT_START record through aggregate
        enrollment.createStartPhysicalDisabilityRecord(physicalDisability, collectedBy);
        
        // Get the created record
        PhysicalDisabilityRecord startRecord = enrollment.getPhysicalDisabilityRecord(DataCollectionStage.PROJECT_START);
        
        // Persist the record
        JpaPhysicalDisabilityEntity disabilityEntity = new JpaPhysicalDisabilityEntity(startRecord);
        physicalDisabilityRepository.save(disabilityEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return startRecord;
    }
    
    /**
     * Create physical disability update record due to change in circumstances
     */
    public PhysicalDisabilityRecord createUpdateRecord(
            UUID enrollmentId,
            LocalDate changeDate,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create UPDATE record
        enrollment.createPhysicalDisabilityUpdateRecord(changeDate, physicalDisability, collectedBy);
        
        // Get the most recent record (should be the update we just created)
        PhysicalDisabilityRecord updateRecord = enrollment.getMostRecentPhysicalDisabilityRecord();
        
        // Persist the record
        JpaPhysicalDisabilityEntity disabilityEntity = new JpaPhysicalDisabilityEntity(updateRecord);
        physicalDisabilityRepository.save(disabilityEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return updateRecord;
    }
    
    /**
     * Create required PROJECT_EXIT physical disability record when client exits
     */
    public PhysicalDisabilityRecord createProjectExitRecord(
            UUID enrollmentId,
            LocalDate exitDate,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create PROJECT_EXIT record
        enrollment.createExitPhysicalDisabilityRecord(exitDate, physicalDisability, collectedBy);
        
        // Get the exit record
        PhysicalDisabilityRecord exitRecord = enrollment.getPhysicalDisabilityRecord(DataCollectionStage.PROJECT_EXIT);
        
        // Persist the record
        JpaPhysicalDisabilityEntity disabilityEntity = new JpaPhysicalDisabilityEntity(exitRecord);
        physicalDisabilityRepository.save(disabilityEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return exitRecord;
    }
    
    /**
     * Create correction record for an existing physical disability record
     */
    public PhysicalDisabilityRecord createCorrectionRecord(
            UUID originalRecordId,
            HmisFivePointResponse physicalDisability,
            String collectedBy) {
        
        JpaPhysicalDisabilityEntity originalEntity = physicalDisabilityRepository.findById(originalRecordId)
            .orElseThrow(() -> new IllegalArgumentException("Physical disability record not found: " + originalRecordId));
        
        PhysicalDisabilityRecord originalRecord = originalEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(originalRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + originalRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create correction record
        enrollment.correctPhysicalDisabilityRecord(originalRecord, physicalDisability, collectedBy);
        
        // Get the correction record (should be the most recent one)
        PhysicalDisabilityRecord correctionRecord = enrollment.getPhysicalDisabilityRecords().stream()
            .filter(record -> record.isCorrection() && record.getCorrectsRecordId().equals(originalRecordId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Correction record not found after creation"));
        
        // Persist the correction record
        JpaPhysicalDisabilityEntity correctionEntity = new JpaPhysicalDisabilityEntity(correctionRecord);
        physicalDisabilityRepository.save(correctionEntity);
        
        // Update enrollment if disabling condition was derived
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return correctionRecord;
    }
    
    /**
     * Update physical expected long-term response for a record
     */
    public void updatePhysicalExpectedLongTerm(
            UUID recordId,
            HmisFivePointResponse expectedLongTerm) {
        
        JpaPhysicalDisabilityEntity disabilityEntity = physicalDisabilityRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Physical disability record not found: " + recordId));
        
        PhysicalDisabilityRecord disabilityRecord = disabilityEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(disabilityRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + disabilityRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Update the expected long-term response
        enrollment.updatePhysicalExpectedLongTerm(disabilityRecord, expectedLongTerm);
        
        // Save updated entities
        JpaPhysicalDisabilityEntity updatedEntity = new JpaPhysicalDisabilityEntity(disabilityRecord);
        physicalDisabilityRepository.save(updatedEntity);
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
    
    /**
     * Update both physical disability responses for a record
     */
    public void updatePhysicalDisabilityStatus(
            UUID recordId,
            HmisFivePointResponse physicalDisability,
            HmisFivePointResponse expectedLongTerm) {
        
        JpaPhysicalDisabilityEntity disabilityEntity = physicalDisabilityRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Physical disability record not found: " + recordId));
        
        PhysicalDisabilityRecord disabilityRecord = disabilityEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(disabilityRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + disabilityRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Update both responses
        enrollment.updatePhysicalDisabilityStatus(disabilityRecord, physicalDisability, expectedLongTerm);
        
        // Save updated entities
        JpaPhysicalDisabilityEntity updatedEntity = new JpaPhysicalDisabilityEntity(disabilityRecord);
        physicalDisabilityRepository.save(updatedEntity);
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
    
    /**
     * Get all physical disability records for an enrollment
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> getPhysicalDisabilityRecordsForEnrollment(UUID enrollmentId) {
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get the most recent physical disability record for an enrollment
     */
    @Transactional(readOnly = true)
    public PhysicalDisabilityRecord getMostRecentPhysicalDisabilityRecord(UUID enrollmentId) {
        return physicalDisabilityRepository.findFirstByEnrollmentIdOrderByInformationDateDesc(enrollmentId)
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .orElse(null);
    }
    
    /**
     * Get latest effective physical disability record (accounting for corrections)
     */
    @Transactional(readOnly = true)
    public PhysicalDisabilityRecord getLatestEffectivePhysicalDisabilityRecord(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getLatestEffectivePhysicalDisabilityRecord();
    }
    
    /**
     * Find enrollments missing PROJECT_START physical disability records
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingProjectStartRecord() {
        return physicalDisabilityRepository.findEnrollmentsMissingProjectStartRecord();
    }
    
    /**
     * Find enrollments that exited but don't have PROJECT_EXIT records
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingProjectExitRecord() {
        return physicalDisabilityRepository.findEnrollmentsMissingProjectExitRecord();
    }
    
    /**
     * Find records with data quality issues that may need correction
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> findRecordsWithDataQualityIssues() {
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository.findRecordsWithDataQualityIssues();
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Check if enrollment has required PROJECT_START record
     */
    @Transactional(readOnly = true)
    public boolean hasProjectStartRecord(UUID enrollmentId) {
        return physicalDisabilityRepository.existsByEnrollmentIdAndStage(enrollmentId, "PROJECT_START");
    }
    
    /**
     * Check if enrollment has PROJECT_EXIT record
     */
    @Transactional(readOnly = true)
    public boolean hasProjectExitRecord(UUID enrollmentId) {
        return physicalDisabilityRepository.existsByEnrollmentIdAndStage(enrollmentId, "PROJECT_EXIT");
    }
    
    /**
     * Check HMIS compliance for physical disability data collection
     */
    @Transactional(readOnly = true)
    public boolean meetsPhysicalDisabilityDataCompliance(UUID enrollmentId) {
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
        List<JpaPhysicalDisabilityEntity> records = physicalDisabilityRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return records.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .allMatch(PhysicalDisabilityRecord::meetsDataQuality);
    }
    
    /**
     * Get physical disability records for HMIS CSV export
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> getPhysicalDisabilityRecordsForHmisExport(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository
            .findForHmisExport(startDate, endDate);
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get latest physical disability record per enrollment for reporting
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> getLatestPhysicalDisabilityRecordPerEnrollment(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository
            .findLatestRecordPerEnrollment(startDate, endDate);
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get records indicating disabling conditions (physical_disability=YES AND physical_expected_long_term=YES)
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> getRecordsWithDisablingConditions(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository
            .findRecordsWithDisablingConditions(startDate, endDate);
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get records with physical disabilities (regardless of long-term expectation)
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> getRecordsWithPhysicalDisabilities(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository
            .findRecordsWithPhysicalDisabilities(startDate, endDate);
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Count records with disabling conditions for reporting
     */
    @Transactional(readOnly = true)
    public Long countRecordsWithDisablingConditions(LocalDate startDate, LocalDate endDate) {
        return physicalDisabilityRepository.countRecordsWithDisablingConditions(startDate, endDate);
    }
    
    /**
     * Get correction records for a specific original record
     */
    @Transactional(readOnly = true)
    public List<PhysicalDisabilityRecord> getCorrectionRecords(UUID originalRecordId) {
        List<JpaPhysicalDisabilityEntity> entities = physicalDisabilityRepository
            .findByCorrectsRecordIdOrderByCreatedAtDesc(originalRecordId);
        
        return entities.stream()
            .map(JpaPhysicalDisabilityEntity::toDomainObject)
            .toList();
    }
}