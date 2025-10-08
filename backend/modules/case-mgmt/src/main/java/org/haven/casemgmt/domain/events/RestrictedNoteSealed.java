package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class RestrictedNoteSealed extends DomainEvent {
    private final UUID noteId;
    private final UUID sealedBy;
    private final String sealedByName;
    private final String sealReason;
    private final String legalBasis;
    private final boolean isTemporary;
    private final Instant expiresAt;

    public RestrictedNoteSealed(UUID noteId, UUID sealedBy, String sealedByName, Instant sealedAt,
                              String sealReason, String legalBasis, boolean isTemporary, Instant expiresAt) {
        super(noteId, sealedAt);
        this.noteId = noteId;
        this.sealedBy = sealedBy;
        this.sealedByName = sealedByName;
        this.sealReason = sealReason;
        this.legalBasis = legalBasis;
        this.isTemporary = isTemporary;
        this.expiresAt = expiresAt;
    }

    @Override
    public String eventType() {
        return "RestrictedNoteSealed";
    }

    // Record-style accessors (for backward compatibility)
    public UUID noteId() { return noteId; }
    public UUID sealedBy() { return sealedBy; }
    public String sealedByName() { return sealedByName; }
    public Instant sealedAt() { return getOccurredOn(); }
    public String sealReason() { return sealReason; }
    public String legalBasis() { return legalBasis; }
    public boolean isTemporary() { return isTemporary; }
    public Instant expiresAt() { return expiresAt; }

    // JavaBean-style getters
    public UUID getNoteId() { return noteId; }
    public UUID getSealedBy() { return sealedBy; }
    public String getSealedByName() { return sealedByName; }
    public Instant getSealedAt() { return getOccurredOn(); }
    public String getSealReason() { return sealReason; }
    public String getLegalBasis() { return legalBasis; }
    public boolean getIsTemporary() { return isTemporary; }
    public Instant getExpiresAt() { return expiresAt; }
}