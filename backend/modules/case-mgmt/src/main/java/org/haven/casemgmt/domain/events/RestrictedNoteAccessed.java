package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNoteAccessed implements DomainEvent {
    private final UUID noteId;
    private final UUID accessedBy;
    private final String accessedByName;
    private final Instant accessedAt;
    private final List<String> userRoles;
    private final String accessMethod;
    private final String ipAddress;
    private final String userAgent;
    private final boolean wasContentViewed;
    private final String accessReason;
    
    public RestrictedNoteAccessed(UUID noteId, UUID accessedBy, String accessedByName, Instant accessedAt, 
                                List<String> userRoles, String accessMethod, String ipAddress, String userAgent,
                                boolean wasContentViewed, String accessReason) {
        this.noteId = noteId;
        this.accessedBy = accessedBy;
        this.accessedByName = accessedByName;
        this.accessedAt = accessedAt;
        this.userRoles = userRoles;
        this.accessMethod = accessMethod;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.wasContentViewed = wasContentViewed;
        this.accessReason = accessReason;
    }
    
    @Override
    public UUID aggregateId() {
        return noteId;
    }
    
    @Override
    public Instant occurredAt() {
        return accessedAt;
    }
    
    @Override
    public String eventType() {
        return "RestrictedNoteAccessed";
    }
    
    public UUID getNoteId() { return noteId; }
    public UUID getAccessedBy() { return accessedBy; }
    public String getAccessedByName() { return accessedByName; }
    public Instant getAccessedAt() { return accessedAt; }
    public List<String> getUserRoles() { return userRoles; }
    public String getAccessMethod() { return accessMethod; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public boolean wasContentViewed() { return wasContentViewed; }
    public String getAccessReason() { return accessReason; }
}