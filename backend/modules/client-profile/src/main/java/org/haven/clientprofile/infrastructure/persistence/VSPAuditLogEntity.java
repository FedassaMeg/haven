package org.haven.clientprofile.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log for VSP (Victim Service Provider) data access
 * Critical for VAWA compliance and DV victim protection
 * Append-only table - no updates or deletes allowed
 */
@Entity
@Table(name = "vsp_access_audit_log", indexes = {
    @Index(name = "idx_vsp_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_vsp_audit_client_id", columnList = "client_id"),
    @Index(name = "idx_vsp_audit_access_time", columnList = "access_time"),
    @Index(name = "idx_vsp_audit_access_granted", columnList = "access_granted"),
    @Index(name = "idx_vsp_audit_data_type", columnList = "data_type"),
    @Index(name = "idx_vsp_audit_user_client", columnList = "user_id,client_id")
})
public class VSPAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id", nullable = false, unique = true, updatable = false)
    private UUID auditId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "data_type", length = 100, updatable = false)
    private String dataType;

    @Column(name = "access_granted", nullable = false, updatable = false)
    private boolean accessGranted;

    @Column(name = "reason", columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "access_time", nullable = false, updatable = false)
    private Instant accessTime;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "session_id", length = 255, updatable = false)
    private String sessionId;

    @Column(name = "redaction_applied", updatable = false)
    private Boolean redactionApplied;

    @Column(name = "vawa_protected", updatable = false)
    private Boolean vawaProtected;

    protected VSPAuditLogEntity() {
        // For JPA
    }

    public VSPAuditLogEntity(UUID auditId, UUID userId, UUID clientId, String dataType,
                            boolean accessGranted, String reason, Instant accessTime,
                            String ipAddress, String sessionId, Boolean redactionApplied,
                            Boolean vawaProtected) {
        this.auditId = auditId;
        this.userId = userId;
        this.clientId = clientId;
        this.dataType = dataType;
        this.accessGranted = accessGranted;
        this.reason = reason;
        this.accessTime = accessTime;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.redactionApplied = redactionApplied;
        this.vawaProtected = vawaProtected;
    }

    // Getters only - immutable audit record
    public Long getId() { return id; }
    public UUID getAuditId() { return auditId; }
    public UUID getUserId() { return userId; }
    public UUID getClientId() { return clientId; }
    public String getDataType() { return dataType; }
    public boolean isAccessGranted() { return accessGranted; }
    public String getReason() { return reason; }
    public Instant getAccessTime() { return accessTime; }
    public String getIpAddress() { return ipAddress; }
    public String getSessionId() { return sessionId; }
    public Boolean getRedactionApplied() { return redactionApplied; }
    public Boolean getVawaProtected() { return vawaProtected; }
}
