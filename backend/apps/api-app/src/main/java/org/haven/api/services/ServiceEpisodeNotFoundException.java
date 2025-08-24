package org.haven.api.services;

import java.util.UUID;

public class ServiceEpisodeNotFoundException extends RuntimeException {
    
    public ServiceEpisodeNotFoundException(UUID episodeId) {
        super("Service episode not found with ID: " + episodeId);
    }
    
    public ServiceEpisodeNotFoundException(String message) {
        super(message);
    }
    
    public ServiceEpisodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}