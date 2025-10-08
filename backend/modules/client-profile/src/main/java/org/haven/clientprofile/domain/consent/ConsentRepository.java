package org.haven.clientprofile.domain.consent;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Consent aggregate
 */
public interface ConsentRepository extends Repository<Consent, ConsentId> {
    
    /**
     * Find all active consents for a client
     */
    List<Consent> findActiveConsentsForClient(ClientId clientId);
    
    /**
     * Find specific consent type for a client
     */
    Optional<Consent> findActiveConsentByType(ClientId clientId, ConsentType consentType);
    
    /**
     * Find all consents for a client (active and inactive)
     */
    List<Consent> findAllConsentsForClient(ClientId clientId);
    
    /**
     * Find consents expiring within a date range
     */
    List<Consent> findConsentsExpiringBetween(Instant startDate, Instant endDate);
    
    /**
     * Find all expired consents that haven't been marked as expired
     */
    List<Consent> findExpiredConsents();
    
    /**
     * Find consents by recipient organization
     */
    List<Consent> findByRecipientOrganization(String recipientOrganization);
    
    /**
     * Check if client has valid consent for specific operation
     */
    boolean hasValidConsentFor(ClientId clientId, ConsentType consentType, String recipientOrganization);
}