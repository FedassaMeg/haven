package org.haven.reporting.infrastructure.persistence;

import org.haven.eventstore.EventEnvelope;
import org.haven.eventstore.EventStore;
import org.haven.reporting.domain.ExportJobAggregate;
import org.haven.reporting.domain.ExportJobRepository;
import org.haven.shared.events.DomainEvent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Event-sourced repository for ExportJob aggregate
 * Follows same pattern as CaseRecord and ServiceEpisode repositories
 */
@Repository
public class EventSourcedExportJobRepository implements ExportJobRepository {

    private final EventStore eventStore;

    public EventSourcedExportJobRepository(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void save(ExportJobAggregate aggregate) {
        List<DomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();

        if (!uncommittedEvents.isEmpty()) {
            long expectedVersion = aggregate.getVersion() - uncommittedEvents.size();
            eventStore.append(aggregate.getId().value(), expectedVersion, uncommittedEvents);
            aggregate.markEventsAsCommitted();
        }
    }

    @Override
    public Optional<ExportJobAggregate> findById(UUID exportJobId) {
        List<EventEnvelope<? extends DomainEvent>> envelopes = eventStore.load(exportJobId);

        if (envelopes.isEmpty()) {
            return Optional.empty();
        }

        List<DomainEvent> events = envelopes.stream()
                .map(EventEnvelope::getEvent)
                .collect(Collectors.toList());

        ExportJobAggregate aggregate = ExportJobAggregate.reconstruct(exportJobId, events);
        return Optional.of(aggregate);
    }

    @Override
    public boolean exists(UUID exportJobId) {
        return !eventStore.load(exportJobId).isEmpty();
    }

    @Override
    public List<ExportJobAggregate> findByRequestedByUserId(String userId) {
        // Note: This is a simple implementation that loads all events and filters
        // In production, you would want to use a read model or indexed query
        // For now, return empty list as placeholder
        // TODO: Implement proper query using read model or event store query capabilities
        return List.of();
    }
}
