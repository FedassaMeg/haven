package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class CaseClosed extends DomainEvent {
    private final UUID caseId;
    private final String reason;

    public CaseClosed(UUID caseId, String reason, Instant occurredAt) {
        super(caseId, occurredAt);
        this.caseId = caseId;
        this.reason = reason;
    }

    @Override
    public String eventType() {
        return "CaseClosed";
    }

    public UUID caseId() {
        return caseId;
    }

    public String reason() {
        return reason;
    }

    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public String getReason() { return reason; }
}