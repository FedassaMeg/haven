package org.haven.shared.events;

import org.haven.shared.events.DomainEvent;

/**
 * Generic interface for domain event handlers
 */
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
    Class<T> getEventType();
}