package org.haven.clientprofile.infrastructure.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.haven.clientprofile.domain.DataSystem;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.UUID;

/**
 * Aspect-based enforcement of data system boundaries
 * Prevents VSP users from accessing HMIS PII data
 */
@Aspect
@Component
public class DataSystemBoundaryEnforcer {
    
    /**
     * Annotation to mark methods that require data system boundary checks
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EnforceDataSystemBoundary {
        DataSystem[] allowedSystems() default {DataSystem.HMIS, DataSystem.COMPARABLE_DB};
        boolean requiresPIIAccess() default false;
    }
    
    @Around("@annotation(enforceBoundary)")
    public Object enforceDataSystemBoundary(ProceedingJoinPoint joinPoint, 
                                          EnforceDataSystemBoundary enforceBoundary) throws Throwable {
        
        // Get current user context (would be injected via security context)
        UserSecurityContext userContext = getCurrentUserContext();
        
        if (userContext == null) {
            throw new DataSystemBoundaryException("No security context available");
        }
        
        // Check if user is VSP and trying to access HMIS data
        if (isVSPUser(userContext) && requiresHMISAccess(enforceBoundary)) {
            throw new DataSystemBoundaryException(
                "VSP users cannot access HMIS PII data. Use ComparableDB for victim service providers.");
        }
        
        // Check client-specific data system restrictions
        Object[] args = joinPoint.getArgs();
        UUID clientId = extractClientId(args);
        
        if (clientId != null && enforceBoundary.requiresPIIAccess()) {
            validateClientDataSystemAccess(userContext, clientId, enforceBoundary.allowedSystems());
        }
        
        // Log access attempt for audit
        logDataSystemAccess(userContext, joinPoint.getSignature().getName(), clientId);
        
        return joinPoint.proceed();
    }
    
    /**
     * Checks if user has VSP role
     */
    private boolean isVSPUser(UserSecurityContext userContext) {
        return userContext.getRoles().contains("VSP") || 
               userContext.getRoles().contains("VICTIM_SERVICE_PROVIDER");
    }
    
    /**
     * Checks if the operation requires HMIS access
     */
    private boolean requiresHMISAccess(EnforceDataSystemBoundary enforceBoundary) {
        List<DataSystem> allowedSystems = List.of(enforceBoundary.allowedSystems());
        return allowedSystems.contains(DataSystem.HMIS) && 
               !allowedSystems.contains(DataSystem.COMPARABLE_DB);
    }
    
    /**
     * Validates that user can access client data from specified data systems
     */
    private void validateClientDataSystemAccess(UserSecurityContext userContext, UUID clientId, 
                                              DataSystem[] allowedSystems) {
        
        // Check client's data system restrictions
        ClientDataSystemRestrictions restrictions = getClientDataSystemRestrictions(clientId);
        
        if (restrictions.isComparableDbOnly() && 
            List.of(allowedSystems).contains(DataSystem.HMIS)) {
            throw new DataSystemBoundaryException(
                "Client data restricted to ComparableDB only - HMIS access denied");
        }
        
        // VSP users can only access ComparableDB data
        if (isVSPUser(userContext) && 
            !restrictions.isComparableDbOnly() && 
            List.of(allowedSystems).contains(DataSystem.HMIS)) {
            throw new DataSystemBoundaryException(
                "VSP users cannot access clients with HMIS data");
        }
    }
    
    /**
     * Extracts client ID from method arguments
     */
    private UUID extractClientId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
            // Check for client ID in common parameter objects
            if (arg != null) {
                try {
                    var clientIdField = arg.getClass().getDeclaredField("clientId");
                    clientIdField.setAccessible(true);
                    Object value = clientIdField.get(arg);
                    if (value instanceof UUID) {
                        return (UUID) value;
                    }
                } catch (Exception e) {
                    // Field doesn't exist or not accessible, continue
                }
            }
        }
        return null;
    }
    
    /**
     * Gets current user security context
     * In real implementation, this would use Spring Security
     */
    private UserSecurityContext getCurrentUserContext() {
        // Placeholder - would integrate with Spring Security
        // return SecurityContextHolder.getContext().getAuthentication();
        return UserSecurityContext.getCurrentUser();
    }
    
    /**
     * Gets client data system restrictions
     */
    private ClientDataSystemRestrictions getClientDataSystemRestrictions(UUID clientId) {
        // This would query the confidentiality_guardrails table
        // For now, return a default implementation
        return new ClientDataSystemRestrictions(clientId, false);
    }
    
    /**
     * Logs data system access for audit trail
     */
    private void logDataSystemAccess(UserSecurityContext userContext, String methodName, UUID clientId) {
        // Log to audit system
        System.out.println(String.format(
            "Data system access: User=%s, Method=%s, ClientId=%s, Roles=%s",
            userContext.getUserId(), methodName, clientId, userContext.getRoles()));
    }
    
    /**
     * User security context holder
     */
    public static class UserSecurityContext {
        private final UUID userId;
        private final List<String> roles;
        private final String organizationId;
        
        public UserSecurityContext(UUID userId, List<String> roles, String organizationId) {
            this.userId = userId;
            this.roles = roles;
            this.organizationId = organizationId;
        }
        
        public UUID getUserId() { return userId; }
        public List<String> getRoles() { return roles; }
        public String getOrganizationId() { return organizationId; }
        
        // Placeholder for getting current user
        public static UserSecurityContext getCurrentUser() {
            // In real implementation, would get from Spring Security context
            return new UserSecurityContext(
                UUID.randomUUID(), 
                List.of("CASE_MANAGER"), 
                "org1"
            );
        }
    }
    
    /**
     * Client data system restrictions
     */
    public static class ClientDataSystemRestrictions {
        private final UUID clientId;
        private final boolean isComparableDbOnly;
        
        public ClientDataSystemRestrictions(UUID clientId, boolean isComparableDbOnly) {
            this.clientId = clientId;
            this.isComparableDbOnly = isComparableDbOnly;
        }
        
        public UUID getClientId() { return clientId; }
        public boolean isComparableDbOnly() { return isComparableDbOnly; }
    }
    
    /**
     * Exception for data system boundary violations
     */
    public static class DataSystemBoundaryException extends RuntimeException {
        public DataSystemBoundaryException(String message) {
            super(message);
        }
        
        public DataSystemBoundaryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}