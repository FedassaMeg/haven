package org.haven.shared.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for Keycloak role synchronization operations.
 */
@Entity
@Table(name = "rbac_sync_log", schema = "haven")
public class RbacSyncLog {

    @Id
    private UUID id;

    @Column(name = "sync_timestamp", nullable = false)
    private Instant syncTimestamp;

    @Column(name = "sync_type", nullable = false, length = 50)
    private String syncType;

    @Column(name = "roles_added")
    private Integer rolesAdded = 0;

    @Column(name = "roles_updated")
    private Integer rolesUpdated = 0;

    @Column(name = "roles_removed")
    private Integer rolesRemoved = 0;

    @Column(name = "drift_detected")
    private Boolean driftDetected = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drift_details", columnDefinition = "jsonb")
    private JsonNode driftDetails;

    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "performed_by")
    private UUID performedBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (syncTimestamp == null) {
            syncTimestamp = Instant.now();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getSyncTimestamp() {
        return syncTimestamp;
    }

    public void setSyncTimestamp(Instant syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
    }

    public String getSyncType() {
        return syncType;
    }

    public void setSyncType(String syncType) {
        this.syncType = syncType;
    }

    public Integer getRolesAdded() {
        return rolesAdded;
    }

    public void setRolesAdded(Integer rolesAdded) {
        this.rolesAdded = rolesAdded;
    }

    public Integer getRolesUpdated() {
        return rolesUpdated;
    }

    public void setRolesUpdated(Integer rolesUpdated) {
        this.rolesUpdated = rolesUpdated;
    }

    public Integer getRolesRemoved() {
        return rolesRemoved;
    }

    public void setRolesRemoved(Integer rolesRemoved) {
        this.rolesRemoved = rolesRemoved;
    }

    public Boolean getDriftDetected() {
        return driftDetected;
    }

    public void setDriftDetected(Boolean driftDetected) {
        this.driftDetected = driftDetected;
    }

    public JsonNode getDriftDetails() {
        return driftDetails;
    }

    public void setDriftDetails(JsonNode driftDetails) {
        this.driftDetails = driftDetails;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(UUID performedBy) {
        this.performedBy = performedBy;
    }
}
