package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.*;
import org.haven.eventstore.EventEnvelope;
import org.haven.eventstore.EventStore;
import org.haven.shared.events.DomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository implementation for Consent aggregate with event sourcing
 */
@Component
public class ConsentRepositoryImpl implements ConsentRepository {
    
    private final JpaConsentRepository jpaRepository;
    private final EventStore eventStore;
    private final ApplicationEventPublisher eventPublisher;
    
    @Autowired
    public ConsentRepositoryImpl(JpaConsentRepository jpaRepository, EventStore eventStore, ApplicationEventPublisher eventPublisher) {
        this.jpaRepository = jpaRepository;
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Optional<Consent> findById(ConsentId id) {
        List<EventEnvelope<? extends DomainEvent>> events = eventStore.load(id.value());
        
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        // Reconstruct aggregate from events
        Consent consent = Consent.reconstruct();
        for (EventEnvelope<? extends DomainEvent> envelope : events) {
            consent.replay(envelope.event(), envelope.sequence());
        }
        
        return Optional.of(consent);
    }
    
    @Override
    public void save(Consent aggregate) {
        // Extract uncommitted events from the aggregate
        List<DomainEvent> pendingEvents = aggregate.getPendingEvents();
        
        if (!pendingEvents.isEmpty()) {
            // Save events to event store with optimistic concurrency control
            eventStore.append(aggregate.getId().value(), aggregate.getVersion() - pendingEvents.size(), pendingEvents);
            
            // Publish events for projection handlers
            for (DomainEvent event : pendingEvents) {
                eventPublisher.publishEvent(event);
            }
            
            // Clear pending events after successful save
            aggregate.clearPendingEvents();
        }
        
        // Update read model/projection for fast queries
        JpaConsentEntity entity = JpaConsentEntity.fromDomain(aggregate);
        jpaRepository.save(entity);
    }
    
    @Override
    public void delete(Consent aggregate) {
        jpaRepository.deleteById(aggregate.getId().value());
    }
    
    @Override
    public ConsentId nextId() {
        return ConsentId.newId();
    }
    
    @Override
    public List<Consent> findActiveConsentsForClient(ClientId clientId) {
        List<JpaConsentEntity> entities = jpaRepository.findActiveConsentsByClientId(
            clientId.value(), ConsentStatus.GRANTED, Instant.now()
        );
        
        return entities.stream()
            .map(entity -> findById(new ConsentId(entity.getId())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
    
    @Override
    public Optional<Consent> findActiveConsentByType(ClientId clientId, ConsentType consentType) {
        Optional<JpaConsentEntity> entity = jpaRepository.findActiveConsentByClientIdAndType(
            clientId.value(), consentType, ConsentStatus.GRANTED, Instant.now()
        );
        
        if (entity.isPresent()) {
            return findById(new ConsentId(entity.get().getId()));
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Consent> findAllConsentsForClient(ClientId clientId) {
        List<JpaConsentEntity> entities = jpaRepository.findByClientId(clientId.value());
        
        return entities.stream()
            .map(entity -> findById(new ConsentId(entity.getId())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
    
    @Override
    public List<Consent> findConsentsExpiringBetween(Instant startDate, Instant endDate) {
        List<JpaConsentEntity> entities = jpaRepository.findConsentsExpiringBetween(
            ConsentStatus.GRANTED, startDate, endDate
        );
        
        return entities.stream()
            .map(entity -> findById(new ConsentId(entity.getId())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
    
    @Override
    public List<Consent> findExpiredConsents() {
        List<JpaConsentEntity> entities = jpaRepository.findExpiredConsents(
            ConsentStatus.GRANTED, Instant.now()
        );
        
        return entities.stream()
            .map(entity -> findById(new ConsentId(entity.getId())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
    
    @Override
    public List<Consent> findByRecipientOrganization(String recipientOrganization) {
        List<JpaConsentEntity> entities = jpaRepository.findByRecipientOrganization(recipientOrganization);
        
        return entities.stream()
            .map(entity -> findById(new ConsentId(entity.getId())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
    
    @Override
    public boolean hasValidConsentFor(ClientId clientId, ConsentType consentType, String recipientOrganization) {
        return jpaRepository.hasValidConsentFor(
            clientId.value(), consentType, recipientOrganization, ConsentStatus.GRANTED, Instant.now()
        );
    }
    
    // Additional query methods for business needs
    
    /**
     * Find consent entities by client ID (for read models)
     */
    public List<JpaConsentEntity> findConsentEntitiesByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value());
    }
    
    /**
     * Find active consent entities for client (for read models)
     */
    public List<JpaConsentEntity> findActiveConsentEntitiesForClient(ClientId clientId) {
        return jpaRepository.findActiveConsentsByClientId(
            clientId.value(), ConsentStatus.GRANTED, Instant.now()
        );
    }
    
    /**
     * Find VAWA protected consent entities
     */
    public List<JpaConsentEntity> findVAWAProtectedConsents() {
        return jpaRepository.findByIsVAWAProtectedTrue();
    }
}