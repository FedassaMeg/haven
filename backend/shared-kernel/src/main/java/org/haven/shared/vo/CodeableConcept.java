package org.haven.shared.vo;

import java.util.List;
import java.util.Objects;

/**
 * FHIR-inspired CodeableConcept value object
 * Based on FHIR CodeableConcept datatype
 */
public record CodeableConcept(
    List<Coding> coding,
    String text
) {
    public CodeableConcept {
        Objects.requireNonNull(coding, "Coding list cannot be null");
        if (coding.isEmpty() && (text == null || text.trim().isEmpty())) {
            throw new IllegalArgumentException("Either coding or text must be provided");
        }
    }

    public record Coding(
        String system,
        String version,
        String code,
        String display,
        Boolean userSelected
    ) {
        public Coding {
            Objects.requireNonNull(code, "Code cannot be null");
            if (code.trim().isEmpty()) {
                throw new IllegalArgumentException("Code cannot be empty");
            }
        }
    }

    public boolean hasCoding(String system, String code) {
        return coding.stream().anyMatch(c -> 
            Objects.equals(c.system(), system) && Objects.equals(c.code(), code));
    }
}