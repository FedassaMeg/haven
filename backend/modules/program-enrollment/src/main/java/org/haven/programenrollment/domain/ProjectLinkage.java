package org.haven.programenrollment.domain;

import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.hmis.HmisProjectType;
import org.haven.programenrollment.domain.events.ProjectLinkageCreated;
import org.haven.programenrollment.domain.events.ProjectLinkageModified;
import org.haven.programenrollment.domain.events.ProjectLinkageRevoked;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents linkage metadata between TH and RRH projects
 * Manages predecessor household references and HUD project identifiers
 */
public class ProjectLinkage extends AggregateRoot<ProjectLinkageId> {

    private UUID thProjectId;
    private UUID rrhProjectId;
    private String thHudProjectId;
    private String rrhHudProjectId;
    private String thProjectName;
    private String rrhProjectName;

    // Linkage metadata
    private LocalDate linkageEffectiveDate;
    private LocalDate linkageEndDate;
    private LinkageStatus status;
    private String linkageReason;
    private String linkageNotes;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
    private Instant createdAt;
    private Instant lastModifiedAt;

    // HUD compliance fields
    private String linkageAuthorization;
    private UUID authorizedByUserId;
    private LocalDate authorizationDate;

    public static ProjectLinkage create(
            UUID thProjectId,
            UUID rrhProjectId,
            String thHudProjectId,
            String rrhHudProjectId,
            String thProjectName,
            String rrhProjectName,
            LocalDate effectiveDate,
            String linkageReason,
            String authorizedBy,
            UUID authorizedByUserId) {

        ProjectLinkageId linkageId = ProjectLinkageId.generate();
        ProjectLinkage linkage = new ProjectLinkage();

        linkage.apply(new ProjectLinkageCreated(
            linkageId.value(),
            thProjectId,
            rrhProjectId,
            thHudProjectId,
            rrhHudProjectId,
            thProjectName,
            rrhProjectName,
            effectiveDate,
            linkageReason,
            authorizedBy,
            authorizedByUserId,
            Instant.now()
        ));

        return linkage;
    }

    /**
     * Modify linkage details
     */
    public void modifyLinkage(String newLinkageReason,
                             String newLinkageNotes,
                             String modifiedBy,
                             UUID modifiedByUserId) {
        if (status == LinkageStatus.REVOKED) {
            throw new IllegalStateException("Cannot modify revoked linkage");
        }

        apply(new ProjectLinkageModified(
            id.value(),
            newLinkageReason,
            newLinkageNotes,
            modifiedBy,
            modifiedByUserId,
            Instant.now()
        ));
    }

    /**
     * Revoke the linkage
     */
    public void revokeLinkage(LocalDate revocationDate,
                             String revocationReason,
                             String revokedBy,
                             UUID revokedByUserId) {
        if (status == LinkageStatus.REVOKED) {
            throw new IllegalStateException("Linkage is already revoked");
        }

        apply(new ProjectLinkageRevoked(
            id.value(),
            revocationDate,
            revocationReason,
            revokedBy,
            revokedByUserId,
            Instant.now()
        ));
    }

    /**
     * Check if linkage is currently effective
     */
    public boolean isEffective() {
        return status == LinkageStatus.ACTIVE &&
               (linkageEndDate == null || LocalDate.now().isBefore(linkageEndDate));
    }

    /**
     * Check if linkage was effective on a specific date
     */
    public boolean wasEffectiveOn(LocalDate date) {
        if (status == LinkageStatus.REVOKED) {
            return false;
        }

        return !date.isBefore(linkageEffectiveDate) &&
               (linkageEndDate == null || !date.isAfter(linkageEndDate));
    }

    /**
     * Validate TH to RRH transition constraints
     */
    public void validateTransitionConstraints(LocalDate thExitDate, LocalDate rrhMoveInDate) {
        if (!isEffective()) {
            throw new IllegalStateException("Cannot validate transition - linkage is not effective");
        }

        if (thExitDate == null) {
            throw new IllegalArgumentException("TH exit date is required for transition validation");
        }

        if (rrhMoveInDate == null) {
            throw new IllegalArgumentException("RRH move-in date is required for transition validation");
        }

        // Core constraint: RRH move-in cannot precede TH exit
        if (rrhMoveInDate.isBefore(thExitDate)) {
            throw new ProjectLinkageViolationException(
                "RRH move-in date (" + rrhMoveInDate + ") cannot precede TH exit date (" + thExitDate + ")",
                ProjectLinkageViolationException.ViolationType.MOVE_IN_DATE_CONSTRAINT,
                this.id.value(),
                thProjectId,
                rrhProjectId
            );
        }

        // Additional constraint: Move-in should be within reasonable timeframe (configurable)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(thExitDate, rrhMoveInDate);
        if (daysBetween > 30) { // 30 days default threshold
            throw new ProjectLinkageViolationException(
                "RRH move-in date (" + rrhMoveInDate + ") is more than 30 days after TH exit (" + thExitDate + ")",
                ProjectLinkageViolationException.ViolationType.EXCESSIVE_TRANSITION_GAP,
                this.id.value(),
                thProjectId,
                rrhProjectId
            );
        }
    }

    /**
     * Get linkage duration in days
     */
    public long getLinkageDurationDays() {
        LocalDate endDate = linkageEndDate != null ? linkageEndDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(linkageEffectiveDate, endDate);
    }

    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ProjectLinkageCreated e) {
            this.id = ProjectLinkageId.of(e.linkageId());
            this.thProjectId = e.thProjectId();
            this.rrhProjectId = e.rrhProjectId();
            this.thHudProjectId = e.thHudProjectId();
            this.rrhHudProjectId = e.rrhHudProjectId();
            this.thProjectName = e.thProjectName();
            this.rrhProjectName = e.rrhProjectName();
            this.linkageEffectiveDate = e.effectiveDate();
            this.linkageReason = e.linkageReason();
            this.createdBy = e.authorizedBy();
            this.authorizedByUserId = e.authorizedByUserId();
            this.status = LinkageStatus.ACTIVE;
            this.createdAt = e.occurredAt();
            this.lastModifiedAt = e.occurredAt();
            this.lastModifiedBy = e.authorizedBy();
            this.authorizationDate = e.effectiveDate();

        } else if (event instanceof ProjectLinkageModified e) {
            this.linkageReason = e.newLinkageReason();
            this.linkageNotes = e.newLinkageNotes();
            this.lastModifiedBy = e.modifiedBy();
            this.lastModifiedAt = e.occurredAt();

        } else if (event instanceof ProjectLinkageRevoked e) {
            this.status = LinkageStatus.REVOKED;
            this.linkageEndDate = e.revocationDate();
            this.linkageNotes = (this.linkageNotes != null ? this.linkageNotes + "\n" : "") +
                               "REVOKED: " + e.revocationReason();
            this.lastModifiedBy = e.revokedBy();
            this.lastModifiedAt = e.occurredAt();

        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }

    public enum LinkageStatus {
        ACTIVE,
        REVOKED,
        EXPIRED
    }

    // Getters
    public UUID getThProjectId() { return thProjectId; }
    public UUID getRrhProjectId() { return rrhProjectId; }
    public String getThHudProjectId() { return thHudProjectId; }
    public String getRrhHudProjectId() { return rrhHudProjectId; }
    public String getThProjectName() { return thProjectName; }
    public String getRrhProjectName() { return rrhProjectName; }
    public LocalDate getLinkageEffectiveDate() { return linkageEffectiveDate; }
    public LocalDate getLinkageEndDate() { return linkageEndDate; }
    public LinkageStatus getStatus() { return status; }
    public String getLinkageReason() { return linkageReason; }
    public String getLinkageNotes() { return linkageNotes; }
    public String getCreatedBy() { return createdBy; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public String getLinkageAuthorization() { return linkageAuthorization; }
    public UUID getAuthorizedByUserId() { return authorizedByUserId; }
    public LocalDate getAuthorizationDate() { return authorizationDate; }
}