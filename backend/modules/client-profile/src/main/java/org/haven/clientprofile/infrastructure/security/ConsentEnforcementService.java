package org.haven.clientprofile.infrastructure.security;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.*;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for enforcing consent requirements in data sharing operations
 * Ensures all data access and sharing complies with client consent
 */
@Service
public class ConsentEnforcementService {
    
    private final ConsentRepository consentRepository;
    private final ConsentLedgerRepository ledgerRepository;
    
    @Autowired
    public ConsentEnforcementService(ConsentRepository consentRepository, 
                                   ConsentLedgerRepository ledgerRepository) {
        this.consentRepository = consentRepository;
        this.ledgerRepository = ledgerRepository;
    }
    
    /**
     * Validate that operation is authorized by client consent
     * @param clientId Client whose data is being accessed
     * @param operation Type of operation (e.g., "share", "export", "hmis")
     * @param recipientOrganization Organization receiving the data
     * @param requiredConsentTypes Required consent types for this operation
     * @return Validation result with details
     */
    public ConsentValidationResult validateOperation(ClientId clientId, String operation, 
                                                   String recipientOrganization,
                                                   ConsentType... requiredConsentTypes) {
        
        if (requiredConsentTypes.length == 0) {
            // Operation doesn't require specific consent
            return ConsentValidationResult.allowed("No consent required");
        }
        
        List<Consent> activeConsents = consentRepository.findActiveConsentsForClient(clientId);
        
        for (ConsentType requiredType : requiredConsentTypes) {
            Optional<Consent> matchingConsent = activeConsents.stream()
                .filter(consent -> consent.getConsentType() == requiredType)
                .filter(consent -> consent.isValidForUse())
                .filter(consent -> consent.authorizes(operation, recipientOrganization))
                .findFirst();
            
            if (matchingConsent.isEmpty()) {
                return ConsentValidationResult.denied(
                    String.format("Missing valid consent for %s to %s", 
                                requiredType.getDisplayName(), recipientOrganization),
                    requiredType
                );
            }
        }
        
        return ConsentValidationResult.allowed("All required consents validated");
    }
    
    /**
     * Validate HMIS data sharing operation
     */
    public ConsentValidationResult validateHMISOperation(ClientId clientId, String operation) {
        return validateOperation(clientId, operation, "HMIS", 
                               ConsentType.HMIS_PARTICIPATION, 
                               ConsentType.INFORMATION_SHARING);
    }
    
    /**
     * Validate court testimony operation
     */
    public ConsentValidationResult validateCourtTestimony(ClientId clientId, String courtOrganization) {
        return validateOperation(clientId, "court testimony", courtOrganization,
                               ConsentType.COURT_TESTIMONY);
    }
    
    /**
     * Validate medical information sharing
     */
    public ConsentValidationResult validateMedicalSharing(ClientId clientId, String healthcareOrganization) {
        return validateOperation(clientId, "medical sharing", healthcareOrganization,
                               ConsentType.MEDICAL_INFORMATION_SHARING);
    }
    
    /**
     * Validate referral sharing
     */
    public ConsentValidationResult validateReferralSharing(ClientId clientId, String referralOrganization) {
        return validateOperation(clientId, "referral", referralOrganization,
                               ConsentType.REFERRAL_SHARING,
                               ConsentType.INFORMATION_SHARING);
    }
    
    /**
     * Validate research participation
     */
    public ConsentValidationResult validateResearchParticipation(ClientId clientId, String researchOrganization) {
        return validateOperation(clientId, "research", researchOrganization,
                               ConsentType.RESEARCH_PARTICIPATION);
    }
    
    /**
     * Get all consents requiring renewal soon (within 30 days)
     */
    public List<Consent> getConsentsRequiringRenewal() {
        Instant thirtyDaysFromNow = Instant.now().plusSeconds(30 * 24 * 3600);
        return consentRepository.findConsentsExpiringBetween(Instant.now(), thirtyDaysFromNow);
    }
    
    /**
     * Expire all consents that have passed their expiration date
     */
    public void expireOverdueConsents() {
        List<Consent> expiredConsents = consentRepository.findExpiredConsents();
        
        for (Consent consent : expiredConsents) {
            consent.expireIfNeeded();
            consentRepository.save(consent);
        }
    }
    
    /**
     * Fast validation using read model for performance-critical operations
     * Note: This bypasses full domain logic for speed but should match domain behavior
     */
    public boolean hasValidConsentFast(ClientId clientId, ConsentType consentType, String recipientOrganization) {
        return ledgerRepository.hasValidConsentFor(
            clientId.value(), 
            consentType, 
            recipientOrganization, 
            ConsentStatus.GRANTED, 
            Instant.now()
        );
    }
    
    /**
     * Check if client has any VAWA-protected consents
     */
    public boolean hasVAWAProtectedConsents(ClientId clientId) {
        return consentRepository.findActiveConsentsForClient(clientId)
            .stream()
            .anyMatch(Consent::isVAWAProtected);
    }
    
    /**
     * Get consent summary for client
     */
    public ConsentSummary getConsentSummary(ClientId clientId) {
        List<Consent> allConsents = consentRepository.findAllConsentsForClient(clientId);
        List<Consent> activeConsents = allConsents.stream()
            .filter(Consent::isValidForUse)
            .toList();
        
        List<Consent> expiringConsents = allConsents.stream()
            .filter(consent -> {
                if (consent.getExpiresAt() == null) return false;
                Instant thirtyDaysFromNow = Instant.now().plusSeconds(30 * 24 * 3600);
                return consent.getExpiresAt().isBefore(thirtyDaysFromNow) && consent.isValidForUse();
            })
            .toList();
        
        return new ConsentSummary(
            allConsents.size(),
            activeConsents.size(),
            expiringConsents.size(),
            hasVAWAProtectedConsents(clientId),
            activeConsents,
            expiringConsents
        );
    }
    
    /**
     * Result of consent validation
     */
    public static class ConsentValidationResult {
        private final boolean allowed;
        private final String reason;
        private final ConsentType missingConsentType;
        
        private ConsentValidationResult(boolean allowed, String reason, ConsentType missingConsentType) {
            this.allowed = allowed;
            this.reason = reason;
            this.missingConsentType = missingConsentType;
        }
        
        public static ConsentValidationResult allowed(String reason) {
            return new ConsentValidationResult(true, reason, null);
        }
        
        public static ConsentValidationResult denied(String reason, ConsentType missingConsentType) {
            return new ConsentValidationResult(false, reason, missingConsentType);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        public ConsentType getMissingConsentType() { return missingConsentType; }
        
        public void throwIfDenied() {
            if (!allowed) {
                throw new ConsentRequiredException(reason, missingConsentType);
            }
        }
    }
    
    /**
     * Summary of client's consent status
     */
    public record ConsentSummary(
        int totalConsents,
        int activeConsents,
        int expiringConsents,
        boolean hasVAWAProtectedConsents,
        List<Consent> activeConsentsList,
        List<Consent> expiringConsentsList
    ) {}
    
    /**
     * Exception thrown when required consent is missing
     */
    public static class ConsentRequiredException extends RuntimeException {
        private final ConsentType missingConsentType;
        
        public ConsentRequiredException(String message, ConsentType missingConsentType) {
            super(message);
            this.missingConsentType = missingConsentType;
        }
        
        public ConsentType getMissingConsentType() {
            return missingConsentType;
        }
    }
}