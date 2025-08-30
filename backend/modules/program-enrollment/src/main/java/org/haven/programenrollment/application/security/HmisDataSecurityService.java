package org.haven.programenrollment.application.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Security service for HMIS data access control
 * Implements role-based access control with enhanced privacy protections
 * for sensitive data elements like DV, disabilities, and client information
 */
@Service
public class HmisDataSecurityService {
    
    private final HmisAuditLogger auditLogger;
    
    public HmisDataSecurityService(HmisAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }
    
    /**
     * Check if user can access disability data
     */
    public boolean canAccessDisabilityData(UUID enrollmentId, String operation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            auditLogger.logUnauthorizedAccess("DISABILITY", enrollmentId, operation, "Not authenticated");
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Basic disability data access
        boolean hasAccess = userRoles.contains("CASE_MANAGER") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("DATA_ENTRY_SPECIALIST") ||
                           userRoles.contains("PROGRAM_COORDINATOR");
        
        if (hasAccess) {
            auditLogger.logDataAccess("DISABILITY", enrollmentId, operation, auth.getName());
        } else {
            auditLogger.logUnauthorizedAccess("DISABILITY", enrollmentId, operation, 
                "Insufficient role permissions: " + String.join(", ", userRoles));
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can access domestic violence data (enhanced security)
     */
    public boolean canAccessDvData(UUID enrollmentId, String operation, DvDataSensitivityLevel sensitivityLevel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            auditLogger.logUnauthorizedAccess("DOMESTIC_VIOLENCE", enrollmentId, operation, "Not authenticated");
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        boolean hasAccess = false;
        
        switch (sensitivityLevel) {
            case BASIC_STATUS -> {
                // Basic DV status (has history yes/no) - broader access
                hasAccess = userRoles.contains("CASE_MANAGER") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("DV_SPECIALIST") ||
                           userRoles.contains("PROGRAM_COORDINATOR");
            }
            case DETAILED_HISTORY -> {
                // Detailed DV information - restricted access
                hasAccess = userRoles.contains("DV_SPECIALIST") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("SAFETY_COORDINATOR");
            }
            case SAFETY_ASSESSMENT -> {
                // Safety assessment and high-risk information - most restricted
                hasAccess = userRoles.contains("DV_SPECIALIST") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("SAFETY_COORDINATOR");
                
                // Additional validation for safety assessment access
                if (hasAccess && sensitivityLevel == DvDataSensitivityLevel.SAFETY_ASSESSMENT) {
                    hasAccess = validateSafetyAssessmentAccess(auth, enrollmentId);
                }
            }
        }
        
        if (hasAccess) {
            auditLogger.logDataAccess("DOMESTIC_VIOLENCE", enrollmentId, operation, auth.getName(), 
                sensitivityLevel.toString());
        } else {
            auditLogger.logUnauthorizedAccess("DOMESTIC_VIOLENCE", enrollmentId, operation, 
                "Insufficient permissions for sensitivity level: " + sensitivityLevel);
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can access current living situation data
     */
    public boolean canAccessCurrentLivingSituationData(UUID enrollmentId, String operation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Street outreach and CLS data access
        boolean hasAccess = userRoles.contains("CASE_MANAGER") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("OUTREACH_WORKER") ||
                           userRoles.contains("PROGRAM_COORDINATOR") ||
                           userRoles.contains("DATA_ENTRY_SPECIALIST");
        
        if (hasAccess) {
            auditLogger.logDataAccess("CURRENT_LIVING_SITUATION", enrollmentId, operation, auth.getName());
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can access date of engagement data
     */
    public boolean canAccessDateOfEngagementData(UUID enrollmentId, String operation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Engagement data access
        boolean hasAccess = userRoles.contains("CASE_MANAGER") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("PROGRAM_COORDINATOR") ||
                           userRoles.contains("SERVICE_PROVIDER");
        
        if (hasAccess) {
            auditLogger.logDataAccess("DATE_OF_ENGAGEMENT", enrollmentId, operation, auth.getName());
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can access bed night data
     */
    public boolean canAccessBedNightData(UUID enrollmentId, String operation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Bed night tracking access
        boolean hasAccess = userRoles.contains("CASE_MANAGER") || 
                           userRoles.contains("ADMIN") ||
                           userRoles.contains("SHELTER_STAFF") ||
                           userRoles.contains("PROGRAM_COORDINATOR") ||
                           userRoles.contains("DATA_ENTRY_SPECIALIST");
        
        if (hasAccess) {
            auditLogger.logDataAccess("BED_NIGHT", enrollmentId, operation, auth.getName());
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can perform corrections on HMIS data
     */
    public boolean canPerformDataCorrections(String dataType, UUID recordId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Data correction permissions - more restrictive
        boolean hasAccess = userRoles.contains("ADMIN") ||
                           userRoles.contains("DATA_QUALITY_MANAGER") ||
                           userRoles.contains("PROGRAM_COORDINATOR");
        
        // Special case for DV corrections - only DV specialists and admins
        if ("DOMESTIC_VIOLENCE".equals(dataType)) {
            hasAccess = userRoles.contains("ADMIN") || userRoles.contains("DV_SPECIALIST");
        }
        
        if (hasAccess) {
            auditLogger.logDataAccess(dataType + "_CORRECTION", recordId, "CORRECT", auth.getName());
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can access bulk operations
     */
    public boolean canPerformBulkOperations(String operationType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Bulk operations - restricted to admin and data managers
        boolean hasAccess = userRoles.contains("ADMIN") ||
                           userRoles.contains("DATA_MANAGER") ||
                           userRoles.contains("SYSTEM_ADMINISTRATOR");
        
        if (hasAccess) {
            auditLogger.logSystemAccess("BULK_" + operationType, auth.getName());
        }
        
        return hasAccess;
    }
    
    /**
     * Check if user can access HMIS reporting data
     */
    public boolean canAccessReportingData(String reportType, LocalDateTime startDate, LocalDateTime endDate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Set<String> userRoles = extractRoles(auth);
        
        // Reporting access
        boolean hasAccess = userRoles.contains("ADMIN") ||
                           userRoles.contains("PROGRAM_COORDINATOR") ||
                           userRoles.contains("DATA_ANALYST") ||
                           userRoles.contains("REPORTING_MANAGER");
        
        // Enhanced restrictions for sensitive reports
        if (reportType.contains("DV") || reportType.contains("DOMESTIC_VIOLENCE")) {
            hasAccess = userRoles.contains("ADMIN") || 
                       userRoles.contains("DV_SPECIALIST") ||
                       userRoles.contains("SAFETY_COORDINATOR");
        }
        
        if (hasAccess) {
            auditLogger.logReportAccess(reportType, startDate, endDate, auth.getName());
        }
        
        return hasAccess;
    }
    
    /**
     * Validate time-based access restrictions
     */
    public boolean validateTimeBasedAccess(String operation, UUID resourceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Business hours: 6 AM to 10 PM
        boolean isBusinessHours = hour >= 6 && hour <= 22;
        
        Set<String> userRoles = extractRoles(auth);
        
        // Admins and emergency personnel can access 24/7
        boolean hasUnrestrictedAccess = userRoles.contains("ADMIN") ||
                                       userRoles.contains("EMERGENCY_CONTACT") ||
                                       userRoles.contains("ON_CALL_COORDINATOR");
        
        if (!isBusinessHours && !hasUnrestrictedAccess) {
            auditLogger.logUnauthorizedAccess("TIME_RESTRICTED", resourceId, operation, 
                "Access attempted outside business hours without emergency authorization");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate location-based access (for highly sensitive operations)
     */
    public boolean validateLocationBasedAccess(String operation, String clientIpAddress) {
        // Placeholder for IP-based access control
        // In production, this would validate against approved IP ranges/VPN
        
        // For high-risk DV operations, could require VPN or office network access
        if (operation.contains("DV_SAFETY_ASSESSMENT")) {
            // Validate IP is from approved secure network
            return isSecureNetworkIp(clientIpAddress);
        }
        
        return true;
    }
    
    private boolean validateSafetyAssessmentAccess(Authentication auth, UUID enrollmentId) {
        // Additional validation for safety assessment access
        // Could include recent training verification, supervisor approval, etc.
        
        Set<String> userRoles = extractRoles(auth);
        
        // Require specific DV training certification
        if (!userRoles.contains("DV_CERTIFIED")) {
            auditLogger.logUnauthorizedAccess("DOMESTIC_VIOLENCE", enrollmentId, "SAFETY_ASSESSMENT", 
                "Missing required DV certification");
            return false;
        }
        
        return true;
    }
    
    private Set<String> extractRoles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(role -> role.startsWith("ROLE_"))
            .map(role -> role.substring(5)) // Remove "ROLE_" prefix
            .collect(java.util.stream.Collectors.toSet());
    }
    
    private boolean isSecureNetworkIp(String ipAddress) {
        // Placeholder - would validate against approved IP ranges
        // For demo purposes, assume validation passes
        return true;
    }
    
    public enum DvDataSensitivityLevel {
        BASIC_STATUS,       // Has DV history yes/no
        DETAILED_HISTORY,   // When experienced, specifics
        SAFETY_ASSESSMENT   // Risk level, safety planning, high-risk indicators
    }
}