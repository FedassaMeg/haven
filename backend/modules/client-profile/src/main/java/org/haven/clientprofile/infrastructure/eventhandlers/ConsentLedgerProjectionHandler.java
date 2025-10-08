package org.haven.clientprofile.infrastructure.eventhandlers;

import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.domain.consent.events.*;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerEntity;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Event handler to maintain the consent ledger read model projection
 * Updates the ConsentLedgerEntity whenever consent domain events occur
 */
@Component
@Transactional
public class ConsentLedgerProjectionHandler {
    
    private final ConsentLedgerRepository ledgerRepository;
    
    public ConsentLedgerProjectionHandler(ConsentLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }
    
    @EventListener
    public void on(ConsentGranted event) {
        ConsentLedgerEntity ledgerEntry = new ConsentLedgerEntity(
            event.consentId(),
            event.clientId(),
            event.consentType(),
            ConsentStatus.GRANTED,
            event.purpose(),
            event.recipientOrganization(),
            event.recipientContact(),
            event.grantedAt(),
            event.expiresAt(),
            event.grantedByUserId(),
            event.isVAWAProtected(),
            event.limitations()
        );
        
        ledgerRepository.save(ledgerEntry);
    }
    
    @EventListener
    public void on(ConsentRevoked event) {
        Optional<ConsentLedgerEntity> ledgerEntry = ledgerRepository.findById(event.consentId());
        
        if (ledgerEntry.isPresent()) {
            ConsentLedgerEntity entry = ledgerEntry.get();
            entry.setStatus(ConsentStatus.REVOKED);
            entry.setRevokedAt(event.revokedAt());
            entry.setRevokedByUserId(event.revokedByUserId());
            entry.setRevocationReason(event.reason());
            
            ledgerRepository.save(entry);
        }
    }
    
    @EventListener
    public void on(ConsentUpdated event) {
        Optional<ConsentLedgerEntity> ledgerEntry = ledgerRepository.findById(event.consentId());
        
        if (ledgerEntry.isPresent()) {
            ConsentLedgerEntity entry = ledgerEntry.get();
            entry.setLimitations(event.newLimitations());
            entry.setRecipientContact(event.newRecipientContact());
            
            ledgerRepository.save(entry);
        }
    }
    
    @EventListener
    public void on(ConsentExtended event) {
        Optional<ConsentLedgerEntity> ledgerEntry = ledgerRepository.findById(event.consentId());
        
        if (ledgerEntry.isPresent()) {
            ConsentLedgerEntity entry = ledgerEntry.get();
            entry.setExpiresAt(event.newExpirationDate());
            
            ledgerRepository.save(entry);
        }
    }
    
    @EventListener
    public void on(ConsentExpired event) {
        Optional<ConsentLedgerEntity> ledgerEntry = ledgerRepository.findById(event.consentId());
        
        if (ledgerEntry.isPresent()) {
            ConsentLedgerEntity entry = ledgerEntry.get();
            entry.setStatus(ConsentStatus.EXPIRED);
            
            ledgerRepository.save(entry);
        }
    }
}