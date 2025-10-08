package org.haven.programenrollment.domain;

import org.haven.shared.Identifier;

import java.util.UUID;

public class ProjectLinkageId extends Identifier {

    private ProjectLinkageId(UUID value) {
        super(value);
    }

    public static ProjectLinkageId generate() {
        return new ProjectLinkageId(UUID.randomUUID());
    }

    public static ProjectLinkageId of(UUID value) {
        return new ProjectLinkageId(value);
    }

    public static ProjectLinkageId fromString(String value) {
        return new ProjectLinkageId(UUID.fromString(value));
    }
}