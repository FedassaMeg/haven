package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EnrollmentExited extends DomainEvent {
    private final UUID enrollmentId;
    private final UUID clientId;
    private final UUID programId;
    private final LocalDate exitDate;
    private final CodeableConcept exitReason;
    private final CodeableConcept destination;
    private final String exitNotes;

    public EnrollmentExited(
        UUID enrollmentId,
        UUID clientId,
        UUID programId,
        LocalDate exitDate,
        CodeableConcept exitReason,
        CodeableConcept destination,
        String exitNotes,
        Instant occurredAt
    ) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (programId == null) throw new IllegalArgumentException("Program ID cannot be null");
        if (exitDate == null) throw new IllegalArgumentException("Exit date cannot be null");
        if (exitReason == null) throw new IllegalArgumentException("Exit reason cannot be null");

        this.enrollmentId = enrollmentId;
        this.clientId = clientId;
        this.programId = programId;
        this.exitDate = exitDate;
        this.exitReason = exitReason;
        this.destination = destination;
        this.exitNotes = exitNotes;
    }

    @Override
    public String eventType() {
        return "EnrollmentExited";
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID programId() {
        return programId;
    }

    public LocalDate exitDate() {
        return exitDate;
    }

    public CodeableConcept exitReason() {
        return exitReason;
    }

    public CodeableConcept destination() {
        return destination;
    }

    public String exitNotes() {
        return exitNotes;
    }

    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public UUID getProgramId() { return programId; }
    public LocalDate getExitDate() { return exitDate; }
    public CodeableConcept getExitReason() { return exitReason; }
    public CodeableConcept getDestination() { return destination; }
    public String getExitNotes() { return exitNotes; }
}