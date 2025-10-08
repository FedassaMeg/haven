package org.haven.eventstore.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.haven.shared.events.DomainEvent;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event serializer with dynamic event type registration.
 * Automatically discovers all DomainEvent implementations in the org.haven package.
 */
@Component("consentEventSerializer")
public class EventSerializer {

    private static final Logger logger = LoggerFactory.getLogger(EventSerializer.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends DomainEvent>> eventTypeMap;

    public EventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.eventTypeMap = new ConcurrentHashMap<>();
    }

    /**
     * Auto-register all DomainEvent implementations found in org.haven package.
     * This eliminates the need to manually update the serializer for each new event type.
     */
    @PostConstruct
    void registerEvents() {
        logger.info("Scanning for DomainEvent implementations in org.haven package...");

        Reflections reflections = new Reflections(
            new ConfigurationBuilder()
                .forPackages("org.haven")
        );

        Set<Class<? extends DomainEvent>> eventClasses = reflections.getSubTypesOf(DomainEvent.class);

        for (Class<? extends DomainEvent> eventClass : eventClasses) {
            try {
                // Get the event type from a temporary instance
                DomainEvent instance = eventClass.getDeclaredConstructor().newInstance();
                String eventType = instance.eventType();
                eventTypeMap.put(eventType, eventClass);
                logger.debug("Registered event type: {} -> {}", eventType, eventClass.getName());
            } catch (Exception e) {
                // If we can't instantiate with no-args constructor, use simple class name
                String eventType = eventClass.getSimpleName();
                eventTypeMap.put(eventType, eventClass);
                logger.debug("Registered event type (fallback): {} -> {}", eventType, eventClass.getName());
            }
        }

        logger.info("Registered {} DomainEvent types", eventTypeMap.size());
    }
    
    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event: " + event.eventType(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> T deserialize(String eventData, String eventType) {
        try {
            Class<? extends DomainEvent> eventClass = eventTypeMap.get(eventType);
            if (eventClass == null) {
                throw new IllegalArgumentException(
                    "Unknown event type: " + eventType + ". Available types: " + eventTypeMap.keySet()
                );
            }
            return (T) objectMapper.readValue(eventData, eventClass);
        } catch (Exception e) {
            throw new EventSerializationException("Failed to deserialize event: " + eventType, e);
        }
    }

    /**
     * Manually register an event type.
     * Useful for testing or runtime registration of external event types.
     */
    public void registerEventType(String eventType, Class<? extends DomainEvent> eventClass) {
        eventTypeMap.put(eventType, eventClass);
        logger.info("Manually registered event type: {} -> {}", eventType, eventClass.getName());
    }

    /**
     * Get all registered event types.
     * Useful for debugging and monitoring.
     */
    public Set<String> getRegisteredEventTypes() {
        return eventTypeMap.keySet();
    }
    
    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}