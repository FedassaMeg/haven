package org.haven.safetyassessment.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class SafetyAssessmentId extends Identifier {
    
    public SafetyAssessmentId(UUID value) {
        super(value);
    }
    
    public static SafetyAssessmentId generate() {
        return new SafetyAssessmentId(UUID.randomUUID());
    }
    
    public static SafetyAssessmentId of(UUID value) {
        return new SafetyAssessmentId(value);
    }
}