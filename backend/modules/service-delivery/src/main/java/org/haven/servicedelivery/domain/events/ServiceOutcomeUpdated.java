package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ServiceOutcomeUpdated extends DomainEvent {
    private final String outcome;
    private final String followUpRequired;
    private final LocalDate followUpDate;
    private final String updatedBy;

    public ServiceOutcomeUpdated(UUID episodeId, String outcome, String followUpRequired, LocalDate followUpDate, String updatedBy, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.outcome = outcome;
        this.followUpRequired = followUpRequired;
        this.followUpDate = followUpDate;
        this.updatedBy = updatedBy;
    }

    public String outcome() {
        return outcome;
    }

    public String followUpRequired() {
        return followUpRequired;
    }

    public LocalDate followUpDate() {
        return followUpDate;
    }

    public String updatedBy() {
        return updatedBy;
    }


    // JavaBean-style getters
    public String getOutcome() { return outcome; }
    public String getFollowUpRequired() { return followUpRequired; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public String getUpdatedBy() { return updatedBy; }
}