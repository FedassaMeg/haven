package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.HumanName;
import org.haven.clientprofile.domain.Client.AdministrativeGender;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClientDemographicsUpdated(
    UUID clientId,
    HumanName name,
    AdministrativeGender gender,
    LocalDate birthDate,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return clientId;
    }
    
    @Override
    public String eventType() {
        return "ClientDemographicsUpdated";
    }
}
