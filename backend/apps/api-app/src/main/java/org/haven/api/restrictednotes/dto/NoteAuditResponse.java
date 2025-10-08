package org.haven.api.restrictednotes.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NoteAuditResponse {
    private UUID noteId;
    private String eventType;
    private UUID performedBy;
    private String performedByName;
    private List<String> userRoles;
    private Instant performedAt;
    private String reason;
    private String accessMethod;
    private String ipAddress;
    private String userAgent;
    private boolean contentViewed;
    private String details;
    
    public UUID getNoteId() { return noteId; }
    public void setNoteId(UUID noteId) { this.noteId = noteId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public UUID getPerformedBy() { return performedBy; }
    public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }
    
    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }
    
    public List<String> getUserRoles() { return userRoles; }
    public void setUserRoles(List<String> userRoles) { this.userRoles = userRoles; }
    
    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getAccessMethod() { return accessMethod; }
    public void setAccessMethod(String accessMethod) { this.accessMethod = accessMethod; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public boolean isContentViewed() { return contentViewed; }
    public void setContentViewed(boolean contentViewed) { this.contentViewed = contentViewed; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}