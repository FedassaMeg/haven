package org.haven.api.projectlinkage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class CreateProjectLinkageRequest {

    @NotNull(message = "TH project ID is required")
    private UUID thProjectId;

    @NotNull(message = "RRH project ID is required")
    private UUID rrhProjectId;

    @NotBlank(message = "TH HUD project ID is required")
    private String thHudProjectId;

    @NotBlank(message = "RRH HUD project ID is required")
    private String rrhHudProjectId;

    @NotBlank(message = "TH project name is required")
    private String thProjectName;

    @NotBlank(message = "RRH project name is required")
    private String rrhProjectName;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    @NotBlank(message = "Linkage reason is required")
    private String linkageReason;

    @NotBlank(message = "Authorized by is required")
    private String authorizedBy;

    @NotNull(message = "Authorized by user ID is required")
    private UUID authorizedByUserId;

    // Default constructor
    public CreateProjectLinkageRequest() {}

    // Constructor
    public CreateProjectLinkageRequest(UUID thProjectId, UUID rrhProjectId,
                                     String thHudProjectId, String rrhHudProjectId,
                                     String thProjectName, String rrhProjectName,
                                     LocalDate effectiveDate, String linkageReason,
                                     String authorizedBy, UUID authorizedByUserId) {
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

    // Getters and setters
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

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getLinkageReason() { return linkageReason; }
    public void setLinkageReason(String linkageReason) { this.linkageReason = linkageReason; }

    public String getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }

    public UUID getAuthorizedByUserId() { return authorizedByUserId; }
    public void setAuthorizedByUserId(UUID authorizedByUserId) { this.authorizedByUserId = authorizedByUserId; }
}