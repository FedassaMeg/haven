package org.haven.clientprofile.application.dto;

import org.haven.clientprofile.domain.Client.AdministrativeGender;
import org.haven.clientprofile.domain.Client.ClientStatus;
import org.haven.shared.vo.Address;
import org.haven.shared.vo.ContactPoint;
import org.haven.shared.vo.HumanName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientDto(
    UUID id,
    HumanName name,
    AdministrativeGender gender,
    LocalDate birthDate,
    List<Address> addresses,
    List<ContactPoint> telecoms,
    ClientStatus status,
    Instant createdAt
) {}