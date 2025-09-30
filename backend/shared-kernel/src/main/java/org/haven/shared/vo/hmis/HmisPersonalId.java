package org.haven.shared.vo.hmis;

import org.haven.shared.security.DeterministicIdGenerator;
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
    
    // Static instance for deterministic ID generation
    // TODO: In production, this should be configured with environment-specific salt
    private static final DeterministicIdGenerator ID_GENERATOR = new DeterministicIdGenerator();

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
     * Create from existing ClientId using deterministic hashing
     * Uses HMAC-SHA256 with salt to generate consistent, non-reversible PersonalIDs
     * This ensures PII hardening while maintaining referential integrity
     */
    public static HmisPersonalId fromClientId(UUID clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("ClientId cannot be null");
        }
        String hashedPersonalId = ID_GENERATOR.generateUuidFormattedPersonalId(clientId);
        return new HmisPersonalId(hashedPersonalId);
    }
    
    /**
     * Create from existing ClientId with custom salt
     * Allows for environment-specific salt configuration
     */
    public static HmisPersonalId fromClientId(UUID clientId, String salt) {
        if (clientId == null) {
            throw new IllegalArgumentException("ClientId cannot be null");
        }
        DeterministicIdGenerator customGenerator = new DeterministicIdGenerator(salt);
        String hashedPersonalId = customGenerator.generateUuidFormattedPersonalId(clientId);
        return new HmisPersonalId(hashedPersonalId);
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