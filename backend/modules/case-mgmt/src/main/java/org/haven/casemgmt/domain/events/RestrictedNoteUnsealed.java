package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class RestrictedNoteUnsealed implements DomainEvent {
    private final UUID noteId;
    private final UUID unsealedBy;
    private final String unsealedByName;
    private final Instant unsealedAt;
    private final String unsealReason;
    private final String legalBasis;
    private final String previousSealReason;
    
    public RestrictedNoteUnsealed(UUID noteId, UUID unsealedBy, String unsealedByName, Instant unsealedAt, 
                                String unsealReason, String legalBasis, String previousSealReason) {
        this.noteId = noteId;
        this.unsealedBy = unsealedBy;
        this.unsealedByName = unsealedByName;
        this.unsealedAt = unsealedAt;
        this.unsealReason = unsealReason;
        this.legalBasis = legalBasis;
        this.previousSealReason = previousSealReason;
    }
    
    @Override
    public UUID aggregateId() {
        return noteId;
    }
    
    @Override
    public Instant occurredAt() {
        return unsealedAt;
    }
    
    @Override
    public String eventType() {
        return "RestrictedNoteUnsealed";
    }
    
    public UUID getNoteId() { return noteId; }
    public UUID getUnsealedBy() { return unsealedBy; }
    public String getUnsealedByName() { return unsealedByName; }
    public Instant getUnsealedAt() { return unsealedAt; }
    public String getUnsealReason() { return unsealReason; }
    public String getLegalBasis() { return legalBasis; }
    public String getPreviousSealReason() { return previousSealReason; }
}