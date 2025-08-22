package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when agency response or investigation outcome is recorded
 */
public record AgencyResponseRecorded(
    UUID reportId,
    UUID caseId,
    String response,
    String investigationOutcome,
    UUID recordedByUserId,
    Instant recordedAt
) implements DomainEvent {
    
    @Override
    public Instant occurredAt() {
        return recordedAt;
    }
    
    @Override
    public String eventType() {
        return "AgencyResponseRecorded";
    }
    
    @Override
    public UUID aggregateId() {
        return reportId;
    }
}