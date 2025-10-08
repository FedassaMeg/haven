package org.haven.shared.events;

import org.haven.shared.events.DomainEvent;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory event publisher for domain events
 */
@Service
public class EventPublisher {
    
    private final Map<Class<? extends DomainEvent>, List<EventHandler<? extends DomainEvent>>> handlers = 
        new ConcurrentHashMap<>();
    
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void subscribe(EventHandler<T> handler) {
        handlers.computeIfAbsent(handler.getEventType(), k -> new CopyOnWriteArrayList<>())
                .add((EventHandler<DomainEvent>) handler);
    }
    
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        List<EventHandler<? extends DomainEvent>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler<? extends DomainEvent> handler : eventHandlers) {
                try {
                    ((EventHandler<DomainEvent>) handler).handle(event);
                } catch (Exception e) {
                    // Log error but don't stop other handlers
                    System.err.println("Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}