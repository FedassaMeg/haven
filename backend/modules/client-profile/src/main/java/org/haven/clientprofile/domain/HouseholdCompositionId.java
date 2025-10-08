package org.haven.clientprofile.domain;

import org.haven.shared.Identifier;

import java.util.UUID;

public class HouseholdCompositionId extends Identifier {
    
    private HouseholdCompositionId(UUID value) {
        super(value);
    }
    
    public static HouseholdCompositionId generate() {
        return new HouseholdCompositionId(UUID.randomUUID());
    }
    
    public static HouseholdCompositionId from(UUID value) {
        return new HouseholdCompositionId(value);
    }
    
    public static HouseholdCompositionId from(String value) {
        return new HouseholdCompositionId(UUID.fromString(value));
    }
}