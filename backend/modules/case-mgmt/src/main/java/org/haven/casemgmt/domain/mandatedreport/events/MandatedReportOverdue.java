package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when mandated report becomes overdue
 */
public record MandatedReportOverdue(
    UUID reportId,
    UUID caseId,
    Instant filingDeadline,
    Instant overdueAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return overdueAt;
    }
    
    @Override
    public String eventType() {
        return "MandatedReportOverdue";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}