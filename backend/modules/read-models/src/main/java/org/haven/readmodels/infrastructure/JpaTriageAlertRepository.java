package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.TriageAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTriageAlertRepository extends JpaRepository<JpaTriageAlertEntity, UUID> {
    
    Optional<JpaTriageAlertEntity> findByClientIdAndAlertType(UUID clientId, TriageAlert.AlertType alertType);
    
    Optional<JpaTriageAlertEntity> findByClientIdAndAlertTypeAndStatus(
        UUID clientId, 
        TriageAlert.AlertType alertType,
        TriageAlert.AlertStatus status
    );
    
    List<JpaTriageAlertEntity> findByStatusOrderBySeverityAscDueDateAsc(TriageAlert.AlertStatus status);
    
    List<JpaTriageAlertEntity> findByAssignedWorkerIdAndStatus(UUID workerId, TriageAlert.AlertStatus status);
    
    @Query("SELECT a FROM JpaTriageAlertEntity a WHERE a.status = :status AND a.dueDate <= :date ORDER BY a.severity ASC, a.dueDate ASC")
    List<JpaTriageAlertEntity> findActiveAlertsDueBefore(@Param("status") TriageAlert.AlertStatus status, @Param("date") LocalDate date);
    
    @Query("SELECT a FROM JpaTriageAlertEntity a WHERE a.severity = :severity AND a.status = :status")
    List<JpaTriageAlertEntity> findBySeverityAndStatus(@Param("severity") TriageAlert.AlertSeverity severity, @Param("status") TriageAlert.AlertStatus status);
    
    @Query("SELECT COUNT(a) FROM JpaTriageAlertEntity a WHERE a.status = 'ACTIVE' AND a.severity = :severity")
    Long countActiveBySeverity(@Param("severity") TriageAlert.AlertSeverity severity);
    
    @Query("SELECT a FROM JpaTriageAlertEntity a WHERE a.status = 'ACTIVE' AND a.dueDate < CURRENT_DATE")
    List<JpaTriageAlertEntity> findOverdueAlerts();
}