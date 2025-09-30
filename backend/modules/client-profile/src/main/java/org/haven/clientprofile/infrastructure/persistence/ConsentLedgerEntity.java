package org.haven.clientprofile.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.domain.consent.ConsentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model projection for consent ledger queries
 * This entity provides fast queries without requiring event replay
 */
@Entity
@Table(name = "consent_ledger", indexes = {
    @Index(name = "idx_consent_ledger_client_id", columnList = "client_id"),
    @Index(name = "idx_consent_ledger_type", columnList = "consent_type"),
    @Index(name = "idx_consent_ledger_status", columnList = "status"),
    @Index(name = "idx_consent_ledger_recipient", columnList = "recipient_organization"),
    @Index(name = "idx_consent_ledger_expires_at", columnList = "expires_at"),
    @Index(name = "idx_consent_ledger_granted_at", columnList = "granted_at")
})
public class ConsentLedgerEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsentStatus status;
    
    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;
    
    @Column(name = "recipient_organization", length = 500)
    private String recipientOrganization;
    
    @Column(name = "recipient_contact", length = 500)
    private String recipientContact;
    
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "granted_by_user_id", nullable = false)
    private UUID grantedByUserId;
    
    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;
    
    @Column(name = "revocation_reason", length = 1000)
    private String revocationReason;
    
    @Column(name = "is_vawa_protected", nullable = false)
    private boolean isVAWAProtected;
    
    @Column(name = "limitations", columnDefinition = "TEXT")
    private String limitations;
    
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;
    
    @Version
    private Long version;
    
    protected ConsentLedgerEntity() {
        // For JPA
    }
    
    public ConsentLedgerEntity(UUID id, UUID clientId, ConsentType consentType, ConsentStatus status,
                              String purpose, String recipientOrganization, String recipientContact,
                              Instant grantedAt, Instant expiresAt, UUID grantedByUserId,
                              boolean isVAWAProtected, String limitations) {
        this.id = id;
        this.clientId = clientId;
        this.consentType = consentType;
        this.status = status;
        this.purpose = purpose;
        this.recipientOrganization = recipientOrganization;
        this.recipientContact = recipientContact;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.grantedByUserId = grantedByUserId;
        this.isVAWAProtected = isVAWAProtected;
        this.limitations = limitations;
        this.lastUpdatedAt = Instant.now();
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public ConsentType getConsentType() { return consentType; }
    public void setConsentType(ConsentType consentType) { this.consentType = consentType; }
    
    public ConsentStatus getStatus() { return status; }
    public void setStatus(ConsentStatus status) { 
        this.status = status;
        this.lastUpdatedAt = Instant.now();
    }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getRecipientOrganization() { return recipientOrganization; }
    public void setRecipientOrganization(String recipientOrganization) { this.recipientOrganization = recipientOrganization; }
    
    public String getRecipientContact() { return recipientContact; }
    public void setRecipientContact(String recipientContact) { 
        this.recipientContact = recipientContact;
        this.lastUpdatedAt = Instant.now();
    }
    
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { 
        this.expiresAt = expiresAt;
        this.lastUpdatedAt = Instant.now();
    }
    
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { 
        this.revokedAt = revokedAt;
        this.lastUpdatedAt = Instant.now();
    }
    
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(UUID grantedByUserId) { this.grantedByUserId = grantedByUserId; }
    
    public UUID getRevokedByUserId() { return revokedByUserId; }
    public void setRevokedByUserId(UUID revokedByUserId) { 
        this.revokedByUserId = revokedByUserId;
        this.lastUpdatedAt = Instant.now();
    }
    
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { 
        this.revocationReason = revocationReason;
        this.lastUpdatedAt = Instant.now();
    }
    
    public boolean isVAWAProtected() { return isVAWAProtected; }
    public void setVAWAProtected(boolean VAWAProtected) { isVAWAProtected = VAWAProtected; }
    
    public String getLimitations() { return limitations; }
    public void setLimitations(String limitations) { 
        this.limitations = limitations;
        this.lastUpdatedAt = Instant.now();
    }
    
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    
    public Long getVersion() { return version; }
}