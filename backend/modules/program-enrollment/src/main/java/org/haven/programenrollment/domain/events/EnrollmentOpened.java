package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EnrollmentOpened extends DomainEvent {
    private final UUID enrollmentId;
    private final UUID clientId;
    private final UUID programId;
    private final LocalDate enrollmentDate;
    private final CodeableConcept relationshipToHead;
    private final CodeableConcept residencePriorToEntry;
    private final String entryFrom;

    public EnrollmentOpened(
        UUID enrollmentId,
        UUID clientId,
        UUID programId,
        LocalDate enrollmentDate,
        CodeableConcept relationshipToHead,
        CodeableConcept residencePriorToEntry,
        String entryFrom,
        Instant occurredAt
    ) {
        super(enrollmentId, occurredAt != null ? occurredAt : Instant.now());
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (programId == null) throw new IllegalArgumentException("Program ID cannot be null");
        if (enrollmentDate == null) throw new IllegalArgumentException("Enrollment date cannot be null");

        this.enrollmentId = enrollmentId;
        this.clientId = clientId;
        this.programId = programId;
        this.enrollmentDate = enrollmentDate;
        this.relationshipToHead = relationshipToHead;
        this.residencePriorToEntry = residencePriorToEntry;
        this.entryFrom = entryFrom;
    }

    @Override
    public String eventType() {
        return "EnrollmentOpened";
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

    public LocalDate enrollmentDate() {
        return enrollmentDate;
    }

    public CodeableConcept relationshipToHead() {
        return relationshipToHead;
    }

    public CodeableConcept residencePriorToEntry() {
        return residencePriorToEntry;
    }

    public String entryFrom() {
        return entryFrom;
    }

    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getClientId() { return clientId; }
    public UUID getProgramId() { return programId; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public CodeableConcept getRelationshipToHead() { return relationshipToHead; }
    public CodeableConcept getResidencePriorToEntry() { return residencePriorToEntry; }
    public String getEntryFrom() { return entryFrom; }
}