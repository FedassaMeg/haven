package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.HumanName;
import org.haven.clientprofile.domain.Client.AdministrativeGender;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ClientCreated extends DomainEvent {
    private final UUID clientId;
    private final HumanName name;
    private final AdministrativeGender gender;
    private final LocalDate birthDate;

    public ClientCreated(UUID clientId, HumanName name, AdministrativeGender gender, LocalDate birthDate, Instant occurredAt) {
        super(clientId, occurredAt);
        this.clientId = clientId;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    // Record-style accessors (for backward compatibility)
    public UUID clientId() {
        return clientId;
    }

    public HumanName name() {
        return name;
    }

    public AdministrativeGender gender() {
        return gender;
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    // JavaBean-style getters
    public UUID getClientId() {
        return clientId;
    }

    public HumanName getName() {
        return name;
    }

    public AdministrativeGender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}
