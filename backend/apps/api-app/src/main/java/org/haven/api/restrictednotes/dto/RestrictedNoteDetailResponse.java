package org.haven.api.restrictednotes.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNoteDetailResponse {
    private UUID noteId;
    private UUID clientId;
    private String clientName;
    private UUID caseId;
    private String caseNumber;
    private String noteType;
    private String content;
    private String title;
    private UUID authorId;
    private String authorName;
    private Instant createdAt;
    private Instant lastModified;
    private List<UUID> authorizedViewers;
    private String visibilityScope;
    private boolean isSealed;
    private String sealReason;
    private Instant sealedAt;
    private UUID sealedBy;
    private boolean requiresSpecialHandling;
    private String visibilityWarning;
    private String message;
    
    public UUID getNoteId() { return noteId; }
    public void setNoteId(UUID noteId) { this.noteId = noteId; }
    
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
    
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
    
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public void setAuthorizedViewers(List<UUID> authorizedViewers) { this.authorizedViewers = authorizedViewers; }
    
    public String getVisibilityScope() { return visibilityScope; }
    public void setVisibilityScope(String visibilityScope) { this.visibilityScope = visibilityScope; }
    
    public boolean isSealed() { return isSealed; }
    public void setSealed(boolean sealed) { isSealed = sealed; }
    
    public String getSealReason() { return sealReason; }
    public void setSealReason(String sealReason) { this.sealReason = sealReason; }
    
    public Instant getSealedAt() { return sealedAt; }
    public void setSealedAt(Instant sealedAt) { this.sealedAt = sealedAt; }
    
    public UUID getSealedBy() { return sealedBy; }
    public void setSealedBy(UUID sealedBy) { this.sealedBy = sealedBy; }
    
    public boolean isRequiresSpecialHandling() { return requiresSpecialHandling; }
    public void setRequiresSpecialHandling(boolean requiresSpecialHandling) { this.requiresSpecialHandling = requiresSpecialHandling; }
    
    public String getVisibilityWarning() { return visibilityWarning; }
    public void setVisibilityWarning(String visibilityWarning) { this.visibilityWarning = visibilityWarning; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}