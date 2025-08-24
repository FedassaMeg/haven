package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceEpisodeLinked(
    UUID enrollmentId,
    UUID serviceEpisodeId,
    String serviceType,
    LocalDate serviceDate,
    String providedBy,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return enrollmentId;
    }
    
    @Override
    public String eventType() {
        return "ServiceEpisodeLinked";
    }
}