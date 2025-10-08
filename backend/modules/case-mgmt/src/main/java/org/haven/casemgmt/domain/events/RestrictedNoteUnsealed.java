package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class RestrictedNoteUnsealed extends DomainEvent {
    private final UUID noteId;
    private final UUID unsealedBy;
    private final String unsealedByName;
    private final String unsealReason;
    private final String legalBasis;
    private final String previousSealReason;

    public RestrictedNoteUnsealed(UUID noteId, UUID unsealedBy, String unsealedByName, Instant unsealedAt,
                                String unsealReason, String legalBasis, String previousSealReason) {
        super(noteId, unsealedAt);
        this.noteId = noteId;
        this.unsealedBy = unsealedBy;
        this.unsealedByName = unsealedByName;
        this.unsealReason = unsealReason;
        this.legalBasis = legalBasis;
        this.previousSealReason = previousSealReason;
    }

    @Override
    public String eventType() {
        return "RestrictedNoteUnsealed";
    }

    // Record-style accessors (for backward compatibility)
    public UUID noteId() { return noteId; }
    public UUID unsealedBy() { return unsealedBy; }
    public String unsealedByName() { return unsealedByName; }
    public Instant unsealedAt() { return getOccurredOn(); }
    public String unsealReason() { return unsealReason; }
    public String legalBasis() { return legalBasis; }
    public String previousSealReason() { return previousSealReason; }

    // JavaBean-style getters
    public UUID getNoteId() { return noteId; }
    public UUID getUnsealedBy() { return unsealedBy; }
    public String getUnsealedByName() { return unsealedByName; }
    public Instant getUnsealedAt() { return getOccurredOn(); }
    public String getUnsealReason() { return unsealReason; }
    public String getLegalBasis() { return legalBasis; }
    public String getPreviousSealReason() { return previousSealReason; }
}