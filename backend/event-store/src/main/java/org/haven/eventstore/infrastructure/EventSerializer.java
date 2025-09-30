package org.haven.eventstore.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.haven.shared.events.DomainEvent;
import org.springframework.stereotype.Component;

@Component("consentEventSerializer")
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    
    public EventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
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
            Class<?> eventClass = Class.forName(getFullClassName(eventType));
            return (T) objectMapper.readValue(eventData, eventClass);
        } catch (Exception e) {
            throw new EventSerializationException("Failed to deserialize event: " + eventType, e);
        }
    }
    
    private String getFullClassName(String eventType) {
        // Map event types to their full class names
        return switch (eventType) {
            case "ConsentGranted" -> "org.haven.clientprofile.domain.consent.events.ConsentGranted";
            case "ConsentRevoked" -> "org.haven.clientprofile.domain.consent.events.ConsentRevoked";
            case "ConsentUpdated" -> "org.haven.clientprofile.domain.consent.events.ConsentUpdated";
            case "ConsentExtended" -> "org.haven.clientprofile.domain.consent.events.ConsentExtended";
            case "ConsentExpired" -> "org.haven.clientprofile.domain.consent.events.ConsentExpired";
            case "RestrictedNoteCreated" -> "org.haven.casemgmt.domain.events.RestrictedNoteCreated";
            case "RestrictedNoteUpdated" -> "org.haven.casemgmt.domain.events.RestrictedNoteUpdated";
            case "RestrictedNoteSealed" -> "org.haven.casemgmt.domain.events.RestrictedNoteSealed";
            case "RestrictedNoteUnsealed" -> "org.haven.casemgmt.domain.events.RestrictedNoteUnsealed";
            case "RestrictedNoteAccessed" -> "org.haven.casemgmt.domain.events.RestrictedNoteAccessed";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
    
    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}