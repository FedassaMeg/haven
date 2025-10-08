package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ServiceEpisodeLinked extends DomainEvent {
    private final UUID episodeId;
    private final String linkedBy;
    private final String reason;

    public ServiceEpisodeLinked(UUID caseId, UUID episodeId, String linkedBy, String reason, Instant occurredAt) {
        super(caseId, occurredAt != null ? occurredAt : Instant.now());
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (episodeId == null) throw new IllegalArgumentException("Episode ID cannot be null");
        if (linkedBy == null || linkedBy.trim().isEmpty()) throw new IllegalArgumentException("Linked by cannot be null or empty");

        this.episodeId = episodeId;
        this.linkedBy = linkedBy;
        this.reason = reason;
    }

    public UUID episodeId() {
        return episodeId;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String reason() {
        return reason;
    }


    // JavaBean-style getters
    public UUID getEpisodeId() { return episodeId; }
    public String getLinkedBy() { return linkedBy; }
    public String getReason() { return reason; }
}