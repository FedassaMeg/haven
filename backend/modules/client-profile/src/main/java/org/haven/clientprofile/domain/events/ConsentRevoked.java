package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ConsentRevoked(
    UUID clientId,
    String consentType,
    String revocationReason,
    String revokedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public ConsentRevoked(UUID clientId, String consentType, String revocationReason, String revokedBy) {
        this(clientId, consentType, revocationReason, revokedBy, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ConsentRevoked";
    }
}