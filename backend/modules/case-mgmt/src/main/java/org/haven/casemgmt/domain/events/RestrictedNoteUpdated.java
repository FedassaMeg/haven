package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNoteUpdated implements DomainEvent {
    private final UUID noteId;
    private final String content;
    private final UUID updatedBy;
    private final String updatedByName;
    private final Instant updatedAt;
    private final List<UUID> authorizedViewers;
    private final String visibilityScope;
    private final String updateReason;
    
    public RestrictedNoteUpdated(UUID noteId, String content, UUID updatedBy, String updatedByName, 
                               Instant updatedAt, List<UUID> authorizedViewers, String visibilityScope, 
                               String updateReason) {
        this.noteId = noteId;
        this.content = content;
        this.updatedBy = updatedBy;
        this.updatedByName = updatedByName;
        this.updatedAt = updatedAt;
        this.authorizedViewers = authorizedViewers;
        this.visibilityScope = visibilityScope;
        this.updateReason = updateReason;
    }
    
    @Override
    public UUID aggregateId() {
        return noteId;
    }
    
    @Override
    public Instant occurredAt() {
        return updatedAt;
    }
    
    @Override
    public String eventType() {
        return "RestrictedNoteUpdated";
    }
    
    public UUID getNoteId() { return noteId; }
    public String getContent() { return content; }
    public UUID getUpdatedBy() { return updatedBy; }
    public String getUpdatedByName() { return updatedByName; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public String getVisibilityScope() { return visibilityScope; }
    public String getUpdateReason() { return updateReason; }
}