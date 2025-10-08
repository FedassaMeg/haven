package org.haven.clientprofile.infrastructure.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.ConsentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

/**
 * Aspect for enforcing consent requirements on data sharing operations
 * Automatically validates consent before allowing data access
 */
@Aspect
@Component
public class ConsentEnforcementAspect {
    
    private final ConsentEnforcementService consentEnforcementService;
    
    @Autowired
    public ConsentEnforcementAspect(ConsentEnforcementService consentEnforcementService) {
        this.consentEnforcementService = consentEnforcementService;
    }
    
    /**
     * Intercept methods annotated with @RequiresConsent
     */
    @Around("@annotation(requiresConsent)")
    public Object enforceConsent(ProceedingJoinPoint joinPoint, RequiresConsent requiresConsent) throws Throwable {
        
        // Extract client ID from method parameters
        UUID clientId = extractClientId(joinPoint);
        if (clientId == null) {
            throw new IllegalArgumentException("Cannot enforce consent: no client ID found in method parameters");
        }
        
        // Validate consent
        ConsentEnforcementService.ConsentValidationResult result = 
            consentEnforcementService.validateOperation(
                new ClientId(clientId),
                requiresConsent.operation(),
                requiresConsent.recipientOrganization(),
                requiresConsent.requiredConsentTypes()
            );
        
        // Throw exception if consent is not valid
        result.throwIfDenied();
        
        // Log consent validation for audit
        logConsentValidation(clientId, requiresConsent.operation(), result);
        
        // Proceed with the method execution
        return joinPoint.proceed();
    }
    
    /**
     * Extract client ID from method parameters
     * Looks for UUID parameter named 'clientId' or ClientId type parameter
     */
    private UUID extractClientId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);
        
        // Look for parameter named 'clientId'
        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if ("clientId".equals(paramNames[i]) && args[i] instanceof UUID) {
                return (UUID) args[i];
            }
        }
        
        // Look for ClientId type parameter
        for (Object arg : args) {
            if (arg instanceof ClientId) {
                return ((ClientId) arg).value();
            }
        }
        
        // Look for UUID type parameter (assume first UUID is client ID)
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }
        
        return null;
    }
    
    /**
     * Get parameter names from method signature
     * In production, this would use reflection or compile-time parameter name retention
     */
    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // Simplified implementation - in real scenario would use reflection
        // or Spring's parameter name discovery
        return new String[]{"clientId", "data", "context", "exportType"};
    }
    
    /**
     * Log consent validation for audit trail
     */
    private void logConsentValidation(UUID clientId, String operation, 
                                    ConsentEnforcementService.ConsentValidationResult result) {
        System.out.println(String.format(
            "CONSENT_VALIDATION: ClientId=%s, Operation=%s, Result=%s, Reason=%s, Time=%s",
            clientId,
            operation,
            result.isAllowed() ? "ALLOWED" : "DENIED",
            result.getReason(),
            java.time.Instant.now()
        ));
    }
    
    /**
     * Annotation to mark methods that require consent validation
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiresConsent {
        
        /**
         * Operation being performed (for consent authorization check)
         */
        String operation();
        
        /**
         * Organization or recipient of the data (optional)
         */
        String recipientOrganization() default "";
        
        /**
         * Required consent types for this operation
         */
        ConsentType[] requiredConsentTypes();
        
        /**
         * Whether to allow operation if no consent exists but none is required
         */
        boolean allowWithoutConsent() default false;
    }
}