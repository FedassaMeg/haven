package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record HearingScheduled(
    UUID legalAdvocacyId,
    UUID clientId,
    UUID caseId,
    LocalDateTime hearingDateTime,
    String courtName,
    String hearingType,
    String purpose,
    String judgeName,
    String address,
    String scheduledBy,
    String notes,
    Instant occurredAt
) implements DomainEvent {
    
    public HearingScheduled {
        if (legalAdvocacyId == null) throw new IllegalArgumentException("Legal advocacy ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (hearingDateTime == null) throw new IllegalArgumentException("Hearing date/time cannot be null");
        if (courtName == null || courtName.trim().isEmpty()) throw new IllegalArgumentException("Court name cannot be null or empty");
        if (hearingType == null || hearingType.trim().isEmpty()) throw new IllegalArgumentException("Hearing type cannot be null or empty");
        if (scheduledBy == null || scheduledBy.trim().isEmpty()) throw new IllegalArgumentException("Scheduled by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return legalAdvocacyId;
    }
    
    @Override
    public String eventType() {
        return "HearingScheduled";
    }
}