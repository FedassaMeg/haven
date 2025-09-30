package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProjectLinkageModified(
    UUID linkageId,
    String newLinkageReason,
    String newLinkageNotes,
    String modifiedBy,
    UUID modifiedByUserId,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return linkageId;
    }

    @Override
    public String eventType() {
        return "project-linkage.modified";
    }
}