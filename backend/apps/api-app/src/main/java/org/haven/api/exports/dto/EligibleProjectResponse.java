package org.haven.api.exports.dto;

import java.util.UUID;

public class EligibleProjectResponse {
    private UUID projectId;
    private String projectName;
    private String projectType;
    private String hudProjectId;
    private boolean userHasAccess;
    private String accessReason;

    public EligibleProjectResponse() {}

    public EligibleProjectResponse(UUID projectId, String projectName, String projectType,
                                   String hudProjectId, boolean userHasAccess, String accessReason) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectType = projectType;
        this.hudProjectId = hudProjectId;
        this.userHasAccess = userHasAccess;
        this.accessReason = accessReason;
    }

    // Getters and setters
    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getHudProjectId() {
        return hudProjectId;
    }

    public void setHudProjectId(String hudProjectId) {
        this.hudProjectId = hudProjectId;
    }

    public boolean isUserHasAccess() {
        return userHasAccess;
    }

    public void setUserHasAccess(boolean userHasAccess) {
        this.userHasAccess = userHasAccess;
    }

    public String getAccessReason() {
        return accessReason;
    }

    public void setAccessReason(String accessReason) {
        this.accessReason = accessReason;
    }
}
