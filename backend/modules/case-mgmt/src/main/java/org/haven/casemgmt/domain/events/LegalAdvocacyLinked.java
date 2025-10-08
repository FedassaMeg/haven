package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class LegalAdvocacyLinked extends DomainEvent {
    private final UUID legalAdvocacyId;
    private final String linkedBy;
    private final String reason;

    public LegalAdvocacyLinked(UUID caseId, UUID legalAdvocacyId, String linkedBy, String reason, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (legalAdvocacyId == null) throw new IllegalArgumentException("Legal advocacy ID cannot be null");
        if (linkedBy == null || linkedBy.trim().isEmpty()) throw new IllegalArgumentException("Linked by cannot be null or empty");

        this.legalAdvocacyId = legalAdvocacyId;
        this.linkedBy = linkedBy;
        this.reason = reason;
    }

    public UUID legalAdvocacyId() {
        return legalAdvocacyId;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String reason() {
        return reason;
    }


    // JavaBean-style getters
    public UUID getLegalAdvocacyId() { return legalAdvocacyId; }
    public String getLinkedBy() { return linkedBy; }
    public String getReason() { return reason; }
}