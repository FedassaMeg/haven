package org.haven.housingassistance.infrastructure.persistence;

import org.haven.housingassistance.domain.LandlordCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for landlord communications
 */
@Repository
public interface LandlordCommunicationRepository extends JpaRepository<LandlordCommunication, UUID> {
    
    /**
     * Find all communications for a specific client
     */
    List<LandlordCommunication> findByClientId(UUID clientId);
    
    /**
     * Find all communications for a specific landlord
     */
    List<LandlordCommunication> findByLandlordId(UUID landlordId);
    
    /**
     * Find communications by client and landlord
     */
    List<LandlordCommunication> findByClientIdAndLandlordId(UUID clientId, UUID landlordId);
    
    /**
     * Find communications by housing assistance
     */
    List<LandlordCommunication> findByHousingAssistanceId(UUID housingAssistanceId);
    
    /**
     * Find communications by status
     */
    List<LandlordCommunication> findBySentStatus(String sentStatus);
    
    /**
     * Find communications sent within a date range
     */
    @Query("SELECT lc FROM LandlordCommunication lc WHERE lc.sentAt BETWEEN :startDate AND :endDate")
    List<LandlordCommunication> findBySentAtBetween(
        @Param("startDate") Instant startDate, 
        @Param("endDate") Instant endDate
    );
    
    /**
     * Find communications by channel
     */
    List<LandlordCommunication> findByChannel(String channel);
    
    /**
     * Find communications that required consent
     */
    @Query("SELECT lc FROM LandlordCommunication lc WHERE lc.consentChecked = true AND lc.consentType = :consentType")
    List<LandlordCommunication> findByConsentType(@Param("consentType") String consentType);
    
    /**
     * Count communications by client and status
     */
    long countByClientIdAndSentStatus(UUID clientId, String sentStatus);
    
    /**
     * Find recent communications for audit purposes
     */
    @Query("SELECT lc FROM LandlordCommunication lc WHERE lc.sentBy = :userId ORDER BY lc.createdAt DESC")
    List<LandlordCommunication> findRecentByUser(@Param("userId") UUID userId);
}