package org.haven.api.restrictednotes.dto;

import java.util.List;
import java.util.UUID;

public class CreateRestrictedNoteRequest {
    private UUID clientId;
    private String clientName;
    private UUID caseId;
    private String caseNumber;
    private String noteType;
    private String content;
    private String title;
    private List<UUID> authorizedViewers;
    private String visibilityScope;
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public String getNoteType() { return noteType; }
    public void setNoteType(String noteType) { this.noteType = noteType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public void setAuthorizedViewers(List<UUID> authorizedViewers) { this.authorizedViewers = authorizedViewers; }
    
    public String getVisibilityScope() { return visibilityScope; }
    public void setVisibilityScope(String visibilityScope) { this.visibilityScope = visibilityScope; }
}