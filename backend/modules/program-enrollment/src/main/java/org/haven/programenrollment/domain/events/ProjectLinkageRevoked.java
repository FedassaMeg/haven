package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectLinkageRevoked(
    UUID linkageId,
    LocalDate revocationDate,
    String revocationReason,
    String revokedBy,
    UUID revokedByUserId,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return linkageId;
    }

    @Override
    public String eventType() {
        return "project-linkage.revoked";
    }
}