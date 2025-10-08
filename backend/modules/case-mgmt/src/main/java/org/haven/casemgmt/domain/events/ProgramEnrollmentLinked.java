package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ProgramEnrollmentLinked extends DomainEvent {
    private final UUID enrollmentId;
    private final String linkedBy;
    private final String linkageReason;

    public ProgramEnrollmentLinked(UUID caseId, UUID enrollmentId, String linkedBy, String linkageReason, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (linkedBy == null) throw new IllegalArgumentException("Linked by cannot be null");

        this.enrollmentId = enrollmentId;
        this.linkedBy = linkedBy;
        this.linkageReason = linkageReason;
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String linkageReason() {
        return linkageReason;
    }


    // JavaBean-style getters
    public UUID getEnrollmentId() { return enrollmentId; }
    public String getLinkedBy() { return linkedBy; }
    public String getLinkageReason() { return linkageReason; }
}