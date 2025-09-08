package org.haven.housingassistance.domain;

import java.util.List;
import java.util.UUID;

/**
 * Domain repository interface for landlord communications
 */
public interface LandlordCommunicationRepository {
    List<LandlordCommunication> findByClientId(UUID clientId);
    List<LandlordCommunication> findByLandlordId(UUID landlordId);
    List<LandlordCommunication> findByClientIdAndLandlordId(UUID clientId, UUID landlordId);
    List<LandlordCommunication> findByHousingAssistanceId(UUID housingAssistanceId);
    
    void saveCommunication(LandlordCommunication communication);
    LandlordCommunication findCommunicationById(UUID id);
}