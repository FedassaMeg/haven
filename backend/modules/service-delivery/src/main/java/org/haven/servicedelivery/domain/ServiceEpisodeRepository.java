package org.haven.servicedelivery.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.services.FundingSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ServiceEpisode repository interface
 */
public interface ServiceEpisodeRepository {
    
    void save(ServiceEpisode serviceEpisode);
    
    Optional<ServiceEpisode> findById(ServiceEpisodeId id);
    
    List<ServiceEpisode> findByClientId(ClientId clientId);
    
    List<ServiceEpisode> findByEnrollmentId(String enrollmentId);
    
    List<ServiceEpisode> findByProgramId(String programId);
    
    List<ServiceEpisode> findByServiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<ServiceEpisode> findByPrimaryProviderId(String providerId);
    
    List<ServiceEpisode> findByFollowUpDateBeforeAndFollowUpRequiredIsNotNull(LocalDate date);
    
    List<ServiceEpisode> findBillableServicesByDateRangeAndFunding(
        LocalDate startDate, 
        LocalDate endDate, 
        FundingSource fundingSource
    );
    
    List<ServiceEpisode> findConfidentialServices(LocalDate startDate, LocalDate endDate);
    
    List<ServiceEpisode> findCourtOrderedServices(String courtOrderNumber);
    
    void delete(ServiceEpisodeId id);
    
    List<ServiceEpisode> findAll();
}