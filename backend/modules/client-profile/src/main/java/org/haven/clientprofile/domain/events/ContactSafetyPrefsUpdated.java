package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.ContactSafetyPrefs;
import java.time.Instant;
import java.util.UUID;

public class ContactSafetyPrefsUpdated extends DomainEvent {
    private final UUID clientId;
    private final ContactSafetyPrefs contactSafetyPrefs;

    public ContactSafetyPrefsUpdated(UUID clientId, ContactSafetyPrefs contactSafetyPrefs, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.contactSafetyPrefs = contactSafetyPrefs;
    }

    public ContactSafetyPrefsUpdated(UUID clientId, ContactSafetyPrefs contactSafetyPrefs) {
        this(clientId, contactSafetyPrefs, Instant.now());
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public ContactSafetyPrefs contactSafetyPrefs() {
        return contactSafetyPrefs;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public ContactSafetyPrefs getContactSafetyPrefs() {
        return contactSafetyPrefs;
    }
}