package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when mandated report becomes overdue
 */

public class MandatedReportOverdue extends DomainEvent {
    private final UUID caseId;
    private final Instant overdueAt;

    public MandatedReportOverdue(UUID reportId, UUID caseId, Instant filingDeadline, Instant overdueAt) {
        super(reportId, filingDeadline);
        this.caseId = caseId;
        this.overdueAt = overdueAt;
    }

    public UUID caseId() {
        return caseId;
    }

    public Instant overdueAt() {
        return overdueAt;
    }


    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public Instant getOverdueAt() { return overdueAt; }
}