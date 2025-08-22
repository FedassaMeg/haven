package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.mandatedreport.ReportType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a new mandated report is created
 */
public record MandatedReportCreated(
    UUID reportId,
    UUID caseId,
    UUID clientId,
    ReportType reportType,
    String reportNumber,
    String reportingAgency,
    String incidentDescription,
    Instant incidentDateTime,
    Instant filingDeadline,
    UUID createdByUserId,
    boolean isEmergencyReport,
    String legalJustification,
    Instant createdAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return createdAt;
    }
    
    @Override
    public String eventType() {
        return "MandatedReportCreated";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}