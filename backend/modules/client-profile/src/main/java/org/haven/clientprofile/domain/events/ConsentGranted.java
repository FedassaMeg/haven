package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ConsentGranted(
    UUID clientId,
    String consentType,
    String purpose,
    Instant expirationDate,
    String grantedBy,
    Instant occurredAt
) implements DomainEvent {
    
    public ConsentGranted(UUID clientId, String consentType, String purpose, Instant expirationDate, String grantedBy) {
        this(clientId, consentType, purpose, expirationDate, grantedBy, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ConsentGranted";
    }
}