package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class RestrictedNoteSealed implements DomainEvent {
    private final UUID noteId;
    private final UUID sealedBy;
    private final String sealedByName;
    private final Instant sealedAt;
    private final String sealReason;
    private final String legalBasis;
    private final boolean isTemporary;
    private final Instant expiresAt;
    
    public RestrictedNoteSealed(UUID noteId, UUID sealedBy, String sealedByName, Instant sealedAt, 
                              String sealReason, String legalBasis, boolean isTemporary, Instant expiresAt) {
        this.noteId = noteId;
        this.sealedBy = sealedBy;
        this.sealedByName = sealedByName;
        this.sealedAt = sealedAt;
        this.sealReason = sealReason;
        this.legalBasis = legalBasis;
        this.isTemporary = isTemporary;
        this.expiresAt = expiresAt;
    }
    
    @Override
    public UUID aggregateId() {
        return noteId;
    }
    
    @Override
    public Instant occurredAt() {
        return sealedAt;
    }
    
    @Override
    public String eventType() {
        return "RestrictedNoteSealed";
    }
    
    public UUID getNoteId() { return noteId; }
    public UUID getSealedBy() { return sealedBy; }
    public String getSealedByName() { return sealedByName; }
    public Instant getSealedAt() { return sealedAt; }
    public String getSealReason() { return sealReason; }
    public String getLegalBasis() { return legalBasis; }
    public boolean isTemporary() { return isTemporary; }
    public Instant getExpiresAt() { return expiresAt; }
}