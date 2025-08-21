package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentOpened(
    UUID enrollmentId,
    UUID clientId,
    UUID programId,
    LocalDate enrollmentDate,
    CodeableConcept relationshipToHead,
    CodeableConcept residencePriorToEntry,
    String entryFrom,
    Instant occurredAt
) implements DomainEvent {
    
    public EnrollmentOpened {
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (programId == null) throw new IllegalArgumentException("Program ID cannot be null");
        if (enrollmentDate == null) throw new IllegalArgumentException("Enrollment date cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return enrollmentId;
    }
    
    @Override
    public String eventType() {
        return "EnrollmentOpened";
    }
}