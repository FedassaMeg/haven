package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.Client.AdministrativeGender;
import org.haven.shared.vo.Address;
import org.haven.shared.vo.ContactPoint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateClientCmd(
    @NotBlank(message = "Given name is required")
    @Size(max = 100, message = "Given name must not exceed 100 characters")
    String givenName,

    @NotBlank(message = "Family name is required")
    @Size(max = 100, message = "Family name must not exceed 100 characters")
    String familyName,

    @NotNull(message = "Gender is required")
    AdministrativeGender gender,

    LocalDate birthDate,

    List<Address> addresses,

    List<ContactPoint> telecoms
) {}
