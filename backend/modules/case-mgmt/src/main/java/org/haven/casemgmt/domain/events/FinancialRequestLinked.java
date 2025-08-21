package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record FinancialRequestLinked(
    UUID caseId,
    UUID requestId,
    String linkedBy,
    String reason,
    Instant occurredAt
) implements DomainEvent {
    
    public FinancialRequestLinked {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (linkedBy == null || linkedBy.trim().isEmpty()) throw new IllegalArgumentException("Linked by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "FinancialRequestLinked";
    }
}