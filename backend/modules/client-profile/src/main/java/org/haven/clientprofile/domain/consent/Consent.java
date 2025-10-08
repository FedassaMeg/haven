package org.haven.clientprofile.domain.consent;

import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.events.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Consent aggregate for managing client consent and Release of Information (ROI)
 * Ensures all data sharing operations have proper client authorization
 */
public class Consent extends AggregateRoot<ConsentId> {
    
    private ClientId clientId;
    private ConsentType consentType;
    private ConsentStatus status;
    private String purpose;
    private String recipientOrganization;
    private String recipientContact;
    private Instant grantedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private UUID grantedByUserId;
    private UUID revokedByUserId;
    private String revocationReason;
    private boolean isVAWAProtected;
    private String limitations;
    
    // Default consent duration is 1 year for most types
    private static final int DEFAULT_DURATION_MONTHS = 12;
    
    protected Consent() {
        // Required for event sourcing reconstruction
    }
    
    /**
     * Create empty consent for event sourcing reconstruction
     * Only to be used by the repository when replaying events
     */
    public static Consent reconstruct() {
        return new Consent();
    }
    
    /**
     * Grant new consent
     */
    public static Consent grant(ConsentId consentId, ClientId clientId, ConsentType consentType,
                               String purpose, String recipientOrganization, String recipientContact,
                               UUID grantedByUserId, Integer durationMonths, String limitations) {
        
        Consent consent = new Consent();
        
        Instant expiresAt = null;
        if (durationMonths != null) {
            expiresAt = Instant.now().plus(durationMonths * 30L, ChronoUnit.DAYS);
        } else if (!isTimeless(consentType)) {
            expiresAt = Instant.now().plus(DEFAULT_DURATION_MONTHS * 30L, ChronoUnit.DAYS);
        }
        
        consent.apply(new ConsentGranted(
            consentId.value(),
            clientId.value(),
            consentType,
            purpose,
            recipientOrganization,
            recipientContact,
            grantedByUserId,
            Instant.now(),
            expiresAt,
            isVAWAProtected(consentType),
            limitations
        ));
        
        return consent;
    }
    
    /**
     * Revoke existing consent
     */
    public void revoke(UUID revokedByUserId, String reason) {
        if (status != ConsentStatus.GRANTED) {
            throw new ConsentDomainException("Cannot revoke consent that is not currently granted");
        }
        
        apply(new ConsentRevoked(
            id.value(),
            clientId.value(),
            consentType,
            revokedByUserId,
            reason,
            Instant.now()
        ));
    }
    
    /**
     * Update consent limitations or recipient information
     */
    public void update(String newLimitations, String newRecipientContact, UUID updatedByUserId) {
        if (status != ConsentStatus.GRANTED) {
            throw new ConsentDomainException("Cannot update consent that is not currently granted");
        }
        
        apply(new ConsentUpdated(
            id.value(),
            clientId.value(),
            newLimitations,
            newRecipientContact,
            updatedByUserId,
            Instant.now()
        ));
    }
    
    /**
     * Extend consent expiration date
     */
    public void extend(Instant newExpirationDate, UUID extendedByUserId) {
        if (status != ConsentStatus.GRANTED) {
            throw new ConsentDomainException("Cannot extend consent that is not currently granted");
        }
        
        if (newExpirationDate.isBefore(Instant.now())) {
            throw new ConsentDomainException("New expiration date cannot be in the past");
        }
        
        apply(new ConsentExtended(
            id.value(),
            clientId.value(),
            this.expiresAt,
            newExpirationDate,
            extendedByUserId,
            Instant.now()
        ));
    }
    
    /**
     * Check if consent is currently valid for use
     */
    public boolean isValidForUse() {
        return status == ConsentStatus.GRANTED && 
               (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
    
    /**
     * Check if consent is expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now()) && status == ConsentStatus.GRANTED;
    }
    
    /**
     * Expire consent if past expiration date
     */
    public void expireIfNeeded() {
        if (isExpired() && status == ConsentStatus.GRANTED) {
            apply(new ConsentExpired(
                id.value(),
                clientId.value(),
                consentType,
                expiresAt,
                Instant.now()
            ));
        }
    }
    
    /**
     * Validate that a specific data sharing operation is allowed under this consent
     */
    public boolean authorizes(String operation, String targetRecipient) {
        if (!isValidForUse()) {
            return false;
        }
        
        // Check recipient matches (null means any recipient)
        if (recipientOrganization != null && !recipientOrganization.equalsIgnoreCase(targetRecipient)) {
            return false;
        }
        
        // Check operation-specific rules
        switch (consentType) {
            case INFORMATION_SHARING:
                return operation.contains("share") || operation.contains("export");
            case HMIS_PARTICIPATION:
                return operation.contains("hmis") || operation.contains("report");
            case COURT_TESTIMONY:
                return operation.contains("court") || operation.contains("legal");
            case MEDICAL_INFORMATION_SHARING:
                return operation.contains("medical") || operation.contains("health");
            case REFERRAL_SHARING:
                return operation.contains("referral") || operation.contains("transfer");
            case RESEARCH_PARTICIPATION:
                return operation.contains("research") || operation.contains("evaluation");
            default:
                return false;
        }
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ConsentGranted e) {
            this.id = new ConsentId(e.consentId());
            this.clientId = new ClientId(e.clientId());
            this.consentType = e.consentType();
            this.status = ConsentStatus.GRANTED;
            this.purpose = e.purpose();
            this.recipientOrganization = e.recipientOrganization();
            this.recipientContact = e.recipientContact();
            this.grantedAt = e.grantedAt();
            this.expiresAt = e.expiresAt();
            this.grantedByUserId = e.grantedByUserId();
            this.isVAWAProtected = e.isVAWAProtected();
            this.limitations = e.limitations();
        } else if (event instanceof ConsentRevoked e) {
            this.status = ConsentStatus.REVOKED;
            this.revokedAt = e.revokedAt();
            this.revokedByUserId = e.revokedByUserId();
            this.revocationReason = e.reason();
        } else if (event instanceof ConsentUpdated e) {
            this.limitations = e.newLimitations();
            this.recipientContact = e.newRecipientContact();
        } else if (event instanceof ConsentExtended e) {
            this.expiresAt = e.newExpirationDate();
        } else if (event instanceof ConsentExpired e) {
            this.status = ConsentStatus.EXPIRED;
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    private static boolean isTimeless(ConsentType consentType) {
        // Some consents like emergency medical don't expire
        return consentType == ConsentType.LEGAL_COUNSEL_COMMUNICATION;
    }
    
    private static boolean isVAWAProtected(ConsentType consentType) {
        // VAWA provides special protections for certain types of information
        return consentType == ConsentType.COURT_TESTIMONY || 
               consentType == ConsentType.LEGAL_COUNSEL_COMMUNICATION ||
               consentType == ConsentType.FAMILY_CONTACT;
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public ConsentType getConsentType() { return consentType; }
    public ConsentStatus getStatus() { return status; }
    public String getPurpose() { return purpose; }
    public String getRecipientOrganization() { return recipientOrganization; }
    public String getRecipientContact() { return recipientContact; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public UUID getRevokedByUserId() { return revokedByUserId; }
    public String getRevocationReason() { return revocationReason; }
    public boolean isVAWAProtected() { return isVAWAProtected; }
    public String getLimitations() { return limitations; }

    // Additional helper methods for compatibility
    public boolean isActive() {
        return status == ConsentStatus.GRANTED &&
               (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public ConsentId getConsentId() {
        return id;
    }

    public Instant getEffectiveDate() {
        return grantedAt;
    }

    public Instant getExpirationDate() {
        return expiresAt;
    }
    
    /**
     * Domain exception for consent business rule violations
     */
    public static class ConsentDomainException extends RuntimeException {
        public ConsentDomainException(String message) {
            super(message);
        }
        
        public ConsentDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}