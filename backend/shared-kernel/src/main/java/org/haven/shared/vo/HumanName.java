package org.haven.shared.vo;

import java.util.List;
import java.util.Objects;

/**
 * FHIR-inspired HumanName value object
 * Based on FHIR HumanName datatype
 */
public record HumanName(
    NameUse use,
    String family,
    List<String> given,
    List<String> prefix,
    List<String> suffix,
    String text
) {
    public HumanName {
        Objects.requireNonNull(use, "Name use cannot be null");
        Objects.requireNonNull(family, "Family name cannot be null");
        Objects.requireNonNull(given, "Given names cannot be null");
        
        if (family.trim().isEmpty()) {
            throw new IllegalArgumentException("Family name cannot be empty");
        }
        if (given.isEmpty()) {
            throw new IllegalArgumentException("At least one given name is required");
        }
    }

    public enum NameUse {
        USUAL, OFFICIAL, TEMP, NICKNAME, ANONYMOUS, OLD, MAIDEN
    }

    public String getFirstName() {
        return given.isEmpty() ? "" : given.get(0);
    }
    
    public String getLastName() {
        return family;
    }
    
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            sb.append(String.join(" ", prefix)).append(" ");
        }
        if (!given.isEmpty()) {
            sb.append(String.join(" ", given)).append(" ");
        }
        sb.append(family);
        if (suffix != null && !suffix.isEmpty()) {
            sb.append(" ").append(String.join(" ", suffix));
        }
        return sb.toString();
    }
}