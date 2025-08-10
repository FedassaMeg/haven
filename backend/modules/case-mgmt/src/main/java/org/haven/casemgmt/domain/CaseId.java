package org.haven.casemgmt.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class CaseId extends Identifier {
    public CaseId(UUID value) {
        super(value);
    }

    public static CaseId generate() {
        return new CaseId(UUID.randomUUID());
    }
}