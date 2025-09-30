package org.haven.shared.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight value object wrapper around a user identifier.
 * Provides basic helpers needed by legacy services that previously relied on housing assistance types.
 */
public final class UserId {

    private final String value;

    private UserId(String value) {
        this.value = value;
    }

    public static UserId of(String value) {
        return new UserId(Objects.requireNonNullElse(value, "system"));
    }

    public static UserId fromUuid(UUID uuid) {
        return new UserId(uuid != null ? uuid.toString() : "system");
    }

    public static UserId system() {
        return new UserId("system");
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
