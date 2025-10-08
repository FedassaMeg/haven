package org.haven.api.projectlinkage.dto;

public class CanLinkProjectsResponse {
    private boolean canLink;
    private String message;

    public CanLinkProjectsResponse() {}

    public CanLinkProjectsResponse(boolean canLink, String message) {
        this.canLink = canLink;
        this.message = message;
    }

    public boolean isCanLink() { return canLink; }
    public void setCanLink(boolean canLink) { this.canLink = canLink; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}