package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ServiceMarkedCourtOrdered extends DomainEvent {
    private final String courtOrderNumber;

    public ServiceMarkedCourtOrdered(UUID episodeId, String courtOrderNumber, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.courtOrderNumber = courtOrderNumber;
    }

    public String courtOrderNumber() {
        return courtOrderNumber;
    }


    // JavaBean-style getters
    public String getCourtOrderNumber() { return courtOrderNumber; }
}