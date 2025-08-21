package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProtectionOrderUpdated(
    UUID caseId,
    UUID clientId,
    UUID protectionOrderId,
    LocalDate updateDate,
    String updateType,
    CodeableConcept updateReason,
    LocalDate newExpirationDate,
    String updatedConditions,
    String courtName,
    String updatedBy,
    UUID updatedByUserId,
    String updateNotes,
    Instant occurredAt
) implements DomainEvent {
    
    public ProtectionOrderUpdated {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (protectionOrderId == null) throw new IllegalArgumentException("Protection order ID cannot be null");
        if (updateDate == null) throw new IllegalArgumentException("Update date cannot be null");
        if (updateType == null || updateType.trim().isEmpty()) throw new IllegalArgumentException("Update type cannot be null or empty");
        if (updatedBy == null || updatedBy.trim().isEmpty()) throw new IllegalArgumentException("Updated by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "ProtectionOrderUpdated";
    }
}