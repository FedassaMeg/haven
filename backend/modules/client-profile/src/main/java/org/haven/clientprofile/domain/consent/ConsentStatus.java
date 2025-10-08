package org.haven.clientprofile.domain.consent;

/**
 * Status of a consent record
 */
public enum ConsentStatus {
    /**
     * Consent has been granted and is currently active
     */
    GRANTED,
    
    /**
     * Consent has been explicitly revoked by the client
     */
    REVOKED,
    
    /**
     * Consent has expired based on time limits
     */
    EXPIRED,
    
    /**
     * Consent is pending client decision
     */
    PENDING,
    
    /**
     * Consent was denied by the client
     */
    DENIED;
    
    public boolean isActive() {
        return this == GRANTED;
    }
    
    public boolean canBeUsed() {
        return this == GRANTED;
    }
    
    public boolean requiresRenewal() {
        return this == EXPIRED || this == REVOKED;
    }
}