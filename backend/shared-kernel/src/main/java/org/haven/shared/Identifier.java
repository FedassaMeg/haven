package org.haven.shared;

import java.util.Objects;
import java.util.UUID;

public abstract class Identifier {
    private final UUID value;

    protected Identifier(UUID value) {
        this.value = value;
    }

    public UUID getValue() {
        return value;
    }
    
    public UUID value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Identifier that = (Identifier) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public static UUID newUuid() {
        return UUID.randomUUID();
    }
}
