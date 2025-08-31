package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.hmis.PriorLivingSituation;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application Service for HMIS-compliant Current Living Situation management
 * Handles street outreach contact tracking and living situation assessment
 * Critical for coordinated entry and unsheltered client engagement
 */
@Service
@Transactional
public class CurrentLivingSituationService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaCurrentLivingSituationRepository clsRepository;
    
    public CurrentLivingSituationService(
            @Lazy JpaProgramEnrollmentRepository enrollmentRepository,
            @Lazy JpaCurrentLivingSituationRepository clsRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.clsRepository = clsRepository;
    }
    
    /**
     * Record current living situation contact
     */
    public CurrentLivingSituation recordCurrentLivingSituation(
            UUID enrollmentId,
            LocalDate contactDate,
            PriorLivingSituation livingSituation,
            Integer lengthOfStayDays,
            String lengthOfStayAtTimeOfContact,
            Boolean verifiedBy,
            String createdBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Record CLS through aggregate - map service parameters to domain method
        String locationDescription = lengthOfStayAtTimeOfContact; // TODO: Better mapping
        String verifiedByString = verifiedBy != null && verifiedBy ? "VERIFIED" : null;
        
        enrollment.recordCurrentLivingSituation(
            contactDate, livingSituation, locationDescription, 
            verifiedByString, createdBy);
        
        // Get the created record (most recent)
        CurrentLivingSituation clsRecord = enrollment.getMostRecentCurrentLivingSituation();
        
        // Persist the record
        JpaCurrentLivingSituationEntity clsEntity = new JpaCurrentLivingSituationEntity(clsRecord);
        clsRepository.save(clsEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return clsRecord;
    }
    
    /**
     * Update living situation contact with additional details
     */
    public CurrentLivingSituation updateCurrentLivingSituation(
            UUID recordId,
            PriorLivingSituation livingSituation,
            Integer lengthOfStayDays,
            String lengthOfStayAtTimeOfContact,
            Boolean verifiedBy) {
        
        JpaCurrentLivingSituationEntity clsEntity = clsRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("CLS record not found: " + recordId));
        
        CurrentLivingSituation clsRecord = clsEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(clsRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + clsRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Update the clsRecord with new values first
        // TODO: Add proper update methods to CurrentLivingSituation
        // For now, just pass the record as-is
        enrollment.updateCurrentLivingSituation(clsRecord);
        
        // Save updated entities
        JpaCurrentLivingSituationEntity updatedEntity = new JpaCurrentLivingSituationEntity(clsRecord);
        clsRepository.save(updatedEntity);
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
        
        return clsRecord;
    }
    
    /**
     * Get all CLS records for an enrollment
     */
    @Transactional(readOnly = true)
    public List<CurrentLivingSituation> getCurrentLivingSituationsForEnrollment(UUID enrollmentId) {
        List<JpaCurrentLivingSituationEntity> entities = clsRepository
            .findByEnrollmentIdOrderByContactDateDesc(enrollmentId);
        
        return entities.stream()
            .map(JpaCurrentLivingSituationEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get the most recent CLS record for an enrollment
     */
    @Transactional(readOnly = true)
    public CurrentLivingSituation getMostRecentCurrentLivingSituation(UUID enrollmentId) {
        return clsRepository.findFirstByEnrollmentIdOrderByContactDateDesc(enrollmentId)
            .map(JpaCurrentLivingSituationEntity::toDomainObject)
            .orElse(null);
    }
    
    /**
     * Get all CLS records for a client across enrollments
     */
    @Transactional(readOnly = true)
    public List<CurrentLivingSituation> getCurrentLivingSituationsForClient(UUID clientId) {
        List<JpaCurrentLivingSituationEntity> entities = clsRepository
            .findByClientIdOrderByContactDateDesc(clientId);
        
        return entities.stream()
            .map(JpaCurrentLivingSituationEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get CLS records within date range
     */
    @Transactional(readOnly = true)
    public List<CurrentLivingSituation> getCurrentLivingSituationsInDateRange(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaCurrentLivingSituationEntity> entities = clsRepository
            .findByContactDateBetween(startDate, endDate);
        
        return entities.stream()
            .map(JpaCurrentLivingSituationEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get CLS records for enrollment within date range
     */
    @Transactional(readOnly = true)
    public List<CurrentLivingSituation> getCurrentLivingSituationsForEnrollmentInDateRange(
            UUID enrollmentId,
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaCurrentLivingSituationEntity> entities = clsRepository
            .findByEnrollmentIdAndContactDateBetween(enrollmentId, startDate, endDate);
        
        return entities.stream()
            .map(JpaCurrentLivingSituationEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get unsheltered contacts (place not meant for habitation)
     */
    @Transactional(readOnly = true)
    public List<CurrentLivingSituation> getUnshelteredContacts(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaCurrentLivingSituationEntity> entities = clsRepository
            .findUnshelteredContacts(startDate, endDate);
        
        return entities.stream()
            .map(JpaCurrentLivingSituationEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get contact counts by living situation type
     */
    @Transactional(readOnly = true)
    public List<LivingSituationSummary> getContactCountsByLivingSituation(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<Object[]> results = clsRepository.countContactsByLivingSituation(startDate, endDate);
        
        return results.stream()
            .map(row -> new LivingSituationSummary(
                PriorLivingSituation.valueOf((String) row[0]),
                ((Long) row[1]).intValue()
            ))
            .toList();
    }
    
    /**
     * Check if enrollment has recent street outreach contact
     */
    @Transactional(readOnly = true)
    public boolean hasRecentStreetContact(UUID enrollmentId, int daysBack) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysBack);
        LocalDate today = LocalDate.now();
        
        List<JpaCurrentLivingSituationEntity> recentContacts = clsRepository
            .findByEnrollmentIdAndContactDateBetween(enrollmentId, cutoffDate, today);
        
        return !recentContacts.isEmpty();
    }
    
    /**
     * Check if client is currently unsheltered based on most recent contact
     */
    @Transactional(readOnly = true)
    public boolean isCurrentlyUnsheltered(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.isCurrentlyUnsheltered();
    }
    
    /**
     * Get current living situation status for enrollment
     */
    @Transactional(readOnly = true)
    public CurrentLivingSituationStatus getCurrentLivingSituationStatus(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getCurrentLivingSituationStatus();
    }
    
    /**
     * Get chronically homeless determination based on CLS history
     */
    @Transactional(readOnly = true)
    public ChronicallyHomelessDetermination getChronicallyHomelessDetermination(UUID enrollmentId) {
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        return enrollment.getChronicallyHomelessDetermination();
    }
    
    /**
     * Find enrollments with frequent unsheltered contacts (high engagement need)
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsWithFrequentUnshelteredContacts(int daysBack, int minContacts) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysBack);
        LocalDate today = LocalDate.now();
        
        List<JpaCurrentLivingSituationEntity> unshelteredContacts = clsRepository
            .findUnshelteredContacts(cutoffDate, today);
        
        return unshelteredContacts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                entity -> entity.toDomainObject().getEnrollmentId().value(),
                java.util.stream.Collectors.counting()
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue() >= minContacts)
            .map(java.util.Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Find enrollments missing recent contact (engagement gaps)
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsMissingRecentContact(int daysWithoutContact) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysWithoutContact);
        
        return enrollmentRepository.findAll().stream()
            .map(JpaProgramEnrollmentEntity::toDomainObject)
            .filter(enrollment -> !enrollment.hasExited())
            .filter(enrollment -> {
                CurrentLivingSituation mostRecent = enrollment.getMostRecentCurrentLivingSituation();
                return mostRecent == null || mostRecent.getContactDate().isBefore(cutoffDate);
            })
            .map(enrollment -> enrollment.getId().value())
            .toList();
    }
    
    /**
     * Get street outreach engagement metrics
     */
    @Transactional(readOnly = true)
    public StreetOutreachMetrics getStreetOutreachMetrics(LocalDate startDate, LocalDate endDate) {
        List<JpaCurrentLivingSituationEntity> allContacts = clsRepository
            .findByContactDateBetween(startDate, endDate);
        
        List<JpaCurrentLivingSituationEntity> unshelteredContacts = clsRepository
            .findUnshelteredContacts(startDate, endDate);
        
        int totalContacts = allContacts.size();
        int unshelteredContactCount = unshelteredContacts.size();
        int uniqueClients = (int) allContacts.stream()
            .map(entity -> entity.toDomainObject().getClientId().value())
            .distinct()
            .count();
        
        return new StreetOutreachMetrics(
            totalContacts,
            unshelteredContactCount,
            uniqueClients,
            startDate,
            endDate
        );
    }
    
    /**
     * Delete CLS record
     */
    public void deleteCurrentLivingSituation(UUID recordId) {
        JpaCurrentLivingSituationEntity clsEntity = clsRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("CLS record not found: " + recordId));
        
        CurrentLivingSituation clsRecord = clsEntity.toDomainObject();
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(clsRecord.getEnrollmentId().value())
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + clsRecord.getEnrollmentId()));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Remove from aggregate
        enrollment.removeCurrentLivingSituation(clsRecord);
        
        // Delete from repository
        clsRepository.delete(clsEntity);
        
        // Update enrollment
        enrollmentRepository.save(new JpaProgramEnrollmentEntity(enrollment));
    }
    
    /**
     * Record bulk street outreach contacts for efficiency
     */
    public List<CurrentLivingSituation> recordBulkStreetContacts(
            List<StreetContactRequest> contactRequests) {
        
        List<CurrentLivingSituation> createdRecords = new java.util.ArrayList<>();
        
        for (StreetContactRequest request : contactRequests) {
            CurrentLivingSituation record = recordCurrentLivingSituation(
                request.enrollmentId(),
                request.contactDate(),
                request.livingSituation(),
                request.lengthOfStayDays(),
                request.lengthOfStayAtTimeOfContact(),
                request.verifiedBy(),
                request.createdBy()
            );
            createdRecords.add(record);
        }
        
        return createdRecords;
    }
    
    /**
     * Value object for living situation summary
     */
    public record LivingSituationSummary(PriorLivingSituation livingSituation, int contactCount) {}
    
    /**
     * Value object for street outreach metrics
     */
    public record StreetOutreachMetrics(
        int totalContacts,
        int unshelteredContacts,
        int uniqueClients,
        LocalDate startDate,
        LocalDate endDate
    ) {}
    
    /**
     * Value object for bulk street contact requests
     */
    public record StreetContactRequest(
        UUID enrollmentId,
        LocalDate contactDate,
        PriorLivingSituation livingSituation,
        Integer lengthOfStayDays,
        String lengthOfStayAtTimeOfContact,
        Boolean verifiedBy,
        String createdBy
    ) {}
}
