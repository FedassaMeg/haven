package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNoteAccessed extends DomainEvent {
    private final UUID noteId;
    private final UUID accessedBy;
    private final String accessedByName;
    private final List<String> userRoles;
    private final String accessMethod;
    private final String ipAddress;
    private final String userAgent;
    private final boolean wasContentViewed;
    private final String accessReason;

    public RestrictedNoteAccessed(UUID noteId, UUID accessedBy, String accessedByName, Instant accessedAt,
                                List<String> userRoles, String accessMethod, String ipAddress, String userAgent,
                                boolean wasContentViewed, String accessReason) {
        super(noteId, accessedAt);
        this.noteId = noteId;
        this.accessedBy = accessedBy;
        this.accessedByName = accessedByName;
        this.userRoles = userRoles;
        this.accessMethod = accessMethod;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.wasContentViewed = wasContentViewed;
        this.accessReason = accessReason;
    }

    @Override
    public String eventType() {
        return "RestrictedNoteAccessed";
    }

    // Record-style accessors (for backward compatibility)
    public UUID noteId() { return noteId; }
    public UUID accessedBy() { return accessedBy; }
    public String accessedByName() { return accessedByName; }
    public Instant accessedAt() { return getOccurredOn(); }
    public List<String> userRoles() { return userRoles; }
    public String accessMethod() { return accessMethod; }
    public String ipAddress() { return ipAddress; }
    public String userAgent() { return userAgent; }
    public boolean wasContentViewed() { return wasContentViewed; }
    public String accessReason() { return accessReason; }

    // JavaBean-style getters
    public UUID getNoteId() { return noteId; }
    public UUID getAccessedBy() { return accessedBy; }
    public String getAccessedByName() { return accessedByName; }
    public Instant getAccessedAt() { return getOccurredOn(); }
    public List<String> getUserRoles() { return userRoles; }
    public String getAccessMethod() { return accessMethod; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public boolean getWasContentViewed() { return wasContentViewed; }
    public String getAccessReason() { return accessReason; }
}