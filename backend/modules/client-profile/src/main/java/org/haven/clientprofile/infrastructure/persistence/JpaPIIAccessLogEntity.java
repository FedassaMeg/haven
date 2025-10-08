package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.pii.PIIAccessLevel;
import org.haven.clientprofile.domain.pii.PIIAccessLog;
import org.haven.clientprofile.domain.pii.PIICategory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pii_access_logs")
public class JpaPIIAccessLogEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "client_id")
    private UUID clientId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private PIICategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private PIIAccessLevel accessLevel;
    
    @Column(name = "granted", nullable = false)
    private boolean granted;
    
    @Column(name = "business_justification")
    private String businessJustification;
    
    @Column(name = "case_id")
    private UUID caseId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;
    
    // Default constructor for JPA
    public JpaPIIAccessLogEntity() {}
    
    public static JpaPIIAccessLogEntity fromDomain(PIIAccessLog log) {
        JpaPIIAccessLogEntity entity = new JpaPIIAccessLogEntity();
        entity.id = log.getId();
        entity.userId = log.getUserId();
        entity.clientId = log.getClientId();
        entity.category = log.getCategory();
        entity.accessLevel = log.getAccessLevel();
        entity.granted = log.isGranted();
        entity.businessJustification = log.getBusinessJustification();
        entity.caseId = log.getCaseId();
        entity.sessionId = log.getSessionId();
        entity.ipAddress = log.getIpAddress();
        entity.accessedAt = log.getAccessedAt();
        return entity;
    }
    
    public PIIAccessLog toDomain() {
        return new PIIAccessLog(
            userId, clientId, category, accessLevel, granted, 
            businessJustification, caseId, sessionId, ipAddress, accessedAt
        );
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public PIICategory getCategory() { return category; }
    public void setCategory(PIICategory category) { this.category = category; }
    
    public PIIAccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(PIIAccessLevel accessLevel) { this.accessLevel = accessLevel; }
    
    public boolean isGranted() { return granted; }
    public void setGranted(boolean granted) { this.granted = granted; }
    
    public String getBusinessJustification() { return businessJustification; }
    public void setBusinessJustification(String businessJustification) { this.businessJustification = businessJustification; }
    
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public Instant getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Instant accessedAt) { this.accessedAt = accessedAt; }
}