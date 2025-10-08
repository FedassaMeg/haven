package org.haven.casemgmt.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class LegalAdvocacyId extends Identifier {
    
    public LegalAdvocacyId(UUID value) {
        super(value);
    }
    
    public static LegalAdvocacyId generate() {
        return new LegalAdvocacyId(UUID.randomUUID());
    }
    
    public static LegalAdvocacyId of(UUID value) {
        return new LegalAdvocacyId(value);
    }
}