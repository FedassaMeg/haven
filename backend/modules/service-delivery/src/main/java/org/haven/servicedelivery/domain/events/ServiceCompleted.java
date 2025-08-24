package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.servicedelivery.domain.ServiceEpisode.ServiceCompletionStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceCompleted(
    UUID episodeId,
    LocalDateTime endTime,
    Integer actualDurationMinutes,
    String outcome,
    ServiceCompletionStatus status,
    String notes,
    Double billableAmount,
    Instant occurredAt
) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return episodeId;
    }
    
    @Override
    public String eventType() {
        return "ServiceCompleted";
    }
}