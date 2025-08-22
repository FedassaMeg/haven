package org.haven.api.triage;

import org.haven.readmodels.domain.TriageAlert;
import org.haven.readmodels.infrastructure.TriageAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/triage")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class TriageController {
    
    private final TriageAlertRepository alertRepository;
    
    public TriageController(TriageAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }
    
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'CASE_MANAGER')")
    public ResponseEntity<TriageDashboardDto> getTriageDashboard(
            @RequestParam(required = false) String workerId,
            @RequestParam(defaultValue = "7") int daysAhead) {
        
        LocalDate cutoffDate = LocalDate.now().plusDays(daysAhead);
        
        // Get all active alerts
        List<TriageAlert> activeAlerts = alertRepository.findByStatusOrderBySeverityAscDueDateAsc(
            TriageAlert.AlertStatus.ACTIVE
        );
        
        // Filter by worker if specified
        if (workerId != null) {
            UUID workerUuid = UUID.fromString(workerId);
            activeAlerts = activeAlerts.stream()
                .filter(a -> workerUuid.equals(a.getAssignedWorkerId()))
                .collect(Collectors.toList());
        }
        
        // Get overdue alerts
        List<TriageAlert> overdueAlerts = alertRepository.findOverdueAlerts();
        
        // Count by severity
        Map<TriageAlert.AlertSeverity, Long> severityCounts = activeAlerts.stream()
            .collect(Collectors.groupingBy(
                TriageAlert::getSeverity,
                Collectors.counting()
            ));
        
        // Filter upcoming alerts (within specified days)
        List<TriageAlert> upcomingAlerts = activeAlerts.stream()
            .filter(a -> a.getDueDate() != null && 
                        !a.getDueDate().isAfter(cutoffDate) &&
                        !a.getDueDate().isBefore(LocalDate.now()))
            .collect(Collectors.toList());
        
        TriageDashboardDto dashboard = new TriageDashboardDto();
        dashboard.setCriticalCount(severityCounts.getOrDefault(TriageAlert.AlertSeverity.CRITICAL, 0L));
        dashboard.setHighCount(severityCounts.getOrDefault(TriageAlert.AlertSeverity.HIGH, 0L));
        dashboard.setMediumCount(severityCounts.getOrDefault(TriageAlert.AlertSeverity.MEDIUM, 0L));
        dashboard.setLowCount(severityCounts.getOrDefault(TriageAlert.AlertSeverity.LOW, 0L));
        dashboard.setOverdueCount((long) overdueAlerts.size());
        dashboard.setUpcomingAlerts(upcomingAlerts.stream()
            .map(this::toAlertDto)
            .collect(Collectors.toList()));
        dashboard.setOverdueAlerts(overdueAlerts.stream()
            .map(this::toAlertDto)
            .collect(Collectors.toList()));
        
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'CASE_MANAGER')")
    public ResponseEntity<List<AlertDto>> getAlerts(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workerId) {
        
        List<TriageAlert> alerts;
        
        if (status != null) {
            TriageAlert.AlertStatus alertStatus = TriageAlert.AlertStatus.valueOf(status);
            alerts = alertRepository.findByStatusOrderBySeverityAscDueDateAsc(alertStatus);
        } else {
            alerts = alertRepository.findAll();
        }
        
        // Apply filters
        if (severity != null) {
            TriageAlert.AlertSeverity alertSeverity = TriageAlert.AlertSeverity.valueOf(severity);
            alerts = alerts.stream()
                .filter(a -> alertSeverity.equals(a.getSeverity()))
                .collect(Collectors.toList());
        }
        
        if (type != null) {
            TriageAlert.AlertType alertType = TriageAlert.AlertType.valueOf(type);
            alerts = alerts.stream()
                .filter(a -> alertType.equals(a.getAlertType()))
                .collect(Collectors.toList());
        }
        
        if (workerId != null) {
            UUID workerUuid = UUID.fromString(workerId);
            alerts = alerts.stream()
                .filter(a -> workerUuid.equals(a.getAssignedWorkerId()))
                .collect(Collectors.toList());
        }
        
        List<AlertDto> alertDtos = alerts.stream()
            .map(this::toAlertDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(alertDtos);
    }
    
    @PutMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'CASE_MANAGER')")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestHeader("X-User-Id") String userId) {
        
        alertRepository.findById(UUID.fromString(alertId))
            .ifPresent(alert -> {
                alert.acknowledge(UUID.fromString(userId));
                alertRepository.save(alert);
            });
        
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'CASE_MANAGER')")
    public ResponseEntity<Void> resolveAlert(@PathVariable String alertId) {
        
        alertRepository.findById(UUID.fromString(alertId))
            .ifPresent(alert -> {
                alert.resolve();
                alertRepository.save(alert);
            });
        
        return ResponseEntity.ok().build();
    }
    
    private AlertDto toAlertDto(TriageAlert alert) {
        AlertDto dto = new AlertDto();
        dto.setId(alert.getId().toString());
        dto.setClientId(alert.getClientId().toString());
        dto.setClientName(alert.getClientName());
        dto.setAlertType(alert.getAlertType().name());
        dto.setSeverity(alert.getSeverity().name());
        dto.setDescription(alert.getDescription());
        dto.setDueDate(alert.getDueDate());
        dto.setStatus(alert.getStatus().name());
        dto.setCaseNumber(alert.getCaseNumber());
        dto.setAssignedWorkerId(alert.getAssignedWorkerId() != null ? 
            alert.getAssignedWorkerId().toString() : null);
        dto.setAssignedWorkerName(alert.getAssignedWorkerName());
        dto.setIsOverdue(alert.isOverdue());
        dto.setDaysUntilDue(alert.getDaysUntilDue());
        return dto;
    }
    
    // DTOs
    public static class TriageDashboardDto {
        private Long criticalCount;
        private Long highCount;
        private Long mediumCount;
        private Long lowCount;
        private Long overdueCount;
        private List<AlertDto> upcomingAlerts;
        private List<AlertDto> overdueAlerts;
        
        // Getters and Setters
        public Long getCriticalCount() { return criticalCount; }
        public void setCriticalCount(Long criticalCount) { this.criticalCount = criticalCount; }
        
        public Long getHighCount() { return highCount; }
        public void setHighCount(Long highCount) { this.highCount = highCount; }
        
        public Long getMediumCount() { return mediumCount; }
        public void setMediumCount(Long mediumCount) { this.mediumCount = mediumCount; }
        
        public Long getLowCount() { return lowCount; }
        public void setLowCount(Long lowCount) { this.lowCount = lowCount; }
        
        public Long getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Long overdueCount) { this.overdueCount = overdueCount; }
        
        public List<AlertDto> getUpcomingAlerts() { return upcomingAlerts; }
        public void setUpcomingAlerts(List<AlertDto> upcomingAlerts) { this.upcomingAlerts = upcomingAlerts; }
        
        public List<AlertDto> getOverdueAlerts() { return overdueAlerts; }
        public void setOverdueAlerts(List<AlertDto> overdueAlerts) { this.overdueAlerts = overdueAlerts; }
    }
    
    public static class AlertDto {
        private String id;
        private String clientId;
        private String clientName;
        private String alertType;
        private String severity;
        private String description;
        private LocalDate dueDate;
        private String status;
        private String caseNumber;
        private String assignedWorkerId;
        private String assignedWorkerName;
        private Boolean isOverdue;
        private Integer daysUntilDue;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
        
        public String getAssignedWorkerId() { return assignedWorkerId; }
        public void setAssignedWorkerId(String assignedWorkerId) { this.assignedWorkerId = assignedWorkerId; }
        
        public String getAssignedWorkerName() { return assignedWorkerName; }
        public void setAssignedWorkerName(String assignedWorkerName) { this.assignedWorkerName = assignedWorkerName; }
        
        public Boolean getIsOverdue() { return isOverdue; }
        public void setIsOverdue(Boolean isOverdue) { this.isOverdue = isOverdue; }
        
        public Integer getDaysUntilDue() { return daysUntilDue; }
        public void setDaysUntilDue(Integer daysUntilDue) { this.daysUntilDue = daysUntilDue; }
    }
}