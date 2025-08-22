package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a follow-up action is added to a mandated report
 */
public record FollowUpActionAdded(
    UUID reportId,
    String action,
    UUID addedByUserId,
    Instant addedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return addedAt;
    }
    
    @Override
    public String eventType() {
        return "FollowUpActionAdded";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}