package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProgramExited extends DomainEvent {
    private final UUID enrollmentId;
    private final LocalDate exitDate;
    private final CodeableConcept exitReason;
    private final CodeableConcept destination;
    private final Boolean exitedToPermanentHousing;
    private final String recordedBy;

    public ProgramExited(
        UUID enrollmentId,
        LocalDate exitDate,
        CodeableConcept exitReason,
        CodeableConcept destination,
        Boolean exitedToPermanentHousing,
        String recordedBy,
        Instant occurredAt
    ) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (exitDate == null) throw new IllegalArgumentException("Exit date cannot be null");
        if (exitReason == null) throw new IllegalArgumentException("Exit reason cannot be null");
        if (destination == null) throw new IllegalArgumentException("Destination cannot be null");
        if (recordedBy == null) throw new IllegalArgumentException("Recorded by cannot be null");

        this.enrollmentId = enrollmentId;
        this.exitDate = exitDate;
        this.exitReason = exitReason;
        this.destination = destination;
        this.exitedToPermanentHousing = exitedToPermanentHousing;
        this.recordedBy = recordedBy;
    }

    @Override
    public String eventType() {
        return "ProgramExited";
    }

    public UUID enrollmentId() {
        return enrollmentId;
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

    public Boolean exitedToPermanentHousing() {
        return exitedToPermanentHousing;
    }

    public String recordedBy() {
        return recordedBy;
    }

    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public LocalDate getExitDate() { return exitDate; }
    public CodeableConcept getExitReason() { return exitReason; }
    public CodeableConcept getDestination() { return destination; }
    public Boolean getExitedToPermanentHousing() { return exitedToPermanentHousing; }
    public String getRecordedBy() { return recordedBy; }
}