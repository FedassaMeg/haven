package org.haven.housingassistance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LandlordCommunicationRepository extends JpaRepository<LandlordCommunication, UUID> {
    List<LandlordCommunication> findByClientId(UUID clientId);
    List<LandlordCommunication> findByLandlordId(UUID landlordId);
    List<LandlordCommunication> findByClientIdAndLandlordId(UUID clientId, UUID landlordId);
    List<LandlordCommunication> findByHousingAssistanceId(UUID housingAssistanceId);
}