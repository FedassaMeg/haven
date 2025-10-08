package org.haven.casemgmt.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class ProgramEnrollmentId extends Identifier {
    
    public ProgramEnrollmentId(UUID value) {
        super(value);
    }
    
    public static ProgramEnrollmentId generate() {
        return new ProgramEnrollmentId(UUID.randomUUID());
    }
    
    public static ProgramEnrollmentId of(UUID value) {
        return new ProgramEnrollmentId(value);
    }
}