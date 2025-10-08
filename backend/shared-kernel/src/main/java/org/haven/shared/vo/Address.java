package org.haven.shared.vo;

import java.util.Objects;

/**
 * FHIR-inspired Address value object
 * Based on FHIR Address datatype
 */
public record Address(
    String line1,
    String line2,
    String city,
    String state,
    String postalCode,
    String country,
    AddressType type,
    AddressUse use
) {
    public Address {
        Objects.requireNonNull(line1, "Address line1 cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");
        Objects.requireNonNull(type, "Address type cannot be null");
        Objects.requireNonNull(use, "Address use cannot be null");
    }

    public enum AddressType {
        POSTAL, PHYSICAL, BOTH
    }

    public enum AddressUse {
        HOME, WORK, TEMP, OLD, BILLING
    }
}