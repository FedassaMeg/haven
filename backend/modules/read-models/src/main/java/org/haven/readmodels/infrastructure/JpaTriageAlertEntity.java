package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.TriageAlert;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "triage_alerts")
public class JpaTriageAlertEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "client_name")
    private String clientName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private TriageAlert.AlertType alertType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private TriageAlert.AlertSeverity severity;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
    
    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TriageAlert.AlertStatus status;
    
    @Column(name = "case_number")
    private String caseNumber;
    
    @Column(name = "assigned_worker_id")
    private UUID assignedWorkerId;
    
    @Column(name = "assigned_worker_name")
    private String assignedWorkerName;
    
    // Default constructor for JPA
    public JpaTriageAlertEntity() {}
    
    // Constructor from domain object
    public JpaTriageAlertEntity(TriageAlert alert) {
        this.id = alert.getId();
        this.clientId = alert.getClientId();
        this.clientName = alert.getClientName();
        this.alertType = alert.getAlertType();
        this.severity = alert.getSeverity();
        this.description = alert.getDescription();
        this.dueDate = alert.getDueDate();
        this.createdAt = alert.getCreatedAt();
        this.acknowledgedAt = alert.getAcknowledgedAt();
        this.acknowledgedBy = alert.getAcknowledgedBy();
        this.status = alert.getStatus();
        this.caseNumber = alert.getCaseNumber();
        this.assignedWorkerId = alert.getAssignedWorkerId();
        this.assignedWorkerName = alert.getAssignedWorkerName();
    }
    
    // Convert to domain object
    public TriageAlert toDomain() {
        TriageAlert alert = new TriageAlert();
        alert.setId(this.id);
        alert.setClientId(this.clientId);
        alert.setClientName(this.clientName);
        alert.setAlertType(this.alertType);
        alert.setSeverity(this.severity);
        alert.setDescription(this.description);
        alert.setDueDate(this.dueDate);
        alert.setCreatedAt(this.createdAt);
        alert.setAcknowledgedAt(this.acknowledgedAt);
        alert.setAcknowledgedBy(this.acknowledgedBy);
        alert.setStatus(this.status);
        alert.setCaseNumber(this.caseNumber);
        alert.setAssignedWorkerId(this.assignedWorkerId);
        alert.setAssignedWorkerName(this.assignedWorkerName);
        return alert;
    }
    
    // Update from domain object
    public void updateFrom(TriageAlert alert) {
        this.clientName = alert.getClientName();
        this.alertType = alert.getAlertType();
        this.severity = alert.getSeverity();
        this.description = alert.getDescription();
        this.dueDate = alert.getDueDate();
        this.acknowledgedAt = alert.getAcknowledgedAt();
        this.acknowledgedBy = alert.getAcknowledgedBy();
        this.status = alert.getStatus();
        this.caseNumber = alert.getCaseNumber();
        this.assignedWorkerId = alert.getAssignedWorkerId();
        this.assignedWorkerName = alert.getAssignedWorkerName();
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public TriageAlert.AlertType getAlertType() { return alertType; }
    public void setAlertType(TriageAlert.AlertType alertType) { this.alertType = alertType; }
    
    public TriageAlert.AlertSeverity getSeverity() { return severity; }
    public void setSeverity(TriageAlert.AlertSeverity severity) { this.severity = severity; }
    
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
    
    public TriageAlert.AlertStatus getStatus() { return status; }
    public void setStatus(TriageAlert.AlertStatus status) { this.status = status; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public UUID getAssignedWorkerId() { return assignedWorkerId; }
    public void setAssignedWorkerId(UUID assignedWorkerId) { this.assignedWorkerId = assignedWorkerId; }
    
    public String getAssignedWorkerName() { return assignedWorkerName; }
    public void setAssignedWorkerName(String assignedWorkerName) { this.assignedWorkerName = assignedWorkerName; }
}