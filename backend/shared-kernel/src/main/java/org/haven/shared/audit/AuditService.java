package org.haven.shared.audit;

import org.haven.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging business events
 * Persists audit trail to database for compliance (VAWA, HUD) and security
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logDomainEvent(DomainEvent event, String userId, String action) {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            event.aggregateId(),
            event.eventType(),
            action,
            userId,
            Instant.now(),
            event.toString()
        );

        // Persist to database for compliance
        try {
            auditLogRepository.save(entry);
            logger.debug("Audit log saved: {}", entry);
        } catch (Exception e) {
            // Log error but don't fail the business operation
            logger.error("Failed to persist audit log: {}", entry, e);
            // Fall back to console logging for critical visibility
            System.err.println("AUDIT_PERSISTENCE_FAILED: " + entry);
        }
    }

    public void logBusinessAction(UUID resourceId, String resourceType,
                                 String action, String userId, String details) {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            resourceId,
            resourceType,
            action,
            userId,
            Instant.now(),
            details
        );

        // Persist to database for compliance
        try {
            auditLogRepository.save(entry);
            logger.debug("Audit log saved: {}", entry);
        } catch (Exception e) {
            // Log error but don't fail the business operation
            logger.error("Failed to persist audit log: {}", entry, e);
            // Fall back to console logging for critical visibility
            System.err.println("AUDIT_PERSISTENCE_FAILED: " + entry);
        }
    }

    public void logAction(String action, Map<String, Object> metadata) {
        // Extract common fields from metadata
        UUID resourceId = metadata.containsKey("id") ?
            UUID.fromString(metadata.get("id").toString()) : UUID.randomUUID();

        String details = metadata.toString();
        String userId = metadata.getOrDefault("userId", "system").toString();

        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            resourceId,
            action,
            action,
            userId,
            Instant.now(),
            details
        );

        // Persist to database for compliance
        try {
            auditLogRepository.save(entry);
            logger.debug("Audit log saved: {}", entry);
        } catch (Exception e) {
            // Log error but don't fail the business operation
            logger.error("Failed to persist audit log: {}", entry, e);
            // Fall back to console logging for critical visibility
            System.err.println("AUDIT_PERSISTENCE_FAILED: " + entry);
        }
    }

    /**
     * Log access control decision with full context
     */
    public void logAccess(UUID userId, String userName, String resourceType, UUID resourceId,
                         String action, String reason, Map<String, Object> metadata) {
        String details = String.format("User: %s, Resource: %s/%s, Action: %s, Reason: %s, Metadata: %s",
                userName, resourceType, resourceId, action, reason, metadata);

        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            resourceId,
            resourceType,
            action,
            userId.toString(),
            Instant.now(),
            details
        );

        // Persist to database for compliance
        try {
            auditLogRepository.save(entry);
            logger.debug("Access audit log saved: {}", entry);
        } catch (Exception e) {
            // Log error but don't fail the business operation
            logger.error("Failed to persist access audit log: {}", entry, e);
            // Fall back to console logging for critical visibility
            System.err.println("ACCESS_AUDIT_PERSISTENCE_FAILED: " + entry);
        }
    }

    /**
     * Log system event with eventType and details
     */
    public void logSystemEvent(String eventType, String details, Map<String, Object> metadata) {
        UUID resourceId = metadata.containsKey("exportJobId") ?
            UUID.fromString(metadata.get("exportJobId").toString()) : UUID.randomUUID();
        String userId = metadata.getOrDefault("userId", "system").toString();

        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            resourceId,
            eventType,
            eventType,
            userId,
            Instant.now(),
            details
        );

        // Persist to database for compliance
        try {
            auditLogRepository.save(entry);
            logger.debug("System event audit log saved: {}", entry);
        } catch (Exception e) {
            // Log error but don't fail the business operation
            logger.error("Failed to persist system event audit log: {}", entry, e);
            // Fall back to console logging for critical visibility
            System.err.println("SYSTEM_EVENT_AUDIT_PERSISTENCE_FAILED: " + entry);
        }
    }

    /**
     * Log data access event with full context
     * Used for tracking access to sensitive data (VAWA, HIPAA, etc.)
     */
    public void logDataAccess(UUID userId, String userName, String resourceType, UUID resourceId,
                             String action, String reason, Map<String, Object> metadata) {
        String details = String.format("User: %s, Resource: %s/%s, Action: %s, Reason: %s, Metadata: %s",
                userName, resourceType, resourceId, action, reason, metadata);

        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            resourceId,
            resourceType,
            action,
            userId.toString(),
            Instant.now(),
            details
        );

        // Persist to database for compliance
        try {
            auditLogRepository.save(entry);
            logger.debug("Data access audit log saved: {}", entry);
        } catch (Exception e) {
            // Log error but don't fail the business operation
            logger.error("Failed to persist data access audit log: {}", entry, e);
            // Fall back to console logging for critical visibility
            System.err.println("DATA_ACCESS_AUDIT_PERSISTENCE_FAILED: " + entry);
        }
    }

    /**
     * Public audit entry record for domain use
     */
    public record AuditEntry(
        UUID id,
        UUID resourceId,
        String resourceType,
        String action,
        String userId,
        Instant timestamp,
        String details
    ) {}
}