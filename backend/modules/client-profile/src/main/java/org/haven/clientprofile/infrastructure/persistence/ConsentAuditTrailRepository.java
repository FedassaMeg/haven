package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.consent.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentAuditTrailRepository extends JpaRepository<ConsentAuditTrailEntity, Long> {
    
    /**
     * Find audit trail for a specific consent
     */
    List<ConsentAuditTrailEntity> findByConsentIdOrderByOccurredAtAsc(UUID consentId);
    
    /**
     * Find audit trail for a specific client
     */
    List<ConsentAuditTrailEntity> findByClientIdOrderByOccurredAtDesc(UUID clientId);
    
    /**
     * Find audit trail by event type
     */
    List<ConsentAuditTrailEntity> findByEventTypeOrderByOccurredAtDesc(String eventType);
    
    /**
     * Find audit trail by acting user
     */
    List<ConsentAuditTrailEntity> findByActingUserIdOrderByOccurredAtDesc(UUID actingUserId);
    
    /**
     * Find audit trail within date range
     */
    @Query("SELECT a FROM ConsentAuditTrailEntity a WHERE a.occurredAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.occurredAt DESC")
    List<ConsentAuditTrailEntity> findByOccurredAtBetween(@Param("startDate") Instant startDate,
                                                          @Param("endDate") Instant endDate);
    
    /**
     * Find audit trail for VAWA protected consents
     */
    @Query("SELECT a FROM ConsentAuditTrailEntity a WHERE a.consentType IN ('COURT_TESTIMONY', 'LEGAL_COUNSEL_COMMUNICATION', 'FAMILY_CONTACT') " +
           "ORDER BY a.occurredAt DESC")
    List<ConsentAuditTrailEntity> findVAWAProtectedAuditTrail();
    
    /**
     * Search audit trail with comprehensive filters
     */
    @Query("SELECT a FROM ConsentAuditTrailEntity a WHERE " +
           "(:consentId IS NULL OR a.consentId = :consentId) " +
           "AND (:clientId IS NULL OR a.clientId = :clientId) " +
           "AND (:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:consentType IS NULL OR a.consentType = :consentType) " +
           "AND (:actingUserId IS NULL OR a.actingUserId = :actingUserId) " +
           "AND (:startDate IS NULL OR a.occurredAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.occurredAt <= :endDate) " +
           "ORDER BY a.occurredAt DESC")
    List<ConsentAuditTrailEntity> searchAuditTrail(@Param("consentId") UUID consentId,
                                                   @Param("clientId") UUID clientId,
                                                   @Param("eventType") String eventType,
                                                   @Param("consentType") ConsentType consentType,
                                                   @Param("actingUserId") UUID actingUserId,
                                                   @Param("startDate") Instant startDate,
                                                   @Param("endDate") Instant endDate);
}