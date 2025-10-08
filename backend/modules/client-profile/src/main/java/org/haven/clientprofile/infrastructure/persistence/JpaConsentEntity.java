package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.consent.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Consent persistence
 */
@Entity
@Table(name = "client_consents", schema = "haven")
public class JpaConsentEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsentStatus status;
    
    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;
    
    @Column(name = "recipient_organization", length = 255)
    private String recipientOrganization;
    
    @Column(name = "recipient_contact", length = 500)
    private String recipientContact;
    
    @Column(name = "granted_at")
    private Instant grantedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;
    
    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;
    
    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;
    
    @Column(name = "is_vawa_protected")
    private Boolean isVAWAProtected = false;
    
    @Column(name = "limitations", columnDefinition = "TEXT")
    private String limitations;
    
    @Version
    private Long version;
    
    // Constructors
    protected JpaConsentEntity() {
        // JPA requires default constructor
    }
    
    public JpaConsentEntity(UUID id, UUID clientId, ConsentType consentType, ConsentStatus status,
                          String purpose, String recipientOrganization, String recipientContact,
                          Instant grantedAt, Instant expiresAt, UUID grantedByUserId,
                          Boolean isVAWAProtected, String limitations) {
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
    }
    
    // Factory method from domain
    public static JpaConsentEntity fromDomain(Consent consent) {
        return new JpaConsentEntity(
            consent.getId().value(),
            consent.getClientId().value(),
            consent.getConsentType(),
            consent.getStatus(),
            consent.getPurpose(),
            consent.getRecipientOrganization(),
            consent.getRecipientContact(),
            consent.getGrantedAt(),
            consent.getExpiresAt(),
            consent.getGrantedByUserId(),
            consent.isVAWAProtected(),
            consent.getLimitations()
        );
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public ConsentType getConsentType() { return consentType; }
    public void setConsentType(ConsentType consentType) { this.consentType = consentType; }
    
    public ConsentStatus getStatus() { return status; }
    public void setStatus(ConsentStatus status) { this.status = status; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getRecipientOrganization() { return recipientOrganization; }
    public void setRecipientOrganization(String recipientOrganization) { this.recipientOrganization = recipientOrganization; }
    
    public String getRecipientContact() { return recipientContact; }
    public void setRecipientContact(String recipientContact) { this.recipientContact = recipientContact; }
    
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(UUID grantedByUserId) { this.grantedByUserId = grantedByUserId; }
    
    public UUID getRevokedByUserId() { return revokedByUserId; }
    public void setRevokedByUserId(UUID revokedByUserId) { this.revokedByUserId = revokedByUserId; }
    
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }
    
    public Boolean getIsVAWAProtected() { return isVAWAProtected; }
    public void setIsVAWAProtected(Boolean isVAWAProtected) { this.isVAWAProtected = isVAWAProtected; }
    
    public String getLimitations() { return limitations; }
    public void setLimitations(String limitations) { this.limitations = limitations; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}