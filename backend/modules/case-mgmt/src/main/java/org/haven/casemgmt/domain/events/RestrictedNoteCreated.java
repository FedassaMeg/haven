package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNoteCreated implements DomainEvent {
    private final UUID noteId;
    private final UUID clientId;
    private final String clientName;
    private final UUID caseId;
    private final String caseNumber;
    private final String noteType;
    private final String content;
    private final UUID authorId;
    private final String authorName;
    private final Instant createdAt;
    private final List<UUID> authorizedViewers;
    private final String visibilityScope;
    private final String title;
    
    public RestrictedNoteCreated(UUID noteId, UUID clientId, String clientName, UUID caseId, 
                               String caseNumber, String noteType, String content, UUID authorId, 
                               String authorName, Instant createdAt, List<UUID> authorizedViewers, 
                               String visibilityScope, String title) {
        this.noteId = noteId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.noteType = noteType;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.authorizedViewers = authorizedViewers;
        this.visibilityScope = visibilityScope;
        this.title = title;
    }
    
    @Override
    public UUID aggregateId() {
        return noteId;
    }
    
    @Override
    public Instant occurredAt() {
        return createdAt;
    }
    
    @Override
    public String eventType() {
        return "RestrictedNoteCreated";
    }
    
    public UUID getNoteId() { return noteId; }
    public UUID getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public UUID getCaseId() { return caseId; }
    public String getCaseNumber() { return caseNumber; }
    public String getNoteType() { return noteType; }
    public String getContent() { return content; }
    public UUID getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public Instant getCreatedAt() { return createdAt; }
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public String getVisibilityScope() { return visibilityScope; }
    public String getTitle() { return title; }
}