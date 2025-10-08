package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.domain.consent.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentLedgerRepository extends JpaRepository<ConsentLedgerEntity, UUID> {
    
    /**
     * Find all consents for a specific client
     */
    List<ConsentLedgerEntity> findByClientIdOrderByGrantedAtDesc(UUID clientId);
    
    /**
     * Find active consents for a client
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE c.clientId = :clientId " +
           "AND c.status = :status " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now) " +
           "ORDER BY c.grantedAt DESC")
    List<ConsentLedgerEntity> findActiveConsentsForClient(@Param("clientId") UUID clientId,
                                                          @Param("status") ConsentStatus status,
                                                          @Param("now") Instant now);
    
    /**
     * Find active consent by type for a client
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE c.clientId = :clientId " +
           "AND c.consentType = :consentType " +
           "AND c.status = :status " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now) " +
           "ORDER BY c.grantedAt DESC")
    Optional<ConsentLedgerEntity> findActiveConsentByClientAndType(@Param("clientId") UUID clientId,
                                                                  @Param("consentType") ConsentType consentType,
                                                                  @Param("status") ConsentStatus status,
                                                                  @Param("now") Instant now);
    
    /**
     * Find consents expiring between dates
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE c.status = :status " +
           "AND c.expiresAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.expiresAt")
    List<ConsentLedgerEntity> findConsentsExpiringBetween(@Param("status") ConsentStatus status,
                                                          @Param("startDate") Instant startDate,
                                                          @Param("endDate") Instant endDate);
    
    /**
     * Find expired consents
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE c.status = :status " +
           "AND c.expiresAt < :now " +
           "ORDER BY c.expiresAt")
    List<ConsentLedgerEntity> findExpiredConsents(@Param("status") ConsentStatus status,
                                                  @Param("now") Instant now);
    
    /**
     * Find consents by recipient organization
     */
    List<ConsentLedgerEntity> findByRecipientOrganizationOrderByGrantedAtDesc(String recipientOrganization);
    
    /**
     * Find VAWA protected consents
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE c.isVAWAProtected = true " +
           "ORDER BY c.grantedAt DESC")
    List<ConsentLedgerEntity> findVAWAProtectedConsents();
    
    /**
     * Find consents by status
     */
    List<ConsentLedgerEntity> findByStatusOrderByGrantedAtDesc(ConsentStatus status);
    
    /**
     * Find consents by type
     */
    List<ConsentLedgerEntity> findByConsentTypeOrderByGrantedAtDesc(ConsentType consentType);
    
    /**
     * Check if valid consent exists for specific parameters
     */
    @Query("SELECT COUNT(c) > 0 FROM ConsentLedgerEntity c WHERE c.clientId = :clientId " +
           "AND c.consentType = :consentType " +
           "AND c.recipientOrganization = :recipientOrganization " +
           "AND c.status = :status " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    boolean hasValidConsentFor(@Param("clientId") UUID clientId,
                              @Param("consentType") ConsentType consentType,
                              @Param("recipientOrganization") String recipientOrganization,
                              @Param("status") ConsentStatus status,
                              @Param("now") Instant now);
    
    /**
     * Search consents with filters
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE " +
           "(:clientId IS NULL OR c.clientId = :clientId) " +
           "AND (:consentType IS NULL OR c.consentType = :consentType) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:recipientOrganization IS NULL OR LOWER(c.recipientOrganization) LIKE LOWER(CONCAT('%', :recipientOrganization, '%'))) " +
           "AND (:grantedAfter IS NULL OR c.grantedAt >= :grantedAfter) " +
           "AND (:grantedBefore IS NULL OR c.grantedAt <= :grantedBefore) " +
           "ORDER BY c.grantedAt DESC")
    List<ConsentLedgerEntity> searchConsents(@Param("clientId") UUID clientId,
                                            @Param("consentType") ConsentType consentType,
                                            @Param("status") ConsentStatus status,
                                            @Param("recipientOrganization") String recipientOrganization,
                                            @Param("grantedAfter") Instant grantedAfter,
                                            @Param("grantedBefore") Instant grantedBefore);
    
    /**
     * Find consents requiring review (near expiration)
     */
    @Query("SELECT c FROM ConsentLedgerEntity c WHERE c.status = 'GRANTED' " +
           "AND c.expiresAt IS NOT NULL " +
           "AND c.expiresAt BETWEEN :now AND :reviewThreshold " +
           "ORDER BY c.expiresAt")
    List<ConsentLedgerEntity> findConsentsRequiringReview(@Param("now") Instant now,
                                                          @Param("reviewThreshold") Instant reviewThreshold);
}