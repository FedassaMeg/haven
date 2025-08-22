package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberRemoved(
    UUID compositionId,
    UUID membershipId,
    UUID memberId,
    LocalDate effectiveDate,
    String recordedBy,
    String reason,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return compositionId;
    }
    
    @Override
    public String eventType() {
        return "HouseholdMemberRemoved";
    }
}