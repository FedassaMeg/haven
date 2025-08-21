package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ServiceEpisodeLinked(
    UUID caseId,
    UUID episodeId,
    String linkedBy,
    String reason,
    Instant occurredAt
) implements DomainEvent {
    
    public ServiceEpisodeLinked {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (episodeId == null) throw new IllegalArgumentException("Episode ID cannot be null");
        if (linkedBy == null || linkedBy.trim().isEmpty()) throw new IllegalArgumentException("Linked by cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "ServiceEpisodeLinked";
    }
}