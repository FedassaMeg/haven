package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.mandatedreport.ReportStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when mandated report status is updated
 */
public record MandatedReportStatusUpdated(
    UUID reportId,
    UUID caseId,
    ReportStatus previousStatus,
    ReportStatus newStatus,
    String statusReason,
    UUID updatedByUserId,
    Instant updatedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return updatedAt;
    }
    
    @Override
    public String eventType() {
        return "MandatedReportStatusUpdated";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}