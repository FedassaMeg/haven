package org.haven.programenrollment.domain.ce;

import org.haven.shared.Identifier;
import java.util.UUID;

/**
 * Identifier for consent-bound CE packet snapshots.
 */
public class CePacketId extends Identifier {

    public CePacketId(UUID value) {
        super(value);
    }

    public static CePacketId newId() {
        return new CePacketId(UUID.randomUUID());
    }

    public static CePacketId of(UUID value) {
        return new CePacketId(value);
    }
}
