package org.haven.shared.vo.hmis;

import java.util.Objects;
import java.util.UUID;

/**
 * HMIS Personal ID - Unique identifier for persons in HMIS
 * This is the primary key used to link records across different HMIS projects
 * and identify unique individuals for reporting purposes.
 * Aligned with HMIS 2024 Data Standards.
 */
public class HmisPersonalId {
    private final String value;

    private HmisPersonalId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("HMIS Personal ID cannot be null or empty");
        }
        this.value = value.trim();
    }

    /**
     * Create a new HMIS Personal ID from an existing value
     */
    public static HmisPersonalId of(String value) {
        return new HmisPersonalId(value);
    }

    /**
     * Generate a new HMIS Personal ID using UUID format
     */
    public static HmisPersonalId generate() {
        return new HmisPersonalId(UUID.randomUUID().toString());
    }

    /**
     * Create from existing ClientId for migration purposes
     */
    public static HmisPersonalId fromClientId(UUID clientId) {
        return new HmisPersonalId(clientId.toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HmisPersonalId that = (HmisPersonalId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "HmisPersonalId{" + value + "}";
    }
}