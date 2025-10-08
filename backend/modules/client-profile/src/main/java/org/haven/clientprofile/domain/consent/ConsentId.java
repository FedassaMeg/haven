package org.haven.clientprofile.domain.consent;

import org.haven.shared.Identifier;
import java.util.UUID;

/**
 * Value object for Consent aggregate identifier
 */
public class ConsentId extends Identifier {
    
    public ConsentId(UUID value) {
        super(value);
    }
    
    public static ConsentId newId() {
        return new ConsentId(UUID.randomUUID());
    }
    
    public static ConsentId fromString(String id) {
        return new ConsentId(UUID.fromString(id));
    }
}