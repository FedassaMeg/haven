package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.mandatedreport.ReportType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when mandated report is filed with appropriate agency
 */
public record MandatedReportFiled(
    UUID reportId,
    UUID caseId,
    UUID clientId,
    ReportType reportType,
    String reportNumber,
    String reportingAgency,
    String agencyContactInfo,
    String incidentDescription,
    Instant incidentDateTime,
    UUID filedByUserId,
    boolean isEmergencyReport,
    Instant filedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return filedAt;
    }
    
    @Override
    public String eventType() {
        return "MandatedReportFiled";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}