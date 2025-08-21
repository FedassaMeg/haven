package org.haven.housingassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HousingAssistanceApproved(
    UUID housingAssistanceId,
    BigDecimal approvedAmount,
    Integer approvedDurationMonths,
    String fundingSourceCode,
    String finalApproverId,
    String approvalNotes,
    Instant occurredAt
) implements DomainEvent {
    
    public HousingAssistanceApproved {
        if (housingAssistanceId == null) throw new IllegalArgumentException("Housing assistance ID cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (fundingSourceCode == null) throw new IllegalArgumentException("Funding source code cannot be null");
        if (finalApproverId == null) throw new IllegalArgumentException("Final approver ID cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return housingAssistanceId;
    }
    
    @Override
    public String eventType() {
        return "HousingAssistanceApproved";
    }
}