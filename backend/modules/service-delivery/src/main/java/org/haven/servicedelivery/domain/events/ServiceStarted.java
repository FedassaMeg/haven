package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class ServiceStarted extends DomainEvent {
    private final LocalDateTime startTime;
    private final String location;

    public ServiceStarted(UUID episodeId, LocalDateTime startTime, String location, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.startTime = startTime;
        this.location = location;
    }

    public LocalDateTime startTime() {
        return startTime;
    }

    public String location() {
        return location;
    }


    // JavaBean-style getters
    public LocalDateTime getStartTime() { return startTime; }
    public String getLocation() { return location; }
}