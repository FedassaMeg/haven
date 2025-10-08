package org.haven.shared.security;

import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * UserContext provides access to the current user's authentication context
 */
@Component
public class UserContext {

    /**
     * Get the current authenticated user's username
     */
    public String getCurrentUser() {
        // In production, this would get from Spring Security context
        // For now, return a placeholder
        return "system";
    }

    /**
     * Get the current authenticated user's ID as UUID
     */
    public UUID getCurrentUserId() {
        // In production, this would get from Spring Security context
        // For now, return a deterministic UUID for system user
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String role) {
        // In production, this would check against Spring Security authorities
        // For now, assume admin access
        return true;
    }
}