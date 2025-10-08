package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.mandatedreport.ReportType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a new mandated report is created
 */

public class MandatedReportCreated extends DomainEvent {
    private final UUID caseId;
    private final UUID clientId;
    private final ReportType reportType;
    private final String reportNumber;
    private final String reportingAgency;
    private final String incidentDescription;
    private final Instant filingDeadline;
    private final UUID createdByUserId;
    private final boolean isEmergencyReport;
    private final String legalJustification;
    private final Instant createdAt;

    public MandatedReportCreated(UUID reportId, UUID caseId, UUID clientId, ReportType reportType, String reportNumber, String reportingAgency, String incidentDescription, Instant incidentDateTime, Instant filingDeadline, UUID createdByUserId, boolean isEmergencyReport, String legalJustification, Instant createdAt) {
        super(reportId, incidentDateTime);
        this.caseId = caseId;
        this.clientId = clientId;
        this.reportType = reportType;
        this.reportNumber = reportNumber;
        this.reportingAgency = reportingAgency;
        this.incidentDescription = incidentDescription;
        this.filingDeadline = filingDeadline;
        this.createdByUserId = createdByUserId;
        this.isEmergencyReport = isEmergencyReport;
        this.legalJustification = legalJustification;
        this.createdAt = createdAt;
    }

    public UUID caseId() {
        return caseId;
    }

    public UUID clientId() {
        return clientId;
    }

    public ReportType reportType() {
        return reportType;
    }

    public String reportNumber() {
        return reportNumber;
    }

    public String reportingAgency() {
        return reportingAgency;
    }

    public String incidentDescription() {
        return incidentDescription;
    }

    public Instant filingDeadline() {
        return filingDeadline;
    }

    public UUID createdByUserId() {
        return createdByUserId;
    }

    public boolean isEmergencyReport() {
        return isEmergencyReport;
    }

    public String legalJustification() {
        return legalJustification;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public UUID reportId() {
        return getAggregateId();
    }

    public Instant incidentDateTime() {
        return getOccurredOn();
    }

    @Override
    public String eventType() {
        return "MandatedReportCreated";
    }


    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public UUID getClientId() { return clientId; }
    public ReportType getReportType() { return reportType; }
    public String getReportNumber() { return reportNumber; }
    public String getReportingAgency() { return reportingAgency; }
    public String getIncidentDescription() { return incidentDescription; }
    public Instant getFilingDeadline() { return filingDeadline; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public boolean IsEmergencyReport() { return isEmergencyReport; }
    public String getLegalJustification() { return legalJustification; }
    public Instant getCreatedAt() { return createdAt; }
}