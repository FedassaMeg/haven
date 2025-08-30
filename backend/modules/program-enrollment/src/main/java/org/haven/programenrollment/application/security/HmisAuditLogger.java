package org.haven.programenrollment.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Comprehensive audit logging service for HMIS data access
 * Provides detailed audit trails for compliance with HMIS privacy and security requirements
 * Supports forensic analysis and compliance reporting
 */
@Service
public class HmisAuditLogger {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("HMIS_AUDIT");
    private static final Logger securityLogger = LoggerFactory.getLogger("HMIS_SECURITY");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Log successful data access
     */
    public void logDataAccess(String dataType, UUID resourceId, String operation, String username) {
        logDataAccess(dataType, resourceId, operation, username, null);
    }
    
    /**
     * Log successful data access with additional context
     */
    public void logDataAccess(String dataType, UUID resourceId, String operation, String username, String additionalContext) {
        try {
            MDC.put("event_type", "DATA_ACCESS");
            MDC.put("data_type", dataType);
            MDC.put("resource_id", resourceId != null ? resourceId.toString() : "NULL");
            MDC.put("operation", operation);
            MDC.put("username", username);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "SUCCESS");
            
            if (additionalContext != null) {
                MDC.put("context", additionalContext);
            }
            
            String logMessage = String.format("Data access: %s operation on %s resource %s by user %s", 
                operation, dataType, resourceId, username);
            
            if (additionalContext != null) {
                logMessage += " [" + additionalContext + "]";
            }
            
            auditLogger.info(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log unauthorized access attempts
     */
    public void logUnauthorizedAccess(String dataType, UUID resourceId, String operation, String reason) {
        try {
            MDC.put("event_type", "UNAUTHORIZED_ACCESS");
            MDC.put("data_type", dataType);
            MDC.put("resource_id", resourceId != null ? resourceId.toString() : "NULL");
            MDC.put("operation", operation);
            MDC.put("reason", reason);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("severity", "HIGH");
            MDC.put("status", "BLOCKED");
            
            String logMessage = String.format("SECURITY ALERT: Unauthorized access attempt to %s resource %s for %s operation - %s", 
                dataType, resourceId, operation, reason);
            
            securityLogger.warn(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log data modifications (create, update, delete)
     */
    public void logDataModification(String dataType, UUID resourceId, String operation, String username, 
                                   Object oldValue, Object newValue) {
        try {
            MDC.put("event_type", "DATA_MODIFICATION");
            MDC.put("data_type", dataType);
            MDC.put("resource_id", resourceId != null ? resourceId.toString() : "NULL");
            MDC.put("operation", operation);
            MDC.put("username", username);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "SUCCESS");
            
            if (oldValue != null) {
                MDC.put("old_value", sanitizeValue(oldValue.toString()));
            }
            if (newValue != null) {
                MDC.put("new_value", sanitizeValue(newValue.toString()));
            }
            
            String logMessage = String.format("Data modification: %s operation on %s resource %s by user %s", 
                operation, dataType, resourceId, username);
            
            auditLogger.info(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log data corrections with enhanced detail
     */
    public void logDataCorrection(String dataType, UUID originalRecordId, UUID correctionRecordId, 
                                 String username, String correctionReason, Object originalValue, Object correctedValue) {
        try {
            MDC.put("event_type", "DATA_CORRECTION");
            MDC.put("data_type", dataType);
            MDC.put("original_record_id", originalRecordId != null ? originalRecordId.toString() : "NULL");
            MDC.put("correction_record_id", correctionRecordId != null ? correctionRecordId.toString() : "NULL");
            MDC.put("username", username);
            MDC.put("correction_reason", correctionReason);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "SUCCESS");
            MDC.put("severity", "MEDIUM"); // Corrections are important for audit trail
            
            if (originalValue != null) {
                MDC.put("original_value", sanitizeValue(originalValue.toString()));
            }
            if (correctedValue != null) {
                MDC.put("corrected_value", sanitizeValue(correctedValue.toString()));
            }
            
            String logMessage = String.format("Data correction: %s record %s corrected by user %s - Reason: %s", 
                dataType, originalRecordId, username, correctionReason);
            
            auditLogger.warn(logMessage); // Use warn level for corrections to highlight them
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log system-level operations
     */
    public void logSystemAccess(String operation, String username) {
        try {
            MDC.put("event_type", "SYSTEM_ACCESS");
            MDC.put("operation", operation);
            MDC.put("username", username);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "SUCCESS");
            
            String logMessage = String.format("System operation: %s by user %s", operation, username);
            
            auditLogger.info(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log report generation and access
     */
    public void logReportAccess(String reportType, LocalDateTime startDate, LocalDateTime endDate, String username) {
        try {
            MDC.put("event_type", "REPORT_ACCESS");
            MDC.put("report_type", reportType);
            MDC.put("username", username);
            MDC.put("report_start_date", startDate != null ? startDate.format(TIMESTAMP_FORMAT) : "NULL");
            MDC.put("report_end_date", endDate != null ? endDate.format(TIMESTAMP_FORMAT) : "NULL");
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "SUCCESS");
            
            String logMessage = String.format("Report access: %s report generated by user %s for period %s to %s", 
                reportType, username, 
                startDate != null ? startDate.format(TIMESTAMP_FORMAT) : "NULL",
                endDate != null ? endDate.format(TIMESTAMP_FORMAT) : "NULL");
            
            auditLogger.info(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log DV-specific high-risk events
     */
    public void logDvHighRiskEvent(String eventType, UUID clientId, UUID enrollmentId, String username, String details) {
        try {
            MDC.put("event_type", "DV_HIGH_RISK_EVENT");
            MDC.put("dv_event_type", eventType);
            MDC.put("client_id", clientId != null ? clientId.toString() : "NULL");
            MDC.put("enrollment_id", enrollmentId != null ? enrollmentId.toString() : "NULL");
            MDC.put("username", username);
            MDC.put("details", details);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("severity", "CRITICAL");
            MDC.put("status", "ALERT");
            
            String logMessage = String.format("DV HIGH RISK EVENT: %s for client %s (enrollment %s) by user %s - %s", 
                eventType, clientId, enrollmentId, username, details);
            
            securityLogger.error(logMessage); // Use error level for high-risk events
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log safety protocol activations
     */
    public void logSafetyProtocolActivation(String protocolType, UUID clientId, String activatedBy, String reason) {
        try {
            MDC.put("event_type", "SAFETY_PROTOCOL_ACTIVATION");
            MDC.put("protocol_type", protocolType);
            MDC.put("client_id", clientId != null ? clientId.toString() : "NULL");
            MDC.put("activated_by", activatedBy);
            MDC.put("reason", reason);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("severity", "HIGH");
            MDC.put("status", "ACTIVATED");
            
            String logMessage = String.format("Safety protocol activation: %s protocol activated for client %s by %s - Reason: %s", 
                protocolType, clientId, activatedBy, reason);
            
            auditLogger.warn(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log bulk operations with counts
     */
    public void logBulkOperation(String operationType, String dataType, int recordCount, int successCount, 
                                int failureCount, String username) {
        try {
            MDC.put("event_type", "BULK_OPERATION");
            MDC.put("operation_type", operationType);
            MDC.put("data_type", dataType);
            MDC.put("total_records", String.valueOf(recordCount));
            MDC.put("success_count", String.valueOf(successCount));
            MDC.put("failure_count", String.valueOf(failureCount));
            MDC.put("username", username);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", failureCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS");
            
            String logMessage = String.format("Bulk operation: %s %s by user %s - %d total, %d success, %d failures", 
                operationType, dataType, username, recordCount, successCount, failureCount);
            
            if (failureCount > 0) {
                auditLogger.warn(logMessage);
            } else {
                auditLogger.info(logMessage);
            }
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log validation failures
     */
    public void logValidationFailure(String dataType, UUID resourceId, String operation, String username, 
                                   java.util.List<String> validationErrors) {
        try {
            MDC.put("event_type", "VALIDATION_FAILURE");
            MDC.put("data_type", dataType);
            MDC.put("resource_id", resourceId != null ? resourceId.toString() : "NULL");
            MDC.put("operation", operation);
            MDC.put("username", username);
            MDC.put("validation_errors", String.join("; ", validationErrors));
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "VALIDATION_FAILED");
            
            String logMessage = String.format("Validation failure: %s operation on %s resource %s by user %s - Errors: %s", 
                operation, dataType, resourceId, username, String.join(", ", validationErrors));
            
            auditLogger.warn(logMessage);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log export operations (for HMIS compliance)
     */
    public void logDataExport(String exportType, LocalDateTime startDate, LocalDateTime endDate, 
                             int recordCount, String username, String destination) {
        try {
            MDC.put("event_type", "DATA_EXPORT");
            MDC.put("export_type", exportType);
            MDC.put("start_date", startDate != null ? startDate.format(TIMESTAMP_FORMAT) : "NULL");
            MDC.put("end_date", endDate != null ? endDate.format(TIMESTAMP_FORMAT) : "NULL");
            MDC.put("record_count", String.valueOf(recordCount));
            MDC.put("username", username);
            MDC.put("destination", destination);
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            MDC.put("status", "SUCCESS");
            MDC.put("severity", "MEDIUM"); // Exports are important for audit trail
            
            String logMessage = String.format("Data export: %s export of %d records by user %s to %s for period %s to %s", 
                exportType, recordCount, username, destination,
                startDate != null ? startDate.format(TIMESTAMP_FORMAT) : "NULL",
                endDate != null ? endDate.format(TIMESTAMP_FORMAT) : "NULL");
            
            auditLogger.warn(logMessage); // Use warn level for exports to highlight them
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Sanitize sensitive values for logging (remove or mask PII)
     */
    private String sanitizeValue(String value) {
        if (value == null) return "NULL";
        
        // For audit purposes, we generally don't log actual sensitive values
        // Instead log metadata about the change
        return "[SANITIZED:" + value.length() + " chars]";
    }
}