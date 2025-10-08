package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EnrollmentOpened extends DomainEvent {
    private final UUID clientId;
    private final UUID caseId;
    private final UUID programId;
    private final String programName;
    private final LocalDate enrollmentDate;
    private final String enrolledBy;
    private final String entryPoint;

    public EnrollmentOpened(UUID enrollmentId, UUID clientId, UUID caseId, UUID programId, String programName, LocalDate enrollmentDate, String enrolledBy, String entryPoint, Instant occurredAt) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (programId == null) throw new IllegalArgumentException("Program ID cannot be null");
        if (enrollmentDate == null) throw new IllegalArgumentException("Enrollment date cannot be null");
        if (enrolledBy == null || enrolledBy.trim().isEmpty()) throw new IllegalArgumentException("Enrolled by cannot be null or empty");

        this.clientId = clientId;
        this.caseId = caseId;
        this.programId = programId;
        this.programName = programName;
        this.enrollmentDate = enrollmentDate;
        this.enrolledBy = enrolledBy;
        this.entryPoint = entryPoint;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID caseId() {
        return caseId;
    }

    public UUID programId() {
        return programId;
    }

    public String programName() {
        return programName;
    }

    public LocalDate enrollmentDate() {
        return enrollmentDate;
    }

    public String enrolledBy() {
        return enrolledBy;
    }

    public String entryPoint() {
        return entryPoint;
    }


    public UUID enrollmentId() {
        return getAggregateId();
    }

    @Override
    public String eventType() {
        return "EnrollmentOpened";
    }

    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getCaseId() { return caseId; }
    public UUID getProgramId() { return programId; }
    public String getProgramName() { return programName; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public String getEnrolledBy() { return enrolledBy; }
    public String getEntryPoint() { return entryPoint; }
}