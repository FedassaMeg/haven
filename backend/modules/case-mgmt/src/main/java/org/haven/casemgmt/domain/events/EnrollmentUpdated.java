package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EnrollmentUpdated extends DomainEvent {
    private final UUID clientId;
    private final LocalDate expectedExitDate;
    private final String notes;
    private final String updatedBy;

    public EnrollmentUpdated(UUID enrollmentId, UUID clientId, LocalDate expectedExitDate, String notes, String updatedBy, Instant occurredAt) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");

        this.clientId = clientId;
        this.expectedExitDate = expectedExitDate;
        this.notes = notes;
        this.updatedBy = updatedBy;
    }

    public UUID clientId() {
        return clientId;
    }

    public LocalDate expectedExitDate() {
        return expectedExitDate;
    }

    public String notes() {
        return notes;
    }

    public String updatedBy() {
        return updatedBy;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public LocalDate getExpectedExitDate() { return expectedExitDate; }
    public String getNotes() { return notes; }
    public String getUpdatedBy() { return updatedBy; }
}