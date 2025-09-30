package org.haven.api.config;

import org.haven.shared.vo.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for common security operations
 */
@Component
public class SecurityUtils {

    /**
     * Get the current authenticated username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }

    /**
     * Get current user ID if available
     */
    public UserId getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // Assuming principal contains user details with ID
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                return UserId.of(userDetails.getUsername()); // Or extract ID from UserDetails
            }
        }
        return UserId.system();
    }
}
