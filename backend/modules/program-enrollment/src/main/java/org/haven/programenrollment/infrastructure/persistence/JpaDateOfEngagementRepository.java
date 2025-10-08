package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Date of Engagement Records
 */
@Repository
public interface JpaDateOfEngagementRepository extends JpaRepository<JpaDateOfEngagementEntity, UUID> {
    
    /**
     * Find engagement record for enrollment (non-correction)
     */
    Optional<JpaDateOfEngagementEntity> findByEnrollmentIdAndIsCorrection(UUID enrollmentId, Boolean isCorrection);
    
    /**
     * Find latest effective engagement date for enrollment
     */
    Optional<JpaDateOfEngagementEntity> findFirstByEnrollmentIdOrderByCreatedAtDesc(UUID enrollmentId);
    
    /**
     * Find all engagement records for client
     */
    List<JpaDateOfEngagementEntity> findByClientIdOrderByEngagementDateDesc(UUID clientId);
    
    /**
     * Find engagement records within date range
     */
    @Query("SELECT e FROM JpaDateOfEngagementEntity e WHERE e.engagementDate >= :startDate AND e.engagementDate <= :endDate ORDER BY e.engagementDate DESC")
    List<JpaDateOfEngagementEntity> findByEngagementDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find enrollments without engagement dates
     */
    @Query("""
        SELECT DISTINCT en.id FROM JpaProgramEnrollmentEntity en 
        WHERE NOT EXISTS (
            SELECT e FROM JpaDateOfEngagementEntity e 
            WHERE e.enrollmentId = en.id 
            AND e.isCorrection = FALSE
        )
        """)
    List<UUID> findEnrollmentsMissingEngagementDate();
    
    /**
     * Find correction records
     */
    List<JpaDateOfEngagementEntity> findByCorrectsRecordIdOrderByCreatedAtDesc(UUID originalRecordId);
    
    /**
     * Check if enrollment has engagement date
     */
    boolean existsByEnrollmentId(UUID enrollmentId);
}