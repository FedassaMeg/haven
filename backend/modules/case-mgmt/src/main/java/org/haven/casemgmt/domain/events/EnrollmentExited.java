package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EnrollmentExited extends DomainEvent {
    private final UUID clientId;
    private final LocalDate exitDate;
    private final CodeableConcept exitReason;
    private final String exitDestination;
    private final String exitNotes;
    private final String exitedBy;

    public EnrollmentExited(UUID enrollmentId, UUID clientId, LocalDate exitDate, CodeableConcept exitReason, String exitDestination, String exitNotes, String exitedBy, Instant occurredAt) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (exitDate == null) throw new IllegalArgumentException("Exit date cannot be null");
        if (exitReason == null) throw new IllegalArgumentException("Exit reason cannot be null");
        if (exitedBy == null || exitedBy.trim().isEmpty()) throw new IllegalArgumentException("Exited by cannot be null or empty");

        this.clientId = clientId;
        this.exitDate = exitDate;
        this.exitReason = exitReason;
        this.exitDestination = exitDestination;
        this.exitNotes = exitNotes;
        this.exitedBy = exitedBy;
    }

    public UUID clientId() {
        return clientId;
    }

    public LocalDate exitDate() {
        return exitDate;
    }

    public CodeableConcept exitReason() {
        return exitReason;
    }

    public String exitDestination() {
        return exitDestination;
    }

    public String exitNotes() {
        return exitNotes;
    }

    public String exitedBy() {
        return exitedBy;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public LocalDate getExitDate() { return exitDate; }
    public CodeableConcept getExitReason() { return exitReason; }
    public String getExitDestination() { return exitDestination; }
    public String getExitNotes() { return exitNotes; }
    public String getExitedBy() { return exitedBy; }
}