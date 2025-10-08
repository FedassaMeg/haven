package org.haven.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for logging privileged actions with enhanced audit trail.
 *
 * Features:
 * - Structured JSON logging for SIEM ingestion
 * - Both success AND failure event logging
 * - SIEM routing tags (pii_audit:*)
 * - MDC context for distributed tracing
 * - Database persistence for compliance
 * - Separation of concerns: structured logs for SIEM, database for queries
 *
 * Integration points:
 * - SLF4J/Logback for log output
 * - MDC for request correlation
 * - AuditLogRepository for database persistence
 * - SIEM sidecar/collector for security monitoring
 *
 * Usage:
 * <pre>
 * privilegedAuditService.logAction(
 *     PrivilegedAuditEvent.builder()
 *         .eventType(PrivilegedActionType.DV_NOTE_READ)
 *         .outcome(AuditOutcome.SUCCESS)
 *         .actorId(userId)
 *         .actorUsername(username)
 *         .actorRoles(roles)
 *         .resourceType("RestrictedNote")
 *         .resourceId(noteId)
 *         .justification("Case review")
 *         .build()
 * );
 * </pre>
 */
@Service
public class PrivilegedAuditService {

    private static final Logger logger = LoggerFactory.getLogger(PrivilegedAuditService.class);

    // Separate logger for SIEM routing (configured in logback.xml)
    private static final Logger siemLogger = LoggerFactory.getLogger("PRIVILEGED_AUDIT");

    private final AuditLogRepository auditLogRepository;

    public PrivilegedAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a privileged action with full audit context.
     *
     * Emits:
     * 1. Structured JSON log to SIEM logger
     * 2. Database audit record for compliance queries
     * 3. Standard application log for debugging
     *
     * @param event Privileged audit event
     */
    public void logAction(PrivilegedAuditEvent event) {
        try {
            // Set MDC context for correlation
            String previousRequestId = MDC.get("requestId");
            if (event.requestId() != null) {
                MDC.put("requestId", event.requestId());
            }
            MDC.put("auditEventId", event.eventId().toString());
            MDC.put("siemTag", event.getSiemTag());

            try {
                // 1. Emit structured JSON log for SIEM ingestion
                logToSiem(event);

                // 2. Persist to database for compliance queries
                persistToDatabase(event);

                // 3. Standard application log
                logToApplication(event);

            } finally {
                // Cleanup MDC
                MDC.remove("auditEventId");
                MDC.remove("siemTag");
                if (previousRequestId != null) {
                    MDC.put("requestId", previousRequestId);
                } else {
                    MDC.remove("requestId");
                }
            }

        } catch (Exception e) {
            // CRITICAL: Audit logging failures must not fail business operations
            // but must be visible for compliance monitoring
            logger.error("AUDIT_LOGGING_FAILURE: Failed to log privileged action - event will be lost!", e);
            System.err.println("CRITICAL_AUDIT_FAILURE: " + event.toJson());
        }
    }

    /**
     * Log success action (convenience method)
     */
    public void logSuccess(PrivilegedAuditEvent.Builder eventBuilder) {
        logAction(eventBuilder.outcome(AuditOutcome.SUCCESS).build());
    }

    /**
     * Log denied action (convenience method)
     */
    public void logDenial(PrivilegedAuditEvent.Builder eventBuilder,
                         AuditOutcome denialOutcome,
                         String denialReason,
                         String denialDetails) {
        logAction(eventBuilder
            .outcome(denialOutcome)
            .denialReason(denialReason)
            .denialDetails(denialDetails)
            .build());
    }

    /**
     * Emit structured JSON log for SIEM ingestion.
     *
     * Log format:
     * - JSON structured data
     * - Tagged with SIEM routing key (pii_audit:*)
     * - Includes severity for alerting
     * - No sensitive payload data (PII scrubbed)
     */
    private void logToSiem(PrivilegedAuditEvent event) {
        String logLevel = determineLogLevel(event);
        String siemTag = event.getSiemTag();
        String jsonPayload = event.toJson();

        // Use appropriate log level based on outcome and severity
        switch (logLevel) {
            case "ERROR" -> siemLogger.error("PRIVILEGED_ACTION [{}] {}", siemTag, jsonPayload);
            case "WARN" -> siemLogger.warn("PRIVILEGED_ACTION [{}] {}", siemTag, jsonPayload);
            default -> siemLogger.info("PRIVILEGED_ACTION [{}] {}", siemTag, jsonPayload);
        }
    }

    /**
     * Persist audit event to database for compliance queries.
     *
     * Database records support:
     * - Compliance officer queries
     * - Incident investigation
     * - SOX audit trail (7 year retention)
     */
    private void persistToDatabase(PrivilegedAuditEvent event) {
        try {
            AuditLogEntity entity = new AuditLogEntity(
                event.eventId(),
                event.resourceId(),
                event.resourceType(),
                event.eventType().name(),
                event.actorId(),
                event.timestamp(),
                buildDetailsString(event),
                event.ipAddress(),
                event.sessionId(),
                "PRIVILEGED_AUDIT",
                event.getSeverity(),
                event.outcome().name()
            );

            auditLogRepository.save(entity);
            logger.debug("Privileged audit event persisted to database: {}", event.eventId());

        } catch (Exception e) {
            // Log persistence failure but don't fail the audit
            logger.error("Failed to persist privileged audit event to database: {}", event.eventId(), e);
            // Ensure event is still visible via console
            System.err.println("AUDIT_DB_PERSISTENCE_FAILED: " + event.toJson());
        }
    }

    /**
     * Log to standard application logger for debugging
     */
    private void logToApplication(PrivilegedAuditEvent event) {
        String message = String.format(
            "Privileged Action: %s by %s (%s) on %s/%s - Outcome: %s",
            event.eventType(),
            event.actorUsername(),
            event.actorId(),
            event.resourceType(),
            event.resourceId(),
            event.outcome()
        );

        if (event.outcome().isSuccess()) {
            logger.info(message);
        } else if (event.outcome().isDenial()) {
            logger.warn("{} - Reason: {}", message, event.denialReason());
        } else {
            logger.error("{} - Error: {}", message, event.denialDetails());
        }
    }

    /**
     * Build human-readable details string for database storage
     */
    private String buildDetailsString(PrivilegedAuditEvent event) {
        StringBuilder details = new StringBuilder();
        details.append("Event: ").append(event.eventType().getDescription()).append("\n");
        details.append("Actor: ").append(event.actorUsername()).append(" (").append(event.actorId()).append(")\n");
        details.append("Roles: ").append(String.join(", ", event.actorRoles())).append("\n");
        details.append("Resource: ").append(event.resourceType());
        if (event.resourceId() != null) {
            details.append("/").append(event.resourceId());
        }
        if (event.resourceDescription() != null) {
            details.append(" - ").append(event.resourceDescription());
        }
        details.append("\n");
        details.append("Outcome: ").append(event.outcome().getDescription()).append("\n");

        if (event.justification() != null) {
            details.append("Justification: ").append(event.justification()).append("\n");
        }
        if (event.consentLedgerId() != null) {
            details.append("Consent Ledger ID: ").append(event.consentLedgerId()).append("\n");
        }
        if (event.hashFingerprint() != null) {
            details.append("Hash Fingerprint: ").append(event.hashFingerprint()).append("\n");
        }
        if (event.denialReason() != null) {
            details.append("Denial Reason: ").append(event.denialReason()).append("\n");
        }
        if (event.denialDetails() != null) {
            details.append("Denial Details: ").append(event.denialDetails()).append("\n");
        }
        if (event.requestId() != null) {
            details.append("Request ID: ").append(event.requestId()).append("\n");
        }

        return details.toString();
    }

    /**
     * Determine log level based on outcome and severity
     */
    private String determineLogLevel(PrivilegedAuditEvent event) {
        // Errors always logged as ERROR
        if (event.outcome().isError()) {
            return "ERROR";
        }

        // Denials logged as WARN for alerting
        if (event.outcome().isDenial()) {
            return "WARN";
        }

        // Critical actions logged as WARN even on success
        if ("CRITICAL".equals(event.getSeverity())) {
            return "WARN";
        }

        // Everything else as INFO
        return "INFO";
    }

    /**
     * Query audit events for security investigation
     */
    public void queryAuditTrail(UUID resourceId, Instant startTime, Instant endTime) {
        // This would typically call a repository method
        logger.info("Querying audit trail for resource {} between {} and {}",
            resourceId, startTime, endTime);

        // Emit audit event for the query itself (accessing audit logs is privileged)
        logAction(PrivilegedAuditEvent.builder()
            .eventType(PrivilegedActionType.AUDIT_LOG_ACCESSED)
            .outcome(AuditOutcome.SUCCESS)
            .actorId(getCurrentUserId())
            .actorUsername(getCurrentUsername())
            .actorRoles(getCurrentUserRoles())
            .resourceType("AuditLog")
            .resourceId(resourceId)
            .resourceDescription("Audit trail query")
            .addMetadata("startTime", startTime.toString())
            .addMetadata("endTime", endTime.toString())
            .build());
    }

    // Helper methods to get current user context
    // In production, these would extract from SecurityContext or JWT

    private UUID getCurrentUserId() {
        // TODO: Extract from SecurityContext
        return UUID.randomUUID();
    }

    private String getCurrentUsername() {
        // TODO: Extract from SecurityContext
        return "system";
    }

    private java.util.List<String> getCurrentUserRoles() {
        // TODO: Extract from SecurityContext
        return java.util.List.of("SYSTEM");
    }
}
