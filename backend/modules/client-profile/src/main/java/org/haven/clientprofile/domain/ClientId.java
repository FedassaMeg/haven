package org.haven.clientprofile.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class ClientId extends Identifier {
    
    public ClientId(UUID value) {
        super(value);
    }
    
    public static ClientId generate() {
        return new ClientId(UUID.randomUUID());
    }
    
    public static ClientId from(String value) {
        return new ClientId(UUID.fromString(value));
    }
}
