package org.haven.clientprofile.domain.events;

import org.haven.clientprofile.domain.HouseholdComposition.HouseholdType;
import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HouseholdCompositionCreated(
    UUID compositionId,
    UUID headOfHouseholdId,
    LocalDate effectiveDate,
    HouseholdType householdType,
    String recordedBy,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return compositionId;
    }
    
    @Override
    public String eventType() {
        return "HouseholdCompositionCreated";
    }
}