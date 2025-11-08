package org.haven.intake.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

/**
 * Identifier for PreIntakeContact aggregate
 */
public class PreIntakeContactId extends Identifier {

    public PreIntakeContactId(UUID value) {
        super(value);
    }

    public static PreIntakeContactId generate() {
        return new PreIntakeContactId(UUID.randomUUID());
    }

    public static PreIntakeContactId from(String value) {
        return new PreIntakeContactId(UUID.fromString(value));
    }

    public static PreIntakeContactId of(UUID value) {
        return new PreIntakeContactId(value);
    }
}
