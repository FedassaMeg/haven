package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.servicedelivery.domain.ServiceEpisode.ServiceCompletionStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class ServiceCompleted extends DomainEvent {
    private final LocalDateTime endTime;
    private final Integer actualDurationMinutes;
    private final String outcome;
    private final ServiceCompletionStatus status;
    private final String notes;
    private final Double billableAmount;

    public ServiceCompleted(UUID episodeId, LocalDateTime endTime, Integer actualDurationMinutes, String outcome, ServiceCompletionStatus status, String notes, Double billableAmount, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.endTime = endTime;
        this.actualDurationMinutes = actualDurationMinutes;
        this.outcome = outcome;
        this.status = status;
        this.notes = notes;
        this.billableAmount = billableAmount;
    }

    public LocalDateTime endTime() {
        return endTime;
    }

    public Integer actualDurationMinutes() {
        return actualDurationMinutes;
    }

    public String outcome() {
        return outcome;
    }

    public ServiceCompletionStatus status() {
        return status;
    }

    public String notes() {
        return notes;
    }

    public Double billableAmount() {
        return billableAmount;
    }


    // JavaBean-style getters
    public LocalDateTime getEndTime() { return endTime; }
    public Integer getActualDurationMinutes() { return actualDurationMinutes; }
    public String getOutcome() { return outcome; }
    public ServiceCompletionStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Double getBillableAmount() { return billableAmount; }
}