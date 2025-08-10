package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.UUID;

public record HouseholdMemberAdded(
    UUID clientId,
    UUID memberId,
    CodeableConcept relationship,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "HouseholdMemberAdded";
    }
}
