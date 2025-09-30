package org.haven.clientprofile.application.dto;

import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.domain.consent.ConsentType;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for consent ledger entries
 */
public record ConsentLedgerResponse(
    UUID id,
    UUID clientId,
    ConsentType consentType,
    ConsentStatus status,
    String purpose,
    String recipientOrganization,
    String recipientContact,
    Instant grantedAt,
    Instant expiresAt,
    Instant revokedAt,
    UUID grantedByUserId,
    UUID revokedByUserId,
    String revocationReason,
    boolean isVAWAProtected,
    String limitations,
    Instant lastUpdatedAt,
    
    // Computed fields
    boolean isExpired,
    boolean isExpiringSoon,
    long daysUntilExpiration
) {
    
    public static ConsentLedgerResponse fromEntity(ConsentLedgerEntity entity) {
        Instant now = Instant.now();
        boolean isExpired = entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(now);
        boolean isExpiringSoon = !isExpired && entity.getExpiresAt() != null && 
                                entity.getExpiresAt().isBefore(now.plusSeconds(30 * 24 * 60 * 60)); // 30 days
        long daysUntilExpiration = entity.getExpiresAt() != null ? 
            (entity.getExpiresAt().getEpochSecond() - now.getEpochSecond()) / (24 * 60 * 60) : -1;
        
        return new ConsentLedgerResponse(
            entity.getId(),
            entity.getClientId(),
            entity.getConsentType(),
            entity.getStatus(),
            entity.getPurpose(),
            entity.getRecipientOrganization(),
            entity.getRecipientContact(),
            entity.getGrantedAt(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getGrantedByUserId(),
            entity.getRevokedByUserId(),
            entity.getRevocationReason(),
            entity.isVAWAProtected(),
            entity.getLimitations(),
            entity.getLastUpdatedAt(),
            isExpired,
            isExpiringSoon,
            daysUntilExpiration
        );
    }
}