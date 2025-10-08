package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class MandatedReportFiled extends DomainEvent {
    private final UUID clientId;
    private final UUID reportId;
    private final CodeableConcept reportType;
    private final LocalDate filedDate;
    private final String reportingAgency;
    private final String reportNumber;
    private final String incidentDescription;
    private final String reportedBy;
    private final UUID reportedByUserId;
    private final boolean isEmergencyReport;
    private final String followUpRequired;

    public MandatedReportFiled(UUID caseId, UUID clientId, UUID reportId, CodeableConcept reportType, LocalDate filedDate, String reportingAgency, String reportNumber, String incidentDescription, String reportedBy, UUID reportedByUserId, boolean isEmergencyReport, String followUpRequired, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (reportType == null) throw new IllegalArgumentException("Report type cannot be null");
        if (filedDate == null) throw new IllegalArgumentException("Filed date cannot be null");
        if (reportingAgency == null || reportingAgency.trim().isEmpty()) throw new IllegalArgumentException("Reporting agency cannot be null or empty");
        if (reportedBy == null || reportedBy.trim().isEmpty()) throw new IllegalArgumentException("Reported by cannot be null or empty");

        this.clientId = clientId;
        this.reportId = reportId;
        this.reportType = reportType;
        this.filedDate = filedDate;
        this.reportingAgency = reportingAgency;
        this.reportNumber = reportNumber;
        this.incidentDescription = incidentDescription;
        this.reportedBy = reportedBy;
        this.reportedByUserId = reportedByUserId;
        this.isEmergencyReport = isEmergencyReport;
        this.followUpRequired = followUpRequired;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID reportId() {
        return reportId;
    }

    public CodeableConcept reportType() {
        return reportType;
    }

    public LocalDate filedDate() {
        return filedDate;
    }

    public String reportingAgency() {
        return reportingAgency;
    }

    public String reportNumber() {
        return reportNumber;
    }

    public String incidentDescription() {
        return incidentDescription;
    }

    public String reportedBy() {
        return reportedBy;
    }

    public UUID reportedByUserId() {
        return reportedByUserId;
    }

    public boolean isEmergencyReport() {
        return isEmergencyReport;
    }

    public String followUpRequired() {
        return followUpRequired;
    }


    public UUID caseId() {
        return getAggregateId();
    }

    @Override
    public String eventType() {
        return "MandatedReportFiled";
    }

    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getReportId() { return reportId; }
    public CodeableConcept getReportType() { return reportType; }
    public LocalDate getFiledDate() { return filedDate; }
    public String getReportingAgency() { return reportingAgency; }
    public String getReportNumber() { return reportNumber; }
    public String getIncidentDescription() { return incidentDescription; }
    public String getReportedBy() { return reportedBy; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public boolean IsEmergencyReport() { return isEmergencyReport; }
    public String getFollowUpRequired() { return followUpRequired; }
}