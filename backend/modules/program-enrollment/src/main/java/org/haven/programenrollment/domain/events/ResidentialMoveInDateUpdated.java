package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ResidentialMoveInDateUpdated extends DomainEvent {
    private final UUID enrollmentId;
    private final LocalDate moveInDate;

    public ResidentialMoveInDateUpdated(UUID enrollmentId, LocalDate moveInDate, Instant occurredAt) {
        super(enrollmentId, occurredAt);
        this.enrollmentId = enrollmentId;
        this.moveInDate = moveInDate;
    }

    @Override
    public String eventType() {
        return "program-enrollment.residential-move-in-date-updated";
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public LocalDate moveInDate() {
        return moveInDate;
    }

    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public LocalDate getMoveInDate() { return moveInDate; }
}