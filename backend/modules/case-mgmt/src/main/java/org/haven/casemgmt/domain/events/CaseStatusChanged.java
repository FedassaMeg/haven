package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.CaseRecord.CaseStatus;
import java.time.Instant;
import java.util.UUID;

public class CaseStatusChanged extends DomainEvent {
    private final CaseStatus oldStatus;
    private final CaseStatus newStatus;

    public CaseStatusChanged(UUID caseId, CaseStatus oldStatus, CaseStatus newStatus, Instant occurredAt) {
        super(caseId, occurredAt);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public CaseStatus oldStatus() {
        return oldStatus;
    }

    public CaseStatus newStatus() {
        return newStatus;
    }


    // JavaBean-style getters
    public CaseStatus getOldStatus() { return oldStatus; }
    public CaseStatus getNewStatus() { return newStatus; }
}