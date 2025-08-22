package org.haven.clientprofile.application.handlers;

import org.haven.clientprofile.application.commands.*;
import org.haven.clientprofile.domain.consent.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handlers for consent operations
 * Ensures proper validation and enforcement of consent business rules
 */
@Service
@Transactional
public class ConsentCommandHandler {
    
    private final Repository<Consent, ConsentId> consentRepository;
    
    @Autowired
    public ConsentCommandHandler(Repository<Consent, ConsentId> consentRepository) {
        this.consentRepository = consentRepository;
    }
    
    /**
     * Handle grant consent command
     */
    public ConsentId handle(GrantConsentCmd command) {
        command.validate();
        
        ConsentId consentId = ConsentId.newId();
        ClientId clientId = new ClientId(command.clientId());
        
        Consent consent = Consent.grant(
            consentId,
            clientId,
            command.consentType(),
            command.purpose(),
            command.recipientOrganization(),
            command.recipientContact(),
            command.grantedByUserId(),
            command.durationMonths(),
            command.limitations()
        );
        
        consentRepository.save(consent);
        
        return consentId;
    }
    
    /**
     * Handle revoke consent command
     */
    public void handle(RevokeConsentCmd command) {
        command.validate();
        
        Consent consent = consentRepository.findById(new ConsentId(command.consentId()))
            .orElseThrow(() -> new ConsentNotFoundException("Consent not found: " + command.consentId()));
        
        consent.revoke(command.revokedByUserId(), command.reason());
        
        consentRepository.save(consent);
    }
    
    /**
     * Handle update consent command
     */
    public void handle(UpdateConsentCmd command) {
        Consent consent = consentRepository.findById(new ConsentId(command.consentId()))
            .orElseThrow(() -> new ConsentNotFoundException("Consent not found: " + command.consentId()));
        
        consent.update(
            command.newLimitations(),
            command.newRecipientContact(),
            command.updatedByUserId()
        );
        
        consentRepository.save(consent);
    }
    
    /**
     * Handle extend consent command
     */
    public void handle(ExtendConsentCmd command) {
        command.validate();
        
        Consent consent = consentRepository.findById(new ConsentId(command.consentId()))
            .orElseThrow(() -> new ConsentNotFoundException("Consent not found: " + command.consentId()));
        
        consent.extend(command.newExpirationDate(), command.extendedByUserId());
        
        consentRepository.save(consent);
    }
    
    /**
     * Exception for consent not found scenarios
     */
    public static class ConsentNotFoundException extends RuntimeException {
        public ConsentNotFoundException(String message) {
            super(message);
        }
    }
}