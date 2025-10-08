package org.haven.servicedelivery.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.eventstore.EventStore;
import org.haven.servicedelivery.domain.ServiceEpisode;
import org.haven.servicedelivery.domain.ServiceEpisodeId;
import org.haven.servicedelivery.domain.ServiceEpisodeRepository;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.services.FundingSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ServiceEpisodeRepositoryImpl implements ServiceEpisodeRepository {

    private final JpaServiceEpisodeRepository jpaRepository;
    private final EventStore eventStore;

    public ServiceEpisodeRepositoryImpl(JpaServiceEpisodeRepository jpaRepository, EventStore eventStore) {
        this.jpaRepository = jpaRepository;
        this.eventStore = eventStore;
    }
    
    @Override
    public void save(ServiceEpisode serviceEpisode) {
        var entity = new JpaServiceEpisodeEntity(serviceEpisode);
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<ServiceEpisode> findById(ServiceEpisodeId id) {
        return jpaRepository.findById(id.value())
            .map(this::toDomain);
    }
    
    @Override
    public List<ServiceEpisode> findByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findByEnrollmentId(String enrollmentId) {
        return jpaRepository.findByEnrollmentId(enrollmentId).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findByProgramId(String programId) {
        return jpaRepository.findByProgramId(programId).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findByServiceDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByServiceDateBetween(startDate, endDate).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findByPrimaryProviderId(String providerId) {
        return jpaRepository.findByPrimaryProviderId(providerId).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findByFollowUpDateBeforeAndFollowUpRequiredIsNotNull(LocalDate date) {
        return jpaRepository.findByFollowUpDateBeforeAndFollowUpRequiredIsNotNull(date).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findBillableServicesByDateRangeAndFunding(LocalDate startDate, LocalDate endDate, FundingSource fundingSource) {
        // For now, just return billable services by date range
        // TODO: Add funding source filtering
        return jpaRepository.findBillableServicesByDateRange(startDate, endDate).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findConfidentialServices(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findConfidentialServices(startDate, endDate).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public List<ServiceEpisode> findCourtOrderedServices(String courtOrderNumber) {
        return jpaRepository.findByCourtOrderNumber(courtOrderNumber).stream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    public void delete(ServiceEpisodeId id) {
        jpaRepository.deleteById(id.value());
    }
    
    @Override
    public List<ServiceEpisode> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDomain)
            .toList();
    }
    
    /**
     * Reconstruct ServiceEpisode from event store
     * CRITICAL: Do not call ServiceEpisode.create() as it creates new events
     * Instead, load events from EventStore and replay them
     */
    private ServiceEpisode toDomain(JpaServiceEpisodeEntity entity) {
        var eventEnvelopes = eventStore.load(entity.getId());

        if (eventEnvelopes.isEmpty()) {
            throw new IllegalStateException("No events found for service episode: " + entity.getId());
        }

        List<DomainEvent> events = eventEnvelopes.stream()
            .map(envelope -> (DomainEvent) envelope.event())
            .toList();

        return ServiceEpisode.reconstruct(entity.getId(), events);
    }
}