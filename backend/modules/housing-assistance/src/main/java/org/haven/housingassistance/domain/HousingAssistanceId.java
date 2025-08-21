package org.haven.housingassistance.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class HousingAssistanceId extends Identifier {
    
    public HousingAssistanceId(UUID value) {
        super(value);
    }
    
    public static HousingAssistanceId generate() {
        return new HousingAssistanceId(UUID.randomUUID());
    }
    
    public static HousingAssistanceId of(UUID value) {
        return new HousingAssistanceId(value);
    }
}