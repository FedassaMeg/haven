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
 * JPA Repository for Current Living Situation Records
 */
@Repository
public interface JpaCurrentLivingSituationRepository extends JpaRepository<JpaCurrentLivingSituationEntity, UUID> {
    
    /**
     * Find all CLS records for an enrollment
     */
    List<JpaCurrentLivingSituationEntity> findByEnrollmentIdOrderByContactDateDesc(UUID enrollmentId);
    
    /**
     * Find CLS records by client
     */
    List<JpaCurrentLivingSituationEntity> findByClientIdOrderByContactDateDesc(UUID clientId);
    
    /**
     * Find most recent CLS record for enrollment
     */
    Optional<JpaCurrentLivingSituationEntity> findFirstByEnrollmentIdOrderByContactDateDesc(UUID enrollmentId);
    
    /**
     * Find CLS records within date range
     */
    @Query("SELECT c FROM JpaCurrentLivingSituationEntity c WHERE c.contactDate >= :startDate AND c.contactDate <= :endDate ORDER BY c.contactDate DESC")
    List<JpaCurrentLivingSituationEntity> findByContactDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find CLS records for enrollment within date range
     */
    @Query("SELECT c FROM JpaCurrentLivingSituationEntity c WHERE c.enrollmentId = :enrollmentId AND c.contactDate >= :startDate AND c.contactDate <= :endDate ORDER BY c.contactDate DESC")
    List<JpaCurrentLivingSituationEntity> findByEnrollmentIdAndContactDateBetween(
        @Param("enrollmentId") UUID enrollmentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find unsheltered contacts
     */
    @Query("SELECT c FROM JpaCurrentLivingSituationEntity c WHERE c.livingSituation = 'PLACE_NOT_MEANT_FOR_HABITATION' AND c.contactDate >= :startDate AND c.contactDate <= :endDate")
    List<JpaCurrentLivingSituationEntity> findUnshelteredContacts(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count contacts by living situation
     */
    @Query("SELECT c.livingSituation, COUNT(c) FROM JpaCurrentLivingSituationEntity c WHERE c.contactDate >= :startDate AND c.contactDate <= :endDate GROUP BY c.livingSituation")
    List<Object[]> countContactsByLivingSituation(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}