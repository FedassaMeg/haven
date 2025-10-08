package org.haven.shared.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for audit log persistence
 * Supports append-only audit trail for compliance and security
 */
public interface AuditLogRepository {

    /**
     * Save an audit entry (append-only)
     * @param entry the audit entry to persist
     */
    void save(AuditService.AuditEntry entry);

    /**
     * Save an audit log entity (for privileged audit events)
     * @param entity the audit log entity to persist
     */
    void save(AuditLogEntity entity);

    /**
     * Find audit log by audit ID
     * @param auditId the audit event identifier
     * @return the audit log entity
     */
    AuditLogEntity findByAuditId(UUID auditId);

    /**
     * Find all audit logs for a user
     * @param userId the user identifier
     * @return list of audit log entities
     */
    List<AuditLogEntity> findByUserId(UUID userId);

    /**
     * Find audit logs for a resource
     * @param resourceId the resource identifier
     * @return list of audit log entities
     */
    List<AuditLogEntity> findByResourceId(UUID resourceId);

    /**
     * Find audit entries for a specific resource within a time range
     * @param resourceId the resource identifier
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit entries
     */
    List<AuditService.AuditEntry> findByResourceId(UUID resourceId, Instant startTime, Instant endTime);

    /**
     * Find audit entries for a specific user within a time range
     * @param userId the user identifier
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit entries
     */
    List<AuditService.AuditEntry> findByUserId(UUID userId, Instant startTime, Instant endTime);

    /**
     * Find audit entries by action type within a time range
     * @param action the action type
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit entries
     */
    List<AuditService.AuditEntry> findByAction(String action, Instant startTime, Instant endTime);

    /**
     * Find audit entries by resource type within a time range
     * @param resourceType the resource type
     * @param startTime start of time range
     * @param endTime end of time range
     * @return list of audit entries
     */
    List<AuditService.AuditEntry> findByResourceType(String resourceType, Instant startTime, Instant endTime);

    /**
     * Count audit entries for a user within a time range
     * @param userId the user identifier
     * @param startTime start of time range
     * @param endTime end of time range
     * @return count of audit entries
     */
    long countByUserId(UUID userId, Instant startTime, Instant endTime);

    /**
     * Count all audit entries for a user (for privileged audit)
     * @param userId the user identifier
     * @return count of audit entries
     */
    long countByUserId(UUID userId);

    /**
     * Find recent audit entries (for monitoring)
     * @param limit maximum number of entries to return
     * @return list of recent audit entries
     */
    List<AuditService.AuditEntry> findRecent(int limit);
}
