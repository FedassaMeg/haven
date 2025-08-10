package org.haven.shared.vo;

import java.util.Objects;

/**
 * FHIR-inspired ContactPoint value object
 * Based on FHIR ContactPoint datatype
 */
public record ContactPoint(
    ContactSystem system,
    String value,
    ContactUse use,
    Integer rank
) {
    public ContactPoint {
        Objects.requireNonNull(system, "Contact system cannot be null");
        Objects.requireNonNull(value, "Contact value cannot be null");
        Objects.requireNonNull(use, "Contact use cannot be null");
        
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact value cannot be empty");
        }
    }

    public enum ContactSystem {
        PHONE, FAX, EMAIL, PAGER, URL, SMS, OTHER
    }

    public enum ContactUse {
        HOME, WORK, TEMP, OLD, MOBILE
    }
}