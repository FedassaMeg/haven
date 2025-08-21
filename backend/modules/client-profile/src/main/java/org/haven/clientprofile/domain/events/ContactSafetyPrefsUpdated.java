package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.ContactSafetyPrefs;
import java.time.Instant;
import java.util.UUID;

public record ContactSafetyPrefsUpdated(
    UUID clientId,
    ContactSafetyPrefs contactSafetyPrefs,
    Instant occurredAt
) implements DomainEvent {
    
    public ContactSafetyPrefsUpdated(UUID clientId, ContactSafetyPrefs contactSafetyPrefs) {
        this(clientId, contactSafetyPrefs, Instant.now());
    }
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ContactSafetyPrefsUpdated";
    }
}