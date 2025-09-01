package org.haven.housingassistance.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.shared.domain.Repository;
import java.util.List;
import java.util.Optional;

public interface HousingAssistanceRepository extends Repository<HousingAssistance, HousingAssistanceId> {
    List<HousingAssistance> findByClientId(ClientId clientId);
    List<HousingAssistance> findByEnrollmentId(ProgramEnrollmentId enrollmentId);
    List<HousingAssistance> findByStatus(HousingAssistance.AssistanceStatus status);
    Optional<HousingAssistance> findByClientIdAndStatus(ClientId clientId, HousingAssistance.AssistanceStatus status);
    List<HousingAssistance> findActiveByClientId(ClientId clientId);
}