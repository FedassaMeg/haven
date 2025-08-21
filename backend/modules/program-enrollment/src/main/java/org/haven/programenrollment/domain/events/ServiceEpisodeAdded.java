package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceEpisodeAdded(
    UUID enrollmentId,
    UUID serviceEpisodeId,
    CodeableConcept serviceType,
    LocalDate serviceDate,
    String providedBy,
    String description,
    Instant occurredAt
) implements DomainEvent {
    
    public ServiceEpisodeAdded {
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (serviceEpisodeId == null) throw new IllegalArgumentException("Service episode ID cannot be null");
        if (serviceType == null) throw new IllegalArgumentException("Service type cannot be null");
        if (serviceDate == null) throw new IllegalArgumentException("Service date cannot be null");
        if (providedBy == null) throw new IllegalArgumentException("Provider cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return enrollmentId;
    }
    
    @Override
    public String eventType() {
        return "ServiceEpisodeAdded";
    }
}