package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class SafetyPlanLinked extends DomainEvent {
    private final UUID safetyPlanId;
    private final String linkedBy;
    private final String reason;

    public SafetyPlanLinked(UUID caseId, UUID safetyPlanId, String linkedBy, String reason, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (safetyPlanId == null) throw new IllegalArgumentException("Safety plan ID cannot be null");
        if (linkedBy == null || linkedBy.trim().isEmpty()) throw new IllegalArgumentException("Linked by cannot be null or empty");

        this.safetyPlanId = safetyPlanId;
        this.linkedBy = linkedBy;
        this.reason = reason;
    }

    public UUID safetyPlanId() {
        return safetyPlanId;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String reason() {
        return reason;
    }


    // JavaBean-style getters
    public UUID getSafetyPlanId() { return safetyPlanId; }
    public String getLinkedBy() { return linkedBy; }
    public String getReason() { return reason; }
}