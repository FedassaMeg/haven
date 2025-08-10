package org.haven.clientprofile.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class HouseholdMemberId extends Identifier {
    public HouseholdMemberId(UUID value) {
        super(value);
    }

    public static HouseholdMemberId generate() {
        return new HouseholdMemberId(UUID.randomUUID());
    }
}