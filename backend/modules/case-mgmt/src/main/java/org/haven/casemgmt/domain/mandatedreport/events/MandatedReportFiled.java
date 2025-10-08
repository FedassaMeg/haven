package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.mandatedreport.ReportType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when mandated report is filed with appropriate agency
 */

public class MandatedReportFiled extends DomainEvent {
    private final UUID caseId;
    private final UUID clientId;
    private final ReportType reportType;
    private final String reportNumber;
    private final String reportingAgency;
    private final String agencyContactInfo;
    private final String incidentDescription;
    private final UUID filedByUserId;
    private final boolean isEmergencyReport;
    private final Instant filedAt;

    public MandatedReportFiled(UUID reportId, UUID caseId, UUID clientId, ReportType reportType, String reportNumber, String reportingAgency, String agencyContactInfo, String incidentDescription, Instant incidentDateTime, UUID filedByUserId, boolean isEmergencyReport, Instant filedAt) {
        super(reportId, incidentDateTime);
        this.caseId = caseId;
        this.clientId = clientId;
        this.reportType = reportType;
        this.reportNumber = reportNumber;
        this.reportingAgency = reportingAgency;
        this.agencyContactInfo = agencyContactInfo;
        this.incidentDescription = incidentDescription;
        this.filedByUserId = filedByUserId;
        this.isEmergencyReport = isEmergencyReport;
        this.filedAt = filedAt;
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

    public String agencyContactInfo() {
        return agencyContactInfo;
    }

    public String incidentDescription() {
        return incidentDescription;
    }

    public UUID filedByUserId() {
        return filedByUserId;
    }

    public boolean isEmergencyReport() {
        return isEmergencyReport;
    }

    public Instant filedAt() {
        return filedAt;
    }


    public UUID reportId() {
        return getAggregateId();
    }

    public Instant incidentDateTime() {
        return getOccurredOn();
    }

    @Override
    public String eventType() {
        return "MandatedReportFiled";
    }

    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public UUID getClientId() { return clientId; }
    public ReportType getReportType() { return reportType; }
    public String getReportNumber() { return reportNumber; }
    public String getReportingAgency() { return reportingAgency; }
    public String getAgencyContactInfo() { return agencyContactInfo; }
    public String getIncidentDescription() { return incidentDescription; }
    public UUID getFiledByUserId() { return filedByUserId; }
    public boolean IsEmergencyReport() { return isEmergencyReport; }
    public Instant getFiledAt() { return filedAt; }
}