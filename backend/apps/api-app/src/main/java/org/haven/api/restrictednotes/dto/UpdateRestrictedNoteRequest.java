package org.haven.api.restrictednotes.dto;

public class UpdateRestrictedNoteRequest {
    private String content;
    private String updateReason;
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getUpdateReason() { return updateReason; }
    public void setUpdateReason(String updateReason) { this.updateReason = updateReason; }
}