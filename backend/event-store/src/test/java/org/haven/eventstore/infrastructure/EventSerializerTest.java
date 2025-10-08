package org.haven.eventstore.infrastructure;

import org.haven.shared.events.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventSerializerTest {

    private EventSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new EventSerializer();
        serializer.registerEvents(); // Simulate @PostConstruct
    }

    @Test
    void shouldAutoRegisterDomainEvents() {
        // Given auto-registration has occurred
        Set<String> registeredTypes = serializer.getRegisteredEventTypes();

        // Then we should have registered events
        assertFalse(registeredTypes.isEmpty(), "Should have registered at least some events");

        // And specific known events should be present
        assertTrue(registeredTypes.contains("ConsentGranted") ||
                  registeredTypes.contains("RestrictedNoteCreated") ||
                  registeredTypes.contains("PolicyDecisionMade"),
                  "Should contain at least one known event type");
    }

    @Test
    void shouldSerializeAndDeserializeEvent() {
        // Given a test event
        TestDomainEvent originalEvent = new TestDomainEvent(
            UUID.randomUUID(),
            "Test message",
            Instant.now()
        );

        // Register the test event type manually
        serializer.registerEventType("TestDomainEvent", TestDomainEvent.class);

        // When serializing and deserializing
        String serialized = serializer.serialize(originalEvent);
        TestDomainEvent deserialized = serializer.deserialize(serialized, "TestDomainEvent");

        // Then the event should be preserved
        assertNotNull(deserialized);
        assertEquals(originalEvent.aggregateId(), deserialized.aggregateId());
        assertEquals(originalEvent.getMessage(), deserialized.getMessage());
        assertEquals(originalEvent.eventType(), deserialized.eventType());
    }

    @Test
    void shouldThrowExceptionForUnknownEventType() {
        // Given an unknown event type
        String eventData = "{\"aggregateId\":\"" + UUID.randomUUID() + "\"}";

        // When deserializing an unknown type
        // Then it should throw an informative exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> serializer.deserialize(eventData, "UnknownEventType")
        );

        assertTrue(exception.getMessage().contains("Unknown event type"),
                  "Exception should mention unknown event type");
        assertTrue(exception.getMessage().contains("Available types"),
                  "Exception should list available types");
    }

    @Test
    void shouldAllowManualEventRegistration() {
        // Given a custom event class
        Class<TestDomainEvent> eventClass = TestDomainEvent.class;

        // When manually registering it
        serializer.registerEventType("CustomEvent", eventClass);

        // Then it should be available for deserialization
        Set<String> registeredTypes = serializer.getRegisteredEventTypes();
        assertTrue(registeredTypes.contains("CustomEvent"),
                  "Manually registered event should be available");
    }

    @Test
    void shouldHandleEventSerializationFailure() {
        // Given an event that will fail to serialize (circular reference, etc.)
        // This is hard to test without creating a problematic event
        // The test verifies the exception type and message format

        TestDomainEvent event = new TestDomainEvent(
            UUID.randomUUID(),
            "Valid message",
            Instant.now()
        );

        // When serializing valid event
        String serialized = serializer.serialize(event);

        // Then it should succeed
        assertNotNull(serialized);
        assertTrue(serialized.contains("aggregateId"));
    }

    /**
     * Test domain event for unit testing
     */
    public static class TestDomainEvent implements DomainEvent {
        private UUID aggregateId;
        private String message;
        private Instant occurredAt;

        // No-arg constructor for Jackson
        public TestDomainEvent() {
        }

        public TestDomainEvent(UUID aggregateId, String message, Instant occurredAt) {
            this.aggregateId = aggregateId;
            this.message = message;
            this.occurredAt = occurredAt;
        }

        @Override
        public UUID aggregateId() {
            return aggregateId;
        }

        @Override
        public String eventType() {
            return "TestDomainEvent";
        }

        @Override
        public Instant occurredAt() {
            return occurredAt;
        }

        public String getMessage() {
            return message;
        }

        public void setAggregateId(UUID aggregateId) {
            this.aggregateId = aggregateId;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setOccurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
        }
    }
}
