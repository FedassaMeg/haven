package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CPSAPSResponseLogged(
    UUID caseId,
    UUID clientId,
    UUID mandatedReportId,
    String agencyName,
    String caseworkerName,
    String caseworkerContact,
    LocalDate responseDate,
    CodeableConcept responseType,
    String investigationStatus,
    String findingsOverview,
    String actionsRequired,
    String followUpDate,
    String responseNotes,
    String loggedBy,
    UUID loggedByUserId,
    boolean requiresImmediateAction,
    Instant occurredAt
) implements DomainEvent {
    
    public CPSAPSResponseLogged {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (mandatedReportId == null) throw new IllegalArgumentException("Mandated report ID cannot be null");
        if (agencyName == null || agencyName.trim().isEmpty()) throw new IllegalArgumentException("Agency name cannot be null or empty");
        if (responseDate == null) throw new IllegalArgumentException("Response date cannot be null");
        if (responseType == null) throw new IllegalArgumentException("Response type cannot be null");
        if (loggedBy == null || loggedBy.trim().isEmpty()) throw new IllegalArgumentException("Logged by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "CPSAPSResponseLogged";
    }
}