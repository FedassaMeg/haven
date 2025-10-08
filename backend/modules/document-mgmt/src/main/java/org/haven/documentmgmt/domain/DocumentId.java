package org.haven.documentmgmt.domain;

import org.haven.shared.Identifier;

import java.util.UUID;

public class DocumentId extends Identifier {
    
    private DocumentId(UUID value) {
        super(value);
    }
    
    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }
    
    public static DocumentId from(String value) {
        return new DocumentId(UUID.fromString(value));
    }
    
    public static DocumentId from(UUID value) {
        return new DocumentId(value);
    }
}