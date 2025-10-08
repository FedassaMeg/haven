package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class CaseNoteAdded extends DomainEvent {
    private final UUID noteId;
    private final String content;
    private final String authorId;

    public CaseNoteAdded(UUID caseId, UUID noteId, String content, String authorId, Instant occurredAt) {
        super(caseId, occurredAt);
        this.noteId = noteId;
        this.content = content;
        this.authorId = authorId;
    }

    public UUID noteId() {
        return noteId;
    }

    public String content() {
        return content;
    }

    public String authorId() {
        return authorId;
    }


    // JavaBean-style getters
    public UUID getNoteId() { return noteId; }
    public String getContent() { return content; }
    public String getAuthorId() { return authorId; }
}