package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNoteUpdated extends DomainEvent {
    private final UUID noteId;
    private final String content;
    private final UUID updatedBy;
    private final String updatedByName;
    private final List<UUID> authorizedViewers;
    private final String visibilityScope;
    private final String updateReason;

    public RestrictedNoteUpdated(UUID noteId, String content, UUID updatedBy, String updatedByName,
                               Instant updatedAt, List<UUID> authorizedViewers, String visibilityScope,
                               String updateReason) {
        super(noteId, updatedAt);
        this.noteId = noteId;
        this.content = content;
        this.updatedBy = updatedBy;
        this.updatedByName = updatedByName;
        this.authorizedViewers = authorizedViewers;
        this.visibilityScope = visibilityScope;
        this.updateReason = updateReason;
    }

    @Override
    public String eventType() {
        return "RestrictedNoteUpdated";
    }

    // Record-style accessors (for backward compatibility)
    public UUID noteId() { return noteId; }
    public String content() { return content; }
    public UUID updatedBy() { return updatedBy; }
    public String updatedByName() { return updatedByName; }
    public Instant updatedAt() { return getOccurredOn(); }
    public List<UUID> authorizedViewers() { return authorizedViewers; }
    public String visibilityScope() { return visibilityScope; }
    public String updateReason() { return updateReason; }

    // JavaBean-style getters
    public UUID getNoteId() { return noteId; }
    public String getContent() { return content; }
    public UUID getUpdatedBy() { return updatedBy; }
    public String getUpdatedByName() { return updatedByName; }
    public Instant getUpdatedAt() { return getOccurredOn(); }
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public String getVisibilityScope() { return visibilityScope; }
    public String getUpdateReason() { return updateReason; }
}