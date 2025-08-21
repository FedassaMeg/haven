package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentOpened(
    UUID enrollmentId,
    UUID clientId,
    UUID caseId,
    UUID programId,
    String programName,
    LocalDate enrollmentDate,
    String enrolledBy,
    String entryPoint,
    Instant occurredAt
) implements DomainEvent {
    
    public EnrollmentOpened {
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (programId == null) throw new IllegalArgumentException("Program ID cannot be null");
        if (enrollmentDate == null) throw new IllegalArgumentException("Enrollment date cannot be null");
        if (enrolledBy == null || enrolledBy.trim().isEmpty()) throw new IllegalArgumentException("Enrolled by cannot be null or empty");
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