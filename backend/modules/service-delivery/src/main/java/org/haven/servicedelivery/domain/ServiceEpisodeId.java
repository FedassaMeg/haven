package org.haven.servicedelivery.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

/**
 * Service Episode identifier
 */
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
    
    public static ServiceEpisodeId from(String value) {
        return new ServiceEpisodeId(UUID.fromString(value));
    }
}