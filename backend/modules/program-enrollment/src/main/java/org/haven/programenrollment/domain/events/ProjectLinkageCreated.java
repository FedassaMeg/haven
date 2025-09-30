package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectLinkageCreated(
    UUID linkageId,
    UUID thProjectId,
    UUID rrhProjectId,
    String thHudProjectId,
    String rrhHudProjectId,
    String thProjectName,
    String rrhProjectName,
    LocalDate effectiveDate,
    String linkageReason,
    String authorizedBy,
    UUID authorizedByUserId,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return linkageId;
    }

    @Override
    public String eventType() {
        return "project-linkage.created";
    }
}