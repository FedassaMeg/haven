package org.haven.shared.domain;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for DDD Aggregate Roots with event sourcing support
 */
public abstract class AggregateRoot<ID extends Identifier> {
    protected ID id;
    protected long version;
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    protected AggregateRoot() {}

    protected AggregateRoot(ID id) {
        this.id = id;
        this.version = 0;
    }

    protected void apply(DomainEvent event) {
        when(event);
        pendingEvents.add(event);
        version++;
    }

    /**
     * Apply event to aggregate state without adding to pending events
     * Used when replaying events from event store
     */
    public void replay(DomainEvent event, long version) {
        when(event);
        this.version = version;
    }

    protected abstract void when(DomainEvent event);

    public ID getId() { return id; }
    public long getVersion() { return version; }
    
    public List<DomainEvent> getPendingEvents() { 
        return List.copyOf(pendingEvents); 
    }
    
    public void clearPendingEvents() { 
        pendingEvents.clear(); 
    }
}