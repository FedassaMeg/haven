package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProjectLinkageRevoked extends DomainEvent {
    private final UUID linkageId;
    private final LocalDate revocationDate;
    private final String revocationReason;
    private final String revokedBy;
    private final UUID revokedByUserId;

    public ProjectLinkageRevoked(
        UUID linkageId,
        LocalDate revocationDate,
        String revocationReason,
        String revokedBy,
        UUID revokedByUserId,
        Instant occurredAt
    ) {
        super(linkageId, occurredAt);
        this.linkageId = linkageId;
        this.revocationDate = revocationDate;
        this.revocationReason = revocationReason;
        this.revokedBy = revokedBy;
        this.revokedByUserId = revokedByUserId;
    }

    @Override
    public String eventType() {
        return "project-linkage.revoked";
    }

    public UUID linkageId() {
        return linkageId;
    }

    public LocalDate revocationDate() {
        return revocationDate;
    }

    public String revocationReason() {
        return revocationReason;
    }

    public String revokedBy() {
        return revokedBy;
    }

    public UUID revokedByUserId() {
        return revokedByUserId;
    }

    // JavaBean-style getters
    public UUID getLinkageId() { return linkageId; }
    public LocalDate getRevocationDate() { return revocationDate; }
    public String getRevocationReason() { return revocationReason; }
    public String getRevokedBy() { return revokedBy; }
    public UUID getRevokedByUserId() { return revokedByUserId; }
}