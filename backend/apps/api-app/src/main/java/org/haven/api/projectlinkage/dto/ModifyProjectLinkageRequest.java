package org.haven.api.projectlinkage.dto;

import jakarta.validation.constraints.NotBlank;

public class ModifyProjectLinkageRequest {

    @NotBlank(message = "Linkage reason is required")
    private String linkageReason;

    private String linkageNotes;

    public ModifyProjectLinkageRequest() {}

    public ModifyProjectLinkageRequest(String linkageReason, String linkageNotes) {
        this.linkageReason = linkageReason;
        this.linkageNotes = linkageNotes;
    }

    public String getLinkageReason() { return linkageReason; }
    public void setLinkageReason(String linkageReason) { this.linkageReason = linkageReason; }

    public String getLinkageNotes() { return linkageNotes; }
    public void setLinkageNotes(String linkageNotes) { this.linkageNotes = linkageNotes; }
}