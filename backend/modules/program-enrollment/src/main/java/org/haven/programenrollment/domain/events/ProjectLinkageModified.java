package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class ProjectLinkageModified extends DomainEvent {
    private final UUID linkageId;
    private final String newLinkageReason;
    private final String newLinkageNotes;
    private final String modifiedBy;
    private final UUID modifiedByUserId;

    public ProjectLinkageModified(
        UUID linkageId,
        String newLinkageReason,
        String newLinkageNotes,
        String modifiedBy,
        UUID modifiedByUserId,
        Instant occurredAt
    ) {
        super(linkageId, occurredAt);
        this.linkageId = linkageId;
        this.newLinkageReason = newLinkageReason;
        this.newLinkageNotes = newLinkageNotes;
        this.modifiedBy = modifiedBy;
        this.modifiedByUserId = modifiedByUserId;
    }

    @Override
    public String eventType() {
        return "project-linkage.modified";
    }

    public UUID linkageId() {
        return linkageId;
    }

    public String newLinkageReason() {
        return newLinkageReason;
    }

    public String newLinkageNotes() {
        return newLinkageNotes;
    }

    public String modifiedBy() {
        return modifiedBy;
    }

    public UUID modifiedByUserId() {
        return modifiedByUserId;
    }

    // JavaBean-style getters
    public UUID getLinkageId() { return linkageId; }
    public String getNewLinkageReason() { return newLinkageReason; }
    public String getNewLinkageNotes() { return newLinkageNotes; }
    public String getModifiedBy() { return modifiedBy; }
    public UUID getModifiedByUserId() { return modifiedByUserId; }
}