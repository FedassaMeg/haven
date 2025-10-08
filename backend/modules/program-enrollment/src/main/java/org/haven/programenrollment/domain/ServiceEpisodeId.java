package org.haven.programenrollment.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class ServiceEpisodeId extends Identifier {
    
    public ServiceEpisodeId(UUID value) {
        super(value);
    }
    
    public static ServiceEpisodeId generate() {
        return new ServiceEpisodeId(UUID.randomUUID());
    }
    
    public static ServiceEpisodeId of(UUID value) {
        return new ServiceEpisodeId(value);
    }
}