package org.haven.programenrollment.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit logging service for Intake PSDE operations
 * Provides comprehensive audit trails for VAWA compliance and security monitoring
 */
@Service
public class IntakePsdeAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(IntakePsdeAuditLogger.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("PSDE_AUDIT");

    /**
     * Log successful data access
     */
    public void logDataAccess(String recordId, String userId, String accessType) {
        Map<String, Object> auditEvent = createBaseAuditEvent("DATA_ACCESS", recordId, userId);
        auditEvent.put("accessType", accessType);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE data access: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log redaction applied to data
     */
    public void logRedactionApplied(String recordId, String userId, String redactionType) {
        Map<String, Object> auditEvent = createBaseAuditEvent("REDACTION_APPLIED", recordId, userId);
        auditEvent.put("redactionType", redactionType);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE redaction applied: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log unauthorized access attempt
     */
    public void logUnauthorizedAccess(String recordId, String userId, String attemptedAction, String reason) {
        Map<String, Object> auditEvent = createBaseAuditEvent("UNAUTHORIZED_ACCESS", recordId, userId);
        auditEvent.put("attemptedAction", attemptedAction);
        auditEvent.put("reason", reason);
        auditEvent.put("result", "BLOCKED");

        auditLogger.warn("PSDE unauthorized access blocked: {}", formatAuditEvent(auditEvent));

        // Also log to security logger for monitoring
        logger.warn("Unauthorized PSDE access attempt by user {} for record {} - {}",
            userId, recordId, reason);
    }

    /**
     * Log PSDE record creation
     */
    public void logRecordCreation(String recordId, String userId, String clientId, String enrollmentId) {
        Map<String, Object> auditEvent = createBaseAuditEvent("RECORD_CREATED", recordId, userId);
        auditEvent.put("clientId", clientId);
        auditEvent.put("enrollmentId", enrollmentId);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE record created: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log PSDE record update
     */
    public void logRecordUpdate(String recordId, String userId, String updateType, String[] modifiedFields) {
        Map<String, Object> auditEvent = createBaseAuditEvent("RECORD_UPDATED", recordId, userId);
        auditEvent.put("updateType", updateType);
        auditEvent.put("modifiedFields", String.join(",", modifiedFields));
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE record updated: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log DV data access specifically
     */
    public void logDvDataAccess(String recordId, String userId, String dvAccessLevel, String justification) {
        Map<String, Object> auditEvent = createBaseAuditEvent("DV_DATA_ACCESS", recordId, userId);
        auditEvent.put("dvAccessLevel", dvAccessLevel);
        auditEvent.put("justification", justification);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("DV data access: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log VAWA confidentiality flag changes
     */
    public void logVawaConfidentialityChange(String recordId, String userId, boolean oldValue, boolean newValue, String reason) {
        Map<String, Object> auditEvent = createBaseAuditEvent("VAWA_CONFIDENTIALITY_CHANGE", recordId, userId);
        auditEvent.put("oldValue", oldValue);
        auditEvent.put("newValue", newValue);
        auditEvent.put("reason", reason);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("VAWA confidentiality flag changed: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log data export events
     */
    public void logDataExport(String userId, String exportType, int recordCount, boolean includesDvData) {
        Map<String, Object> auditEvent = createBaseAuditEvent("DATA_EXPORT", null, userId);
        auditEvent.put("exportType", exportType);
        auditEvent.put("recordCount", recordCount);
        auditEvent.put("includesDvData", includesDvData);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE data export: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log validation failures
     */
    public void logValidationFailure(String recordId, String userId, String validationType, String errorDetails) {
        Map<String, Object> auditEvent = createBaseAuditEvent("VALIDATION_FAILURE", recordId, userId);
        auditEvent.put("validationType", validationType);
        auditEvent.put("errorDetails", errorDetails);
        auditEvent.put("result", "FAILURE");

        auditLogger.warn("PSDE validation failure: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log high-risk DV case detection
     */
    public void logHighRiskDvCaseDetected(String recordId, String userId, String riskIndicators) {
        Map<String, Object> auditEvent = createBaseAuditEvent("HIGH_RISK_DV_DETECTED", recordId, userId);
        auditEvent.put("riskIndicators", riskIndicators);
        auditEvent.put("result", "DETECTED");

        auditLogger.info("High-risk DV case detected: {}", formatAuditEvent(auditEvent));

        // Also log as warning for monitoring systems
        logger.warn("High-risk DV case detected for record {} with indicators: {}", recordId, riskIndicators);
    }

    /**
     * Log role-based access control decisions
     */
    public void logRoleBasedAccessDecision(String recordId, String userId, String userRoles, String accessDecision, String reason) {
        Map<String, Object> auditEvent = createBaseAuditEvent("RBAC_DECISION", recordId, userId);
        auditEvent.put("userRoles", userRoles);
        auditEvent.put("accessDecision", accessDecision);
        auditEvent.put("reason", reason);

        auditLogger.info("Role-based access decision: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log system-level events
     */
    public void logSystemEvent(String eventType, String userId, String details) {
        Map<String, Object> auditEvent = createBaseAuditEvent("SYSTEM_EVENT", null, userId);
        auditEvent.put("eventType", eventType);
        auditEvent.put("details", details);

        auditLogger.info("PSDE system event: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log data correction operation
     */
    public void logDataCorrection(String originalRecordId, String correctedBy, String correctionReason, String correctionType, String recordId) {
        Map<String, Object> auditEvent = createBaseAuditEvent("DATA_CORRECTION", recordId, correctedBy);
        auditEvent.put("originalRecordId", originalRecordId);
        auditEvent.put("correctionReason", correctionReason);
        auditEvent.put("correctionType", correctionType);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE data correction: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log backdated entry creation
     */
    public void logBackdatedEntry(String recordId, String createdBy, String backdatingReason, String effectiveDate, String enrollmentId) {
        Map<String, Object> auditEvent = createBaseAuditEvent("BACKDATED_ENTRY", recordId, createdBy);
        auditEvent.put("backdatingReason", backdatingReason);
        auditEvent.put("effectiveDate", effectiveDate);
        auditEvent.put("enrollmentId", enrollmentId);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE backdated entry: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log idempotent operation
     */
    public void logIdempotentOperation(String recordId, String userId, String idempotencyKey, String operationResult) {
        Map<String, Object> auditEvent = createBaseAuditEvent("IDEMPOTENT_OPERATION", recordId, userId);
        auditEvent.put("idempotencyKey", idempotencyKey);
        auditEvent.put("operationResult", operationResult);

        auditLogger.info("PSDE idempotent operation: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log audit trail access
     */
    public void logAuditTrailAccess(String recordId, String userId) {
        Map<String, Object> auditEvent = createBaseAuditEvent("AUDIT_TRAIL_ACCESS", recordId, userId);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE audit trail accessed: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Log historical data access
     */
    public void logHistoricalAccess(String enrollmentId, String userId, String timestamp) {
        Map<String, Object> auditEvent = createBaseAuditEvent("HISTORICAL_ACCESS", enrollmentId, userId);
        auditEvent.put("requestedTimestamp", timestamp);
        auditEvent.put("result", "SUCCESS");

        auditLogger.info("PSDE historical data accessed: {}", formatAuditEvent(auditEvent));
    }

    /**
     * Create base audit event with common fields
     */
    private Map<String, Object> createBaseAuditEvent(String action, String recordId, String userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("action", action);
        event.put("recordId", recordId != null ? recordId : "N/A");
        event.put("userId", userId != null ? userId : "SYSTEM");
        event.put("component", "INTAKE_PSDE");
        event.put("version", "1.0");
        return event;
    }

    /**
     * Format audit event as JSON-like string for logging
     */
    private String formatAuditEvent(Map<String, Object> event) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        event.forEach((key, value) -> {
            if (sb.length() > 1) sb.append(", ");
            sb.append(key).append("=").append(value);
        });
        sb.append("}");
        return sb.toString();
    }

    /**
     * Get audit logger for external components
     */
    public Logger getAuditLogger() {
        return auditLogger;
    }
}