package org.haven.clientprofile.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail for all consent-related events
 * This provides a comprehensive log of all consent mutations for compliance and auditing
 */
@Entity
@Table(name = "consent_audit_trail", indexes = {
    @Index(name = "idx_consent_audit_consent_id", columnList = "consent_id"),
    @Index(name = "idx_consent_audit_client_id", columnList = "client_id"),
    @Index(name = "idx_consent_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_consent_audit_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_consent_audit_user_id", columnList = "acting_user_id")
})
public class ConsentAuditTrailEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "consent_id", nullable = false)
    private UUID consentId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;
    
    @Column(name = "acting_user_id", nullable = false)
    private UUID actingUserId;
    
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "reason", length = 1000)
    private String reason;
    
    @Column(name = "recipient_organization", length = 500)
    private String recipientOrganization;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
    
    protected ConsentAuditTrailEntity() {
        // For JPA
    }
    
    public ConsentAuditTrailEntity(UUID consentId, UUID clientId, String eventType, ConsentType consentType,
                                  UUID actingUserId, Instant occurredAt, String eventData, String reason,
                                  String recipientOrganization, String ipAddress, String userAgent) {
        this.consentId = consentId;
        this.clientId = clientId;
        this.eventType = eventType;
        this.consentType = consentType;
        this.actingUserId = actingUserId;
        this.occurredAt = occurredAt;
        this.eventData = eventData;
        this.reason = reason;
        this.recipientOrganization = recipientOrganization;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
    
    // Getters only - this is an immutable audit record
    public Long getId() { return id; }
    public UUID getConsentId() { return consentId; }
    public UUID getClientId() { return clientId; }
    public String getEventType() { return eventType; }
    public ConsentType getConsentType() { return consentType; }
    public UUID getActingUserId() { return actingUserId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getEventData() { return eventData; }
    public String getReason() { return reason; }
    public String getRecipientOrganization() { return recipientOrganization; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
}