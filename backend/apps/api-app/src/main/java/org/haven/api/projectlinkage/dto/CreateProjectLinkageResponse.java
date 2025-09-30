package org.haven.api.projectlinkage.dto;

import java.util.UUID;

public class CreateProjectLinkageResponse {
    private UUID linkageId;
    private String message;

    public CreateProjectLinkageResponse() {}

    public CreateProjectLinkageResponse(UUID linkageId, String message) {
        this.linkageId = linkageId;
        this.message = message;
    }

    public UUID getLinkageId() { return linkageId; }
    public void setLinkageId(UUID linkageId) { this.linkageId = linkageId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}