package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProtectionOrderExpired(
    UUID caseId,
    UUID clientId,
    UUID protectionOrderId,
    LocalDate originalExpirationDate,
    LocalDate actualExpirationDate,
    boolean wasRenewed,
    UUID renewedOrderId,
    String expirationNotes,
    boolean autoExpired,
    Instant occurredAt
) implements DomainEvent {
    
    public ProtectionOrderExpired {
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (protectionOrderId == null) throw new IllegalArgumentException("Protection order ID cannot be null");
        if (originalExpirationDate == null) throw new IllegalArgumentException("Original expiration date cannot be null");
        if (actualExpirationDate == null) throw new IllegalArgumentException("Actual expiration date cannot be null");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return caseId;
    }
    
    @Override
    public String eventType() {
        return "ProtectionOrderExpired";
    }
}