package org.haven.api.intake.dto;

import org.haven.clientprofile.domain.Client.AdministrativeGender;
import org.haven.shared.vo.Address;
import org.haven.shared.vo.ContactPoint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PromoteClientRequest(
    @NotNull(message = "Temp client ID is required")
    UUID tempClientId,

    @NotBlank(message = "Given name is required")
    @Size(max = 100, message = "Given name must not exceed 100 characters")
    String givenName,

    @NotBlank(message = "Family name is required")
    @Size(max = 100, message = "Family name must not exceed 100 characters")
    String familyName,

    @NotNull(message = "Gender is required")
    AdministrativeGender gender,

    LocalDate birthDate,

    List<AddressDto> addresses,

    List<ContactPointDto> telecoms,

    boolean vawaProtected,

    String socialSecurityNumber
) {
    public static record AddressDto(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
    ) {
        public Address toValueObject() {
            return new Address(
                line1 != null ? line1 : "",
                line2,
                city != null ? city : "",
                state != null ? state : "",
                postalCode != null ? postalCode : "",
                country != null ? country : "US",
                Address.AddressType.BOTH,
                Address.AddressUse.HOME
            );
        }
    }

    public static record ContactPointDto(
        @NotNull String system,
        @NotNull String value,
        String use
    ) {
        public ContactPoint toValueObject() {
            ContactPoint.ContactSystem contactSystem =
                ContactPoint.ContactSystem.valueOf(system.toUpperCase());
            ContactPoint.ContactUse contactUse = use != null ?
                ContactPoint.ContactUse.valueOf(use.toUpperCase()) :
                ContactPoint.ContactUse.HOME;

            return new ContactPoint(contactSystem, value, contactUse, null);
        }
    }
}
