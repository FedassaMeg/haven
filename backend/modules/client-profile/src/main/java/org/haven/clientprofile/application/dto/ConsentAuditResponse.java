package org.haven.clientprofile.application.dto;

import org.haven.clientprofile.domain.consent.ConsentType;
import org.haven.clientprofile.infrastructure.persistence.ConsentAuditTrailEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for consent audit trail entries
 */
public record ConsentAuditResponse(
    Long id,
    UUID consentId,
    UUID clientId,
    String eventType,
    ConsentType consentType,
    UUID actingUserId,
    Instant occurredAt,
    String eventData,
    String reason,
    String recipientOrganization,
    String ipAddress,
    String userAgent
) {
    
    public static ConsentAuditResponse fromEntity(ConsentAuditTrailEntity entity) {
        return new ConsentAuditResponse(
            entity.getId(),
            entity.getConsentId(),
            entity.getClientId(),
            entity.getEventType(),
            entity.getConsentType(),
            entity.getActingUserId(),
            entity.getOccurredAt(),
            entity.getEventData(),
            entity.getReason(),
            entity.getRecipientOrganization(),
            entity.getIpAddress(),
            entity.getUserAgent()
        );
    }
}