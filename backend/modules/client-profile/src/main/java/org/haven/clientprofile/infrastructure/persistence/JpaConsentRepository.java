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

/**
 * JPA repository interface for Consent entities
 */
@Repository
public interface JpaConsentRepository extends JpaRepository<JpaConsentEntity, UUID> {
    
    /**
     * Find consents by client ID
     */
    List<JpaConsentEntity> findByClientId(UUID clientId);
    
    /**
     * Find active consents for a client
     */
    @Query("SELECT c FROM JpaConsentEntity c WHERE c.clientId = :clientId AND c.status = :grantedStatus AND (c.expiresAt IS NULL OR c.expiresAt > :currentTime)")
    List<JpaConsentEntity> findActiveConsentsByClientId(@Param("clientId") UUID clientId, 
                                                       @Param("grantedStatus") ConsentStatus grantedStatus,
                                                       @Param("currentTime") Instant currentTime);
    
    /**
     * Find specific consent type for a client
     */
    @Query("SELECT c FROM JpaConsentEntity c WHERE c.clientId = :clientId AND c.consentType = :consentType AND c.status = :grantedStatus AND (c.expiresAt IS NULL OR c.expiresAt > :currentTime)")
    Optional<JpaConsentEntity> findActiveConsentByClientIdAndType(@Param("clientId") UUID clientId,
                                                                @Param("consentType") ConsentType consentType,
                                                                @Param("grantedStatus") ConsentStatus grantedStatus,
                                                                @Param("currentTime") Instant currentTime);
    
    /**
     * Find consents expiring within a date range
     */
    @Query("SELECT c FROM JpaConsentEntity c WHERE c.status = :grantedStatus AND c.expiresAt BETWEEN :startDate AND :endDate")
    List<JpaConsentEntity> findConsentsExpiringBetween(@Param("grantedStatus") ConsentStatus grantedStatus,
                                                      @Param("startDate") Instant startDate, 
                                                      @Param("endDate") Instant endDate);
    
    /**
     * Find expired consents that haven't been marked as expired
     */
    @Query("SELECT c FROM JpaConsentEntity c WHERE c.status = :grantedStatus AND c.expiresAt < :currentTime")
    List<JpaConsentEntity> findExpiredConsents(@Param("grantedStatus") ConsentStatus grantedStatus,
                                             @Param("currentTime") Instant currentTime);
    
    /**
     * Find consents by recipient organization
     */
    List<JpaConsentEntity> findByRecipientOrganization(String recipientOrganization);
    
    /**
     * Find VAWA protected consents
     */
    List<JpaConsentEntity> findByIsVAWAProtectedTrue();
    
    /**
     * Find consents by type
     */
    List<JpaConsentEntity> findByConsentType(ConsentType consentType);
    
    /**
     * Find consents by status
     */
    List<JpaConsentEntity> findByStatus(ConsentStatus status);
    
    /**
     * Check if client has valid consent for specific type and recipient
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM JpaConsentEntity c WHERE c.clientId = :clientId AND c.consentType = :consentType AND c.status = :grantedStatus AND (c.expiresAt IS NULL OR c.expiresAt > :currentTime) AND (c.recipientOrganization = :recipientOrganization OR c.recipientOrganization IS NULL)")
    boolean hasValidConsentFor(@Param("clientId") UUID clientId,
                             @Param("consentType") ConsentType consentType,
                             @Param("recipientOrganization") String recipientOrganization,
                             @Param("grantedStatus") ConsentStatus grantedStatus,
                             @Param("currentTime") Instant currentTime);
}