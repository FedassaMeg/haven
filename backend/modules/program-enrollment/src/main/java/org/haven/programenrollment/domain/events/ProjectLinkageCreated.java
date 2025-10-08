package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProjectLinkageCreated extends DomainEvent {
    private final UUID linkageId;
    private final UUID thProjectId;
    private final UUID rrhProjectId;
    private final String thHudProjectId;
    private final String rrhHudProjectId;
    private final String thProjectName;
    private final String rrhProjectName;
    private final LocalDate effectiveDate;
    private final String linkageReason;
    private final String authorizedBy;
    private final UUID authorizedByUserId;

    public ProjectLinkageCreated(
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
    ) {
        super(linkageId, occurredAt);
        this.linkageId = linkageId;
        this.thProjectId = thProjectId;
        this.rrhProjectId = rrhProjectId;
        this.thHudProjectId = thHudProjectId;
        this.rrhHudProjectId = rrhHudProjectId;
        this.thProjectName = thProjectName;
        this.rrhProjectName = rrhProjectName;
        this.effectiveDate = effectiveDate;
        this.linkageReason = linkageReason;
        this.authorizedBy = authorizedBy;
        this.authorizedByUserId = authorizedByUserId;
    }

    @Override
    public String eventType() {
        return "project-linkage.created";
    }

    public UUID linkageId() {
        return linkageId;
    }

    public UUID thProjectId() {
        return thProjectId;
    }

    public UUID rrhProjectId() {
        return rrhProjectId;
    }

    public String thHudProjectId() {
        return thHudProjectId;
    }

    public String rrhHudProjectId() {
        return rrhHudProjectId;
    }

    public String thProjectName() {
        return thProjectName;
    }

    public String rrhProjectName() {
        return rrhProjectName;
    }

    public LocalDate effectiveDate() {
        return effectiveDate;
    }

    public String linkageReason() {
        return linkageReason;
    }

    public String authorizedBy() {
        return authorizedBy;
    }

    public UUID authorizedByUserId() {
        return authorizedByUserId;
    }

    // JavaBean-style getters
    public UUID getLinkageId() { return linkageId; }
    public UUID getThProjectId() { return thProjectId; }
    public UUID getRrhProjectId() { return rrhProjectId; }
    public String getThHudProjectId() { return thHudProjectId; }
    public String getRrhHudProjectId() { return rrhHudProjectId; }
    public String getThProjectName() { return thProjectName; }
    public String getRrhProjectName() { return rrhProjectName; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getLinkageReason() { return linkageReason; }
    public String getAuthorizedBy() { return authorizedBy; }
    public UUID getAuthorizedByUserId() { return authorizedByUserId; }
}