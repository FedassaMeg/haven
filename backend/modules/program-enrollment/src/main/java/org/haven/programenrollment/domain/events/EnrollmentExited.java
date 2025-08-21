package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentExited(
    UUID enrollmentId,
    UUID clientId,
    UUID programId,
    LocalDate exitDate,
    CodeableConcept exitReason,
    CodeableConcept destination,
    String exitNotes,
    Instant occurredAt
) implements DomainEvent {
    
    public EnrollmentExited {
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (programId == null) throw new IllegalArgumentException("Program ID cannot be null");
        if (exitDate == null) throw new IllegalArgumentException("Exit date cannot be null");
        if (exitReason == null) throw new IllegalArgumentException("Exit reason cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return enrollmentId;
    }
    
    @Override
    public String eventType() {
        return "EnrollmentExited";
    }
}