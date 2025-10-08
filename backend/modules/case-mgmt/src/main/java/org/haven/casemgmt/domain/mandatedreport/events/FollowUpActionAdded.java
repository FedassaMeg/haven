package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when a follow-up action is added to a mandated report
 */

public class FollowUpActionAdded extends DomainEvent {
    private final String action;
    private final UUID addedByUserId;

    public FollowUpActionAdded(UUID reportId, String action, UUID addedByUserId, Instant addedAt) {
        super(reportId, addedAt);
        this.action = action;
        this.addedByUserId = addedByUserId;
    }

    public String action() {
        return action;
    }

    public UUID addedByUserId() {
        return addedByUserId;
    }


    // JavaBean-style getters
    public String getAction() { return action; }
    public UUID getAddedByUserId() { return addedByUserId; }
}