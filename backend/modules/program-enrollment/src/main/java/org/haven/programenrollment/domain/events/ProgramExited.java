package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProgramExited(
    UUID enrollmentId,
    LocalDate exitDate,
    CodeableConcept exitReason,
    CodeableConcept destination,
    Boolean exitedToPermanentHousing,
    String recordedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public ProgramExited {
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (exitDate == null) throw new IllegalArgumentException("Exit date cannot be null");
        if (exitReason == null) throw new IllegalArgumentException("Exit reason cannot be null");
        if (destination == null) throw new IllegalArgumentException("Destination cannot be null");
        if (recordedBy == null) throw new IllegalArgumentException("Recorded by cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return enrollmentId;
    }
    
    @Override
    public String eventType() {
        return "ProgramExited";
    }
}