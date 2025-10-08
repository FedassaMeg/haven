package org.haven.api.projectlinkage.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProjectLinkageResponse {
    private UUID linkageId;
    private UUID thProjectId;
    private UUID rrhProjectId;
    private String thHudProjectId;
    private String rrhHudProjectId;
    private String thProjectName;
    private String rrhProjectName;
    private LocalDate linkageEffectiveDate;
    private LocalDate linkageEndDate;
    private String status;
    private String linkageReason;
    private String linkageNotes;
    private String createdBy;
    private String lastModifiedBy;
    private Instant createdAt;
    private Instant lastModifiedAt;

    public ProjectLinkageResponse() {}

    public ProjectLinkageResponse(UUID linkageId, UUID thProjectId, UUID rrhProjectId,
                                 String thHudProjectId, String rrhHudProjectId,
                                 String thProjectName, String rrhProjectName,
                                 LocalDate linkageEffectiveDate, LocalDate linkageEndDate,
                                 String status, String linkageReason, String linkageNotes,
                                 String createdBy, String lastModifiedBy,
                                 Instant createdAt, Instant lastModifiedAt) {
        this.linkageId = linkageId;
        this.thProjectId = thProjectId;
        this.rrhProjectId = rrhProjectId;
        this.thHudProjectId = thHudProjectId;
        this.rrhHudProjectId = rrhHudProjectId;
        this.thProjectName = thProjectName;
        this.rrhProjectName = rrhProjectName;
        this.linkageEffectiveDate = linkageEffectiveDate;
        this.linkageEndDate = linkageEndDate;
        this.status = status;
        this.linkageReason = linkageReason;
        this.linkageNotes = linkageNotes;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
    }

    // Getters and setters
    public UUID getLinkageId() { return linkageId; }
    public void setLinkageId(UUID linkageId) { this.linkageId = linkageId; }

    public UUID getThProjectId() { return thProjectId; }
    public void setThProjectId(UUID thProjectId) { this.thProjectId = thProjectId; }

    public UUID getRrhProjectId() { return rrhProjectId; }
    public void setRrhProjectId(UUID rrhProjectId) { this.rrhProjectId = rrhProjectId; }

    public String getThHudProjectId() { return thHudProjectId; }
    public void setThHudProjectId(String thHudProjectId) { this.thHudProjectId = thHudProjectId; }

    public String getRrhHudProjectId() { return rrhHudProjectId; }
    public void setRrhHudProjectId(String rrhHudProjectId) { this.rrhHudProjectId = rrhHudProjectId; }

    public String getThProjectName() { return thProjectName; }
    public void setThProjectName(String thProjectName) { this.thProjectName = thProjectName; }

    public String getRrhProjectName() { return rrhProjectName; }
    public void setRrhProjectName(String rrhProjectName) { this.rrhProjectName = rrhProjectName; }

    public LocalDate getLinkageEffectiveDate() { return linkageEffectiveDate; }
    public void setLinkageEffectiveDate(LocalDate linkageEffectiveDate) { this.linkageEffectiveDate = linkageEffectiveDate; }

    public LocalDate getLinkageEndDate() { return linkageEndDate; }
    public void setLinkageEndDate(LocalDate linkageEndDate) { this.linkageEndDate = linkageEndDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLinkageReason() { return linkageReason; }
    public void setLinkageReason(String linkageReason) { this.linkageReason = linkageReason; }

    public String getLinkageNotes() { return linkageNotes; }
    public void setLinkageNotes(String linkageNotes) { this.linkageNotes = linkageNotes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(Instant lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
}