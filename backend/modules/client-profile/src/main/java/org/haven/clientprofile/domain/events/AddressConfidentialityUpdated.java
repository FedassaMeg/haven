package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.clientprofile.domain.AddressConfidentiality;
import java.time.Instant;
import java.util.UUID;

public class AddressConfidentialityUpdated extends DomainEvent {
    private final UUID clientId;
    private final AddressConfidentiality previousAddressConfidentiality;
    private final AddressConfidentiality newAddressConfidentiality;
    private final String updatedBy;
    private final UUID updatedByUserId;
    private final String updateReason;

    public AddressConfidentialityUpdated(
        UUID clientId,
        AddressConfidentiality previousAddressConfidentiality,
        AddressConfidentiality newAddressConfidentiality,
        String updatedBy,
        UUID updatedByUserId,
        String updateReason,
        Instant occurredAt
    ) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.previousAddressConfidentiality = previousAddressConfidentiality;
        this.newAddressConfidentiality = newAddressConfidentiality;
        this.updatedBy = updatedBy;
        this.updatedByUserId = updatedByUserId;
        this.updateReason = updateReason;
    }

    public AddressConfidentialityUpdated(
        UUID clientId,
        AddressConfidentiality previousAddressConfidentiality,
        AddressConfidentiality newAddressConfidentiality,
        String updatedBy,
        UUID updatedByUserId,
        String updateReason
    ) {
        this(clientId, previousAddressConfidentiality, newAddressConfidentiality, updatedBy, updatedByUserId, updateReason, Instant.now());
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public AddressConfidentiality previousAddressConfidentiality() {
        return previousAddressConfidentiality;
    }

    public AddressConfidentiality newAddressConfidentiality() {
        return newAddressConfidentiality;
    }

    public String updatedBy() {
        return updatedBy;
    }

    public UUID updatedByUserId() {
        return updatedByUserId;
    }

    public String updateReason() {
        return updateReason;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public AddressConfidentiality getPreviousAddressConfidentiality() {
        return previousAddressConfidentiality;
    }

    public AddressConfidentiality getNewAddressConfidentiality() {
        return newAddressConfidentiality;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public UUID getUpdatedByUserId() {
        return updatedByUserId;
    }

    public String getUpdateReason() {
        return updateReason;
    }
}