package org.haven.casemgmt.domain;

import org.haven.shared.Identifier;

import java.util.UUID;

public class RestrictedNoteId extends Identifier {
    
    protected RestrictedNoteId(UUID value) {
        super(value);
    }
    
    public static RestrictedNoteId of(UUID value) {
        return new RestrictedNoteId(value);
    }
    
    public static RestrictedNoteId newId() {
        return new RestrictedNoteId(UUID.randomUUID());
    }
}