package org.haven.eventstore.infrastructure;

import org.haven.shared.events.DomainEvent;
import org.haven.eventstore.EventEnvelope;
import org.haven.eventstore.EventStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class JpaEventStore implements EventStore {
    
    private final JpaEventStoreRepository repository;
    private final EventSerializer serializer;
    
    public JpaEventStore(JpaEventStoreRepository repository, 
                        @Qualifier("consentEventSerializer") EventSerializer serializer) {
        this.repository = repository;
        this.serializer = serializer;
    }
    
    @Override
    public <EV extends DomainEvent> void append(UUID aggregateId, long expectedVersion, List<EV> events) {
        if (events.isEmpty()) {
            return;
        }
        
        // Get current version from event store
        long currentVersion = repository.findMaxSequenceByAggregateId(aggregateId).orElse(0L);
        
        // Check optimistic concurrency
        if (expectedVersion != currentVersion) {
            throw new ConcurrencyException(
                String.format("Expected version %d but current version is %d for aggregate %s", 
                    expectedVersion, currentVersion, aggregateId)
            );
        }
        
        // Append events with sequential numbering
        long nextSequence = currentVersion + 1;
        for (EV event : events) {
            String eventData = serializer.serialize(event);
            JpaEventStoreEntity entity = new JpaEventStoreEntity(
                aggregateId,
                nextSequence,
                event.eventType(),
                eventData,
                Instant.now()
            );
            
            repository.save(entity);
            nextSequence++;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EventEnvelope<? extends DomainEvent>> load(UUID aggregateId) {
        List<JpaEventStoreEntity> entities = repository.findByAggregateIdOrderBySequence(aggregateId);
        
        return entities.stream()
            .<EventEnvelope<? extends DomainEvent>>map(entity -> {
                DomainEvent event = serializer.deserialize(entity.getEventData(), entity.getEventType());
                return new EventEnvelope<>(
                    entity.getAggregateId(),
                    entity.getSequence(),
                    entity.getRecordedAt(),
                    event
                );
            })
            .toList();
    }
    
    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) {
            super(message);
        }
    }
}