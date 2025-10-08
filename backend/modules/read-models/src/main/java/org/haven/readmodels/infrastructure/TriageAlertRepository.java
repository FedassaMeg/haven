package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.TriageAlert;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class TriageAlertRepository {
    
    private final JpaTriageAlertRepository jpaRepository;
    
    public TriageAlertRepository(JpaTriageAlertRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    public Optional<TriageAlert> findByClientIdAndAlertType(UUID clientId, TriageAlert.AlertType alertType) {
        return jpaRepository.findByClientIdAndAlertType(clientId, alertType)
            .map(JpaTriageAlertEntity::toDomain);
    }
    
    public Optional<TriageAlert> findByClientIdAndAlertTypeAndStatus(
        UUID clientId, 
        TriageAlert.AlertType alertType,
        TriageAlert.AlertStatus status
    ) {
        return jpaRepository.findByClientIdAndAlertTypeAndStatus(clientId, alertType, status)
            .map(JpaTriageAlertEntity::toDomain);
    }
    
    public List<TriageAlert> findByStatusOrderBySeverityAscDueDateAsc(TriageAlert.AlertStatus status) {
        return jpaRepository.findByStatusOrderBySeverityAscDueDateAsc(status)
            .stream()
            .map(JpaTriageAlertEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public List<TriageAlert> findByAssignedWorkerIdAndStatus(UUID workerId, TriageAlert.AlertStatus status) {
        return jpaRepository.findByAssignedWorkerIdAndStatus(workerId, status)
            .stream()
            .map(JpaTriageAlertEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public List<TriageAlert> findActiveAlertsDueBefore(TriageAlert.AlertStatus status, LocalDate date) {
        return jpaRepository.findActiveAlertsDueBefore(status, date)
            .stream()
            .map(JpaTriageAlertEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public List<TriageAlert> findBySeverityAndStatus(TriageAlert.AlertSeverity severity, TriageAlert.AlertStatus status) {
        return jpaRepository.findBySeverityAndStatus(severity, status)
            .stream()
            .map(JpaTriageAlertEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public Long countActiveBySeverity(TriageAlert.AlertSeverity severity) {
        return jpaRepository.countActiveBySeverity(severity);
    }
    
    public List<TriageAlert> findOverdueAlerts() {
        return jpaRepository.findOverdueAlerts()
            .stream()
            .map(JpaTriageAlertEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public Optional<TriageAlert> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(JpaTriageAlertEntity::toDomain);
    }
    
    public TriageAlert save(TriageAlert alert) {
        JpaTriageAlertEntity entity = jpaRepository.findById(alert.getId())
            .orElse(new JpaTriageAlertEntity(alert));
        
        entity.updateFrom(alert);
        JpaTriageAlertEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    public List<TriageAlert> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(JpaTriageAlertEntity::toDomain)
            .collect(Collectors.toList());
    }
}