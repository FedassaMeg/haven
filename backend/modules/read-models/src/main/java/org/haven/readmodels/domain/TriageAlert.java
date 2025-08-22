package org.haven.readmodels.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class TriageAlert {
    private UUID id;
    private UUID clientId;
    private String clientName;
    private AlertType alertType;
    private AlertSeverity severity;
    private String description;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant acknowledgedAt;
    private UUID acknowledgedBy;
    private AlertStatus status;
    private String caseNumber;
    private UUID assignedWorkerId;
    private String assignedWorkerName;
    
    public enum AlertType {
        HIGH_RISK_CLIENT,
        COURT_DATE,
        ROI_EXPIRING,
        LEASE_RENEWAL,
        RECERTIFICATION_DUE,
        PAYMENT_PENDING,
        SAFETY_CHECK_NEEDED,
        PROTECTION_ORDER_EXPIRING,
        CONSENT_EXPIRING,
        DOCUMENTATION_MISSING
    }
    
    public enum AlertSeverity {
        CRITICAL,  // Immediate action required
        HIGH,      // Action required within 24 hours
        MEDIUM,    // Action required within 3 days
        LOW        // Action required within 7 days
    }
    
    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        IN_PROGRESS,
        RESOLVED,
        EXPIRED
    }
    
    // Constructor
    public TriageAlert() {}
    
    public TriageAlert(UUID clientId, String clientName, AlertType alertType, 
                      AlertSeverity severity, String description, LocalDate dueDate) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.clientName = clientName;
        this.alertType = alertType;
        this.severity = severity;
        this.description = description;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
        this.status = AlertStatus.ACTIVE;
    }
    
    // Methods
    public void acknowledge(UUID userId) {
        this.acknowledgedAt = Instant.now();
        this.acknowledgedBy = userId;
        this.status = AlertStatus.ACKNOWLEDGED;
    }
    
    public void markInProgress() {
        this.status = AlertStatus.IN_PROGRESS;
    }
    
    public void resolve() {
        this.status = AlertStatus.RESOLVED;
    }
    
    public boolean isOverdue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate) 
               && status != AlertStatus.RESOLVED;
    }
    
    public int getDaysUntilDue() {
        if (dueDate == null) return Integer.MAX_VALUE;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    
    public UUID getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(UUID acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public UUID getAssignedWorkerId() { return assignedWorkerId; }
    public void setAssignedWorkerId(UUID assignedWorkerId) { this.assignedWorkerId = assignedWorkerId; }
    
    public String getAssignedWorkerName() { return assignedWorkerName; }
    public void setAssignedWorkerName(String assignedWorkerName) { this.assignedWorkerName = assignedWorkerName; }
}