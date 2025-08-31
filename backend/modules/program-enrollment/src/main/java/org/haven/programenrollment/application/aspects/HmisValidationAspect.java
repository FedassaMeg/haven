package org.haven.programenrollment.application.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.haven.programenrollment.application.security.HmisDataSecurityService;
import org.haven.programenrollment.application.security.HmisAuditLogger;
import org.haven.programenrollment.application.validation.*;
import org.haven.shared.validation.BusinessRuleValidator.ValidationResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.List;

/**
 * AOP Aspect for comprehensive HMIS validation and security enforcement
 * Provides cross-cutting concerns for data validation, security checks, and audit logging
 * Automatically applied to all HMIS data operations
 */
@Aspect
@Component
public class HmisValidationAspect {
    
    private final HmisDataSecurityService securityService;
    private final HmisAuditLogger auditLogger;
    private final DisabilityValidationService disabilityValidationService;
    private final DvValidationService dvValidationService;
    
    public HmisValidationAspect(HmisDataSecurityService securityService,
                               HmisAuditLogger auditLogger,
                               DisabilityValidationService disabilityValidationService,
                               DvValidationService dvValidationService) {
        this.securityService = securityService;
        this.auditLogger = auditLogger;
        this.disabilityValidationService = disabilityValidationService;
        this.dvValidationService = dvValidationService;
    }
    
    /**
     * Unified security validation for POST operations in enrollment controllers.
     * Avoids direct references to specific controller classes to prevent bean cycles.
     */
    @Before("@annotation(org.springframework.web.bind.annotation.PostMapping) && @within(org.springframework.web.bind.annotation.RestController)")
    public void validatePostMappingAccess(JoinPoint joinPoint) {
        UUID enrollmentId = extractEnrollmentId(joinPoint);
        String operation = extractOperation(joinPoint);
        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String dataType = extractDataTypeFromController(controllerName);

        switch (dataType) {
            case "DISABILITY" -> {
                if (!securityService.canAccessDisabilityData(enrollmentId, operation)) {
                    throw new AccessDeniedException("Insufficient permissions for disability data access");
                }
            }
            case "DOMESTIC_VIOLENCE" -> {
                HmisDataSecurityService.DvDataSensitivityLevel sensitivityLevel =
                    determineDvSensitivityLevel(operation);
                if (!securityService.canAccessDvData(enrollmentId, operation, sensitivityLevel)) {
                    throw new AccessDeniedException("Insufficient permissions for DV data access");
                }
            }
            case "CURRENT_LIVING_SITUATION" -> {
                if (!securityService.canAccessCurrentLivingSituationData(enrollmentId, operation)) {
                    throw new AccessDeniedException("Insufficient permissions for current living situation data access");
                }
            }
            case "DATE_OF_ENGAGEMENT" -> {
                if (!securityService.canAccessDateOfEngagementData(enrollmentId, operation)) {
                    throw new AccessDeniedException("Insufficient permissions for date of engagement data access");
                }
            }
            case "BED_NIGHT" -> {
                if (!securityService.canAccessBedNightData(enrollmentId, operation)) {
                    throw new AccessDeniedException("Insufficient permissions for bed night data access");
                }
            }
            default -> {
                // No access decision for unknown types.
            }
        }
    }
    
    /**
     * Validate data corrections with enhanced security
     */
    @Before("execution(* *..*Service.create*CorrectionRecord(..))")
    public void validateDataCorrectionAccess(JoinPoint joinPoint) {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String dataType = extractDataTypeFromService(serviceName);
        UUID recordId = extractRecordId(joinPoint);
        
        if (!securityService.canPerformDataCorrections(dataType, recordId)) {
            throw new AccessDeniedException("Insufficient permissions for data corrections");
        }
    }
    
    /**
     * Validate bulk operations
     */
    @Before("execution(* *..*Controller.*Bulk*(..))")
    public void validateBulkOperationAccess(JoinPoint joinPoint) {
        String operation = joinPoint.getSignature().getName();
        
        if (!securityService.canPerformBulkOperations(operation)) {
            throw new AccessDeniedException("Insufficient permissions for bulk operations");
        }
    }
    
    /**
     * Time-based access validation for sensitive operations
     */
    @Before("(@within(org.springframework.web.bind.annotation.RestController) && execution(* *..*Controller.createSafetyAlert(..))) || " +
            "execution(* *..*Service.create*SafetyAlert(..))")
    public void validateTimeBasedAccess(JoinPoint joinPoint) {
        String operation = joinPoint.getSignature().getName();
        UUID resourceId = extractResourceId(joinPoint);
        
        if (!securityService.validateTimeBasedAccess(operation, resourceId)) {
            throw new AccessDeniedException("Operation not permitted outside business hours");
        }
    }
    
    /**
     * Log successful operations
     */
    @AfterReturning(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)", returning = "result")
    public void logSuccessfulOperation(JoinPoint joinPoint, Object result) {
        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String operation = joinPoint.getSignature().getName();
        String dataType = extractDataTypeFromController(controllerName);
        UUID resourceId = extractResourceId(joinPoint);
        
        auditLogger.logDataAccess(dataType, resourceId, operation, getCurrentUsername());
    }
    
    /**
     * Log failed operations
     */
    @AfterThrowing(pointcut = "within(@org.springframework.web.bind.annotation.RestController *)", throwing = "exception")
    public void logFailedOperation(JoinPoint joinPoint, Exception exception) {
        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String operation = joinPoint.getSignature().getName();
        String dataType = extractDataTypeFromController(controllerName);
        UUID resourceId = extractResourceId(joinPoint);
        
        if (exception instanceof AccessDeniedException) {
            auditLogger.logUnauthorizedAccess(dataType, resourceId, operation, exception.getMessage());
        } else if (exception instanceof org.haven.shared.validation.BusinessRuleValidator.BusinessRuleViolationException) {
            var validationException = (org.haven.shared.validation.BusinessRuleValidator.BusinessRuleViolationException) exception;
            auditLogger.logValidationFailure(dataType, resourceId, operation, getCurrentUsername(), 
                validationException.getViolations());
        }
    }
    
    /**
     * Log data modifications with old and new values
     */
    @AfterReturning(pointcut = "execution(* *..*Service.create*(..) || * *..*Service.update*(..))", returning = "result")
    public void logDataModification(JoinPoint joinPoint, Object result) {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String dataType = extractDataTypeFromService(serviceName);
        String operation = joinPoint.getSignature().getName();
        
        // Extract resource ID from result if possible
        UUID resourceId = extractResourceIdFromResult(result);
        
        auditLogger.logDataModification(dataType, resourceId, operation, getCurrentUsername(), null, result);
    }
    
    /**
     * Log corrections with detailed tracking
     */
    @AfterReturning(pointcut = "execution(* *..*Service.create*CorrectionRecord(..))", returning = "result")
    public void logDataCorrection(JoinPoint joinPoint, Object result) {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String dataType = extractDataTypeFromService(serviceName);
        
        // Extract original record ID and correction details from arguments
        Object[] args = joinPoint.getArgs();
        UUID originalRecordId = null;
        String correctionReason = "Correction applied";
        
        if (args.length > 0 && args[0] instanceof UUID) {
            originalRecordId = (UUID) args[0];
        }
        
        UUID correctionRecordId = extractResourceIdFromResult(result);
        
        auditLogger.logDataCorrection(dataType, originalRecordId, correctionRecordId, 
            getCurrentUsername(), correctionReason, null, result);
    }
    
    /**
     * Log DV high-risk events
     */
    @AfterReturning(pointcut = "@within(org.springframework.web.bind.annotation.RestController) && execution(* *..*Controller.createSafetyAlert(..))")
    public void logDvHighRiskEvent(JoinPoint joinPoint) {
        UUID enrollmentId = extractEnrollmentId(joinPoint);
        
        auditLogger.logDvHighRiskEvent("SAFETY_ALERT_CREATED", null, enrollmentId, 
            getCurrentUsername(), "Safety alert created through API");
    }
    
    /**
     * Log bulk operations with detailed metrics
     */
    @AfterReturning(pointcut = "execution(* *..*Controller.*Bulk*(..))", returning = "result")
    public void logBulkOperation(JoinPoint joinPoint, Object result) {
        String operation = joinPoint.getSignature().getName();
        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String dataType = extractDataTypeFromController(controllerName);
        
        // Extract counts from result (this would need to be adapted based on actual response structure)
        int totalCount = extractTotalCountFromBulkResult(result);
        int successCount = extractSuccessCountFromBulkResult(result);
        int failureCount = totalCount - successCount;
        
        auditLogger.logBulkOperation(operation, dataType, totalCount, successCount, failureCount, getCurrentUsername());
    }
    
    // Helper methods
    
    private UUID extractEnrollmentId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof UUID) {
            return (UUID) args[0];
        }
        return null;
    }
    
    private UUID extractRecordId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }
        return null;
    }
    
    private UUID extractResourceId(JoinPoint joinPoint) {
        return extractEnrollmentId(joinPoint); // Default to enrollment ID
    }
    
    private UUID extractResourceIdFromResult(Object result) {
        if (result != null) {
            try {
                // Use reflection to find ID field in response objects
                var recordIdField = result.getClass().getDeclaredField("recordId");
                recordIdField.setAccessible(true);
                return (UUID) recordIdField.get(result);
            } catch (Exception e) {
                // Ignore and return null
            }
        }
        return null;
    }
    
    private String extractOperation(JoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }
    
    private String extractDataTypeFromController(String controllerName) {
        if (controllerName.contains("Disability")) return "DISABILITY";
        if (controllerName.contains("Dv")) return "DOMESTIC_VIOLENCE";
        if (controllerName.contains("CurrentLivingSituation")) return "CURRENT_LIVING_SITUATION";
        if (controllerName.contains("DateOfEngagement")) return "DATE_OF_ENGAGEMENT";
        if (controllerName.contains("BedNight")) return "BED_NIGHT";
        return "UNKNOWN";
    }
    
    private String extractDataTypeFromService(String serviceName) {
        if (serviceName.contains("Disability")) return "DISABILITY";
        if (serviceName.contains("Dv")) return "DOMESTIC_VIOLENCE";
        if (serviceName.contains("CurrentLivingSituation")) return "CURRENT_LIVING_SITUATION";
        if (serviceName.contains("DateOfEngagement")) return "DATE_OF_ENGAGEMENT";
        if (serviceName.contains("BedNight")) return "BED_NIGHT";
        return "UNKNOWN";
    }
    
    private HmisDataSecurityService.DvDataSensitivityLevel determineDvSensitivityLevel(String operation) {
        if (operation.contains("SafetyAssessment")) {
            return HmisDataSecurityService.DvDataSensitivityLevel.SAFETY_ASSESSMENT;
        }
        if (operation.contains("create") || operation.contains("update") || operation.contains("correct")) {
            return HmisDataSecurityService.DvDataSensitivityLevel.DETAILED_HISTORY;
        }
        return HmisDataSecurityService.DvDataSensitivityLevel.BASIC_STATUS;
    }
    
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "ANONYMOUS";
    }
    
    private int extractTotalCountFromBulkResult(Object result) {
        // This would need to be implemented based on actual bulk response structure
        return 0; // Placeholder
    }
    
    private int extractSuccessCountFromBulkResult(Object result) {
        // This would need to be implemented based on actual bulk response structure
        return 0; // Placeholder
    }
}
