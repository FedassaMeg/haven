package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceProvided(
    UUID enrollmentId,
    UUID clientId,
    UUID serviceId,
    CodeableConcept serviceType,
    CodeableConcept serviceCategory,
    LocalDate serviceDate,
    String providedBy,
    UUID providerId,
    String description,
    Integer durationMinutes,
    String location,
    boolean isConfidential,
    Instant occurredAt
) implements DomainEvent {
    
    public ServiceProvided {
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (serviceId == null) throw new IllegalArgumentException("Service ID cannot be null");
        if (serviceType == null) throw new IllegalArgumentException("Service type cannot be null");
        if (serviceDate == null) throw new IllegalArgumentException("Service date cannot be null");
        if (providedBy == null || providedBy.trim().isEmpty()) throw new IllegalArgumentException("Provider cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return enrollmentId;
    }
    
    @Override
    public String eventType() {
        return "ServiceProvided";
    }
}