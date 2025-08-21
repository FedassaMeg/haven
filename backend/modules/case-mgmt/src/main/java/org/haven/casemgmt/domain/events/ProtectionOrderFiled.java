package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProtectionOrderFiled(
    UUID caseId,
    UUID clientId,
    UUID protectionOrderId,
    CodeableConcept orderType,
    LocalDate filedDate,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    String courtName,
    String judgeOrCommissioner,
    String caseNumber,
    String protectedParties,
    String restrainedParties,
    String orderConditions,
    boolean isTemporary,
    String filedBy,
    UUID filedByUserId,
    Instant occurredAt
) implements DomainEvent {
    
    public ProtectionOrderFiled {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (protectionOrderId == null) throw new IllegalArgumentException("Protection order ID cannot be null");
        if (orderType == null) throw new IllegalArgumentException("Order type cannot be null");
        if (filedDate == null) throw new IllegalArgumentException("Filed date cannot be null");
        if (courtName == null || courtName.trim().isEmpty()) throw new IllegalArgumentException("Court name cannot be null or empty");
        if (protectedParties == null || protectedParties.trim().isEmpty()) throw new IllegalArgumentException("Protected parties cannot be null or empty");
        if (restrainedParties == null || restrainedParties.trim().isEmpty()) throw new IllegalArgumentException("Restrained parties cannot be null or empty");
        if (filedBy == null || filedBy.trim().isEmpty()) throw new IllegalArgumentException("Filed by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "ProtectionOrderFiled";
    }
}