package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustodyChangeRecorded(
    UUID compositionId,
    UUID childId,
    CodeableConcept newCustodyRelationship,
    LocalDate effectiveDate,
    String courtOrder,
    String recordedBy,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return compositionId;
    }
    
    @Override
    public String eventType() {
        return "CustodyChangeRecorded";
    }
}