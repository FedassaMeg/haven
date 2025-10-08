package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class FinancialRequestLinked extends DomainEvent {
    private final UUID requestId;
    private final String linkedBy;
    private final String reason;

    public FinancialRequestLinked(UUID caseId, UUID requestId, String linkedBy, String reason, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (linkedBy == null || linkedBy.trim().isEmpty()) throw new IllegalArgumentException("Linked by cannot be null or empty");

        this.requestId = requestId;
        this.linkedBy = linkedBy;
        this.reason = reason;
    }

    public UUID requestId() {
        return requestId;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String reason() {
        return reason;
    }


    // JavaBean-style getters
    public UUID getRequestId() { return requestId; }
    public String getLinkedBy() { return linkedBy; }
    public String getReason() { return reason; }
}