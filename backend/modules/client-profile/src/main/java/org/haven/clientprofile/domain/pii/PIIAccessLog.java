package org.haven.clientprofile.domain.pii;

import java.time.Instant;
import java.util.UUID;

public class PIIAccessLog {
    private final UUID id;
    private final UUID userId;
    private final UUID clientId;
    private final PIICategory category;
    private final PIIAccessLevel accessLevel;
    private final boolean granted;
    private final String businessJustification;
    private final UUID caseId;
    private final String sessionId;
    private final String ipAddress;
    private final Instant accessedAt;
    
    public PIIAccessLog(UUID userId, UUID clientId, PIICategory category,
                       PIIAccessLevel accessLevel, boolean granted, String businessJustification,
                       UUID caseId, String sessionId, String ipAddress, Instant accessedAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.clientId = clientId;
        this.category = category;
        this.accessLevel = accessLevel;
        this.granted = granted;
        this.businessJustification = businessJustification;
        this.caseId = caseId;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.accessedAt = accessedAt;
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getClientId() { return clientId; }
    public PIICategory getCategory() { return category; }
    public PIIAccessLevel getAccessLevel() { return accessLevel; }
    public boolean isGranted() { return granted; }
    public String getBusinessJustification() { return businessJustification; }
    public UUID getCaseId() { return caseId; }
    public String getSessionId() { return sessionId; }
    public String getIpAddress() { return ipAddress; }
    public Instant getAccessedAt() { return accessedAt; }
}