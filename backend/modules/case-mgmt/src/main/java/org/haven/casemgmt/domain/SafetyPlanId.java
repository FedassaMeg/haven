package org.haven.casemgmt.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class SafetyPlanId extends Identifier {
    
    public SafetyPlanId(UUID value) {
        super(value);
    }
    
    public static SafetyPlanId generate() {
        return new SafetyPlanId(UUID.randomUUID());
    }
    
    public static SafetyPlanId of(UUID value) {
        return new SafetyPlanId(value);
    }
}