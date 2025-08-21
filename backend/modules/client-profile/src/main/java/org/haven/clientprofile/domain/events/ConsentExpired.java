package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsentExpired(
    UUID clientId,
    String consentType,
    LocalDate originalExpirationDate,
    String expiredConsentPurpose,
    boolean autoExpired,
    Instant occurredAt
) implements DomainEvent {
    
    public ConsentExpired(UUID clientId, String consentType, LocalDate originalExpirationDate, String expiredConsentPurpose, boolean autoExpired) {
        this(clientId, consentType, originalExpirationDate, expiredConsentPurpose, autoExpired, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ConsentExpired";
    }
}