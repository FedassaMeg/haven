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
 * JPA Repository for Bed Night Records
 */
@Repository
public interface JpaBedNightRepository extends JpaRepository<JpaBedNightEntity, UUID> {
    
    /**
     * Find all bed nights for an enrollment
     */
    List<JpaBedNightEntity> findByEnrollmentIdOrderByBedNightDateDesc(UUID enrollmentId);
    
    /**
     * Find bed nights for client
     */
    List<JpaBedNightEntity> findByClientIdOrderByBedNightDateDesc(UUID clientId);
    
    /**
     * Find bed nights within date range
     */
    @Query("SELECT b FROM JpaBedNightEntity b WHERE b.bedNightDate >= :startDate AND b.bedNightDate <= :endDate ORDER BY b.bedNightDate DESC")
    List<JpaBedNightEntity> findByBedNightDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find bed nights for enrollment within date range
     */
    @Query("SELECT b FROM JpaBedNightEntity b WHERE b.enrollmentId = :enrollmentId AND b.bedNightDate >= :startDate AND b.bedNightDate <= :endDate ORDER BY b.bedNightDate DESC")
    List<JpaBedNightEntity> findByEnrollmentIdAndBedNightDateBetween(
        @Param("enrollmentId") UUID enrollmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find specific bed night
     */
    Optional<JpaBedNightEntity> findByEnrollmentIdAndBedNightDate(UUID enrollmentId, LocalDate bedNightDate);
    
    /**
     * Count bed nights for enrollment
     */
    Long countByEnrollmentId(UUID enrollmentId);
    
    /**
     * Count bed nights within date range
     */
    @Query("SELECT COUNT(b) FROM JpaBedNightEntity b WHERE b.bedNightDate >= :startDate AND b.bedNightDate <= :endDate")
    Long countByBedNightDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Check if bed night exists for enrollment and date
     */
    boolean existsByEnrollmentIdAndBedNightDate(UUID enrollmentId, LocalDate bedNightDate);
    
    /**
     * Delete bed night by enrollment and date
     */
    void deleteByEnrollmentIdAndBedNightDate(UUID enrollmentId, LocalDate bedNightDate);
}