package org.haven.shared.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entity for compliance tracking
 * Append-only table - no updates or deletes allowed
 * Used for VAWA, HUD reporting, and incident response
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_resource_id", columnList = "resource_id"),
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_resource_type", columnList = "resource_type"),
    @Index(name = "idx_audit_user_action", columnList = "user_id,action"),
    @Index(name = "idx_audit_resource_timestamp", columnList = "resource_id,timestamp")
})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false, unique = true, updatable = false)
    private UUID auditId;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Column(name = "resource_type", length = 100, updatable = false)
    private String resourceType;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "details", columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "session_id", length = 255, updatable = false)
    private String sessionId;

    @Column(name = "component", length = 100, updatable = false)
    private String component;

    @Column(name = "severity", length = 20, updatable = false)
    private String severity;

    @Column(name = "result", length = 50, updatable = false)
    private String result;

    protected AuditLogEntity() {
        // For JPA
    }

    public AuditLogEntity(UUID auditId, UUID resourceId, String resourceType, String action,
                         UUID userId, Instant timestamp, String details, String ipAddress,
                         String sessionId, String component, String severity, String result) {
        this.auditId = auditId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.action = action;
        this.userId = userId;
        this.timestamp = timestamp;
        this.details = details;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.component = component;
        this.severity = severity;
        this.result = result;
    }

    // Factory method to create from AuditEntry
    public static AuditLogEntity fromAuditEntry(AuditService.AuditEntry entry) {
        UUID userId;
        try {
            userId = UUID.fromString(entry.userId());
        } catch (IllegalArgumentException e) {
            // If userId is not a valid UUID (e.g., "system"), generate a placeholder
            userId = UUID.nameUUIDFromBytes(entry.userId().getBytes());
        }

        return new AuditLogEntity(
            entry.id(),
            entry.resourceId(),
            entry.resourceType(),
            entry.action(),
            userId,
            entry.timestamp(),
            entry.details(),
            null, // IP address not in current AuditEntry
            null, // Session ID not in current AuditEntry
            "SYSTEM",
            "INFO",
            "SUCCESS"
        );
    }

    // Getters only - immutable audit record
    public Long getId() { return id; }
    public UUID getAuditId() { return auditId; }
    public UUID getResourceId() { return resourceId; }
    public String getResourceType() { return resourceType; }
    public String getAction() { return action; }
    public UUID getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public String getSessionId() { return sessionId; }
    public String getComponent() { return component; }
    public String getSeverity() { return severity; }
    public String getResult() { return result; }
}
