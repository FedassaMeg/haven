package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application Service for HMIS-compliant Date of Engagement management
 * Handles engagement date tracking with correction support
 * Critical for program effectiveness measurement and coordinated entry
 */
@Service
@Transactional
public class DateOfEngagementService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaDateOfEngagementRepository engagementRepository;
    
    public DateOfEngagementService(
            @Lazy JpaProgramEnrollmentRepository enrollmentRepository,
            @Lazy JpaDateOfEngagementRepository engagementRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.engagementRepository = engagementRepository;
    }
    
    /**
     * Set initial date of engagement for enrollment
     */
    public DateOfEngagement setDateOfEngagement(
            UUID enrollmentId,
            LocalDate engagementDate,
            String createdBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Set engagement date through aggregate
        enrollment.setDateOfEngagement(engagementDate, createdBy);
        
        // Get the created record
        DateOfEngagement engagementRecord = enrollment.getDateOfEngagement();
        
        // Persist the record
        JpaDateOfEngagementEntity engagementEntity = new JpaDateOfEngagementEntity(engagementRecord);
        engagementRepository.save(engagementEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return engagementRecord;
    }
    
    /**
     * Correct existing date of engagement
     */
    public DateOfEngagement correctDateOfEngagement(
            UUID enrollmentId,
            LocalDate newEngagementDate,
            String createdBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Correct engagement date through aggregate
        enrollment.correctDateOfEngagement(newEngagementDate, createdBy);
        
        // Get the correction record (most recent)
        DateOfEngagement correctionRecord = enrollment.getDateOfEngagement();
        
        // Persist the correction record
        JpaDateOfEngagementEntity correctionEntity = new JpaDateOfEngagementEntity(correctionRecord);
        engagementRepository.save(correctionEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return correctionRecord;
    }
    
    /**
     * Get current effective date of engagement for enrollment
     */
    @Transactional(readOnly = true)
    public DateOfEngagement getDateOfEngagement(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getDateOfEngagement();
    }
    
    /**
     * Get the effective engagement date (LocalDate only)
     */
    @Transactional(readOnly = true)
    public LocalDate getEffectiveEngagementDate(UUID enrollmentId) {
        DateOfEngagement engagement = getDateOfEngagement(enrollmentId);
        return engagement != null ? engagement.getEngagementDate() : null;
    }
    
    /**
     * Check if enrollment has date of engagement set
     */
    @Transactional(readOnly = true)
    public boolean hasDateOfEngagement(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.hasDateOfEngagement();
    }
    
    /**
     * Get all engagement records for enrollment (including corrections)
     */
    @Transactional(readOnly = true)
    public List<DateOfEngagement> getAllEngagementRecordsForEnrollment(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getAllDateOfEngagementRecords();
    }
    
    /**
     * Get all engagement records for a client across enrollments
     */
    @Transactional(readOnly = true)
    public List<DateOfEngagement> getEngagementRecordsForClient(UUID clientId) {
        List<JpaDateOfEngagementEntity> entities = engagementRepository
            .findByClientIdOrderByEngagementDateDesc(clientId);
        
        return entities.stream()
            .map(JpaDateOfEngagementEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get engagement records within date range
     */
    @Transactional(readOnly = true)
    public List<DateOfEngagement> getEngagementRecordsInDateRange(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDateOfEngagementEntity> entities = engagementRepository
            .findByEngagementDateBetween(startDate, endDate);
        
        return entities.stream()
            .map(JpaDateOfEngagementEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Find enrollments missing date of engagement
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingEngagementDate() {
        return engagementRepository.findEnrollmentsMissingEngagementDate();
    }
    
    /**
     * Get correction records for a specific original record
     */
    @Transactional(readOnly = true)
    public List<DateOfEngagement> getCorrectionRecords(UUID originalRecordId) {
        List<JpaDateOfEngagementEntity> entities = engagementRepository
            .findByCorrectsRecordIdOrderByCreatedAtDesc(originalRecordId);
        
        return entities.stream()
            .map(JpaDateOfEngagementEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Calculate engagement metrics for program effectiveness
     */
    @Transactional(readOnly = true)
    public EngagementMetrics calculateEngagementMetrics(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDateOfEngagementEntity> engagementRecords = engagementRepository
            .findByEngagementDateBetween(startDate, endDate);
        
        List<JpaProgramEnrollmentEntity> allEnrollments = enrollmentRepository
            .findAll(); // Could be optimized with date range query
        
        int totalEnrollments = allEnrollments.size();
        int enrollmentsWithEngagement = engagementRecords.size();
        double engagementRate = totalEnrollments > 0 ? 
            (double) enrollmentsWithEngagement / totalEnrollments : 0.0;
        
        // Calculate average days to engagement
        double averageDaysToEngagement = engagementRecords.stream()
            .mapToLong(entity -> {
                DateOfEngagement engagement = entity.toDomainObject();
                ProgramEnrollmentId enrollmentId = engagement.getEnrollmentId();
                
                // Find corresponding enrollment to get entry date
                return allEnrollments.stream()
                    .filter(e -> e.toDomainObject().getId().equals(enrollmentId))
                    .findFirst()
                    .map(e -> java.time.temporal.ChronoUnit.DAYS.between(
                        e.toDomainObject().getEntryDate(), 
                        engagement.getEngagementDate()))
                    .orElse(0L);
            })
            .average()
            .orElse(0.0);
        
        return new EngagementMetrics(
            totalEnrollments,
            enrollmentsWithEngagement,
            engagementRate,
            averageDaysToEngagement,
            startDate,
            endDate
        );
    }
    
    /**
     * Check engagement compliance for enrollment
     */
    @Transactional(readOnly = true)
    public boolean meetsEngagementCompliance(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElse(null);
        
        if (enrollmentEntity == null) {
            return false;
        }
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Basic compliance: must have engagement date if enrollment is active for more than X days
        // This could be configurable per project type
        if (!enrollment.hasDateOfEngagement()) {
            LocalDate cutoffDate = enrollment.getEntryDate().plusDays(30); // 30 days grace period
            return LocalDate.now().isBefore(cutoffDate);
        }
        
        // Engagement date must be after entry date
        DateOfEngagement engagement = enrollment.getDateOfEngagement();
        return !engagement.getEngagementDate().isBefore(enrollment.getEntryDate());
    }
    
    /**
     * Get rapid re-housing engagement analysis
     */
    @Transactional(readOnly = true)
    public RapidReHousingEngagementAnalysis getRapidReHousingEngagementAnalysis(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaDateOfEngagementEntity> engagementRecords = engagementRepository
            .findByEngagementDateBetween(startDate, endDate);
        
        // Filter for rapid re-housing enrollments and analyze engagement patterns
        List<EngagementTiming> engagementTimings = engagementRecords.stream()
            .map(entity -> {
                DateOfEngagement engagement = entity.toDomainObject();
                ProgramEnrollmentId enrollmentId = engagement.getEnrollmentId();
                
                // Get enrollment to calculate days to engagement
                JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository
                    .findById(enrollmentId.value())
                    .orElse(null);
                
                if (enrollmentEntity != null) {
                    ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
                    long daysToEngagement = java.time.temporal.ChronoUnit.DAYS.between(
                        enrollment.getEntryDate(), 
                        engagement.getEngagementDate());
                    
                    return new EngagementTiming(
                        enrollmentId.value(),
                        enrollment.getEntryDate(),
                        engagement.getEngagementDate(),
                        daysToEngagement
                    );
                }
                return null;
            })
            .filter(timing -> timing != null)
            .toList();
        
        // Categorize engagement timing
        long rapidEngagement = engagementTimings.stream()
            .filter(timing -> timing.daysToEngagement() <= 7)
            .count();
        
        long standardEngagement = engagementTimings.stream()
            .filter(timing -> timing.daysToEngagement() > 7 && timing.daysToEngagement() <= 30)
            .count();
        
        long delayedEngagement = engagementTimings.stream()
            .filter(timing -> timing.daysToEngagement() > 30)
            .count();
        
        return new RapidReHousingEngagementAnalysis(
            engagementTimings.size(),
            rapidEngagement,
            standardEngagement,
            delayedEngagement,
            startDate,
            endDate
        );
    }
    
    /**
     * Delete engagement record (use with caution - prefer corrections)
     */
    public void deleteDateOfEngagement(UUID recordId) {
        JpaDateOfEngagementEntity engagementEntity = engagementRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Engagement record not found: " + recordId));
        
        DateOfEngagement engagementRecord = engagementEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(
            engagementRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + 
                engagementRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Remove from aggregate
        enrollment.removeDateOfEngagement(engagementRecord);
        
        // Delete from repository
        engagementRepository.delete(engagementEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
    
    /**
     * Value object for engagement metrics
     */
    public record EngagementMetrics(
        int totalEnrollments,
        int enrollmentsWithEngagement,
        double engagementRate,
        double averageDaysToEngagement,
        LocalDate startDate,
        LocalDate endDate
    ) {}
    
    /**
     * Value object for engagement timing analysis
     */
    public record EngagementTiming(
        UUID enrollmentId,
        LocalDate entryDate,
        LocalDate engagementDate,
        long daysToEngagement
    ) {}
    
    /**
     * Value object for rapid re-housing engagement analysis
     */
    public record RapidReHousingEngagementAnalysis(
        int totalEngagements,
        long rapidEngagement,      // <= 7 days
        long standardEngagement,   // 8-30 days  
        long delayedEngagement,    // > 30 days
        LocalDate startDate,
        LocalDate endDate
    ) {}
}
