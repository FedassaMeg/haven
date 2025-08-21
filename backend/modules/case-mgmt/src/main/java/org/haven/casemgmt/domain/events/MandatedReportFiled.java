package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MandatedReportFiled(
    UUID caseId,
    UUID clientId,
    UUID reportId,
    CodeableConcept reportType,
    LocalDate filedDate,
    String reportingAgency,
    String reportNumber,
    String incidentDescription,
    String reportedBy,
    UUID reportedByUserId,
    boolean isEmergencyReport,
    String followUpRequired,
    Instant occurredAt
) implements DomainEvent {
    
    public MandatedReportFiled {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (reportId == null) throw new IllegalArgumentException("Report ID cannot be null");
        if (reportType == null) throw new IllegalArgumentException("Report type cannot be null");
        if (filedDate == null) throw new IllegalArgumentException("Filed date cannot be null");
        if (reportingAgency == null || reportingAgency.trim().isEmpty()) throw new IllegalArgumentException("Reporting agency cannot be null or empty");
        if (reportedBy == null || reportedBy.trim().isEmpty()) throw new IllegalArgumentException("Reported by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "MandatedReportFiled";
    }
}