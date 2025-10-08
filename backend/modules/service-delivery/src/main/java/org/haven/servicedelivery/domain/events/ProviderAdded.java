package org.haven.servicedelivery.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class ProviderAdded extends DomainEvent {
    private final String providerId;
    private final String providerName;
    private final String role;

    public ProviderAdded(UUID episodeId, String providerId, String providerName, String role, Instant occurredAt) {
        super(episodeId, occurredAt);
        this.providerId = providerId;
        this.providerName = providerName;
        this.role = role;
    }

    public String providerId() {
        return providerId;
    }

    public String providerName() {
        return providerName;
    }

    public String role() {
        return role;
    }


    // JavaBean-style getters
    public String getProviderId() { return providerId; }
    public String getProviderName() { return providerName; }
    public String getRole() { return role; }
}