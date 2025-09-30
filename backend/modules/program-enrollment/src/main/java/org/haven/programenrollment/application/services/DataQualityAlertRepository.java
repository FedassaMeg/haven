package org.haven.programenrollment.application.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DataQualityAlert
 */
public interface DataQualityAlertRepository {

    /**
     * Save an alert
     */
    void save(DataQualityAlert alert);

    /**
     * Find alert by ID
     */
    Optional<DataQualityAlert> findById(UUID alertId);

    /**
     * Find unresolved alerts
     */
    List<DataQualityAlert> findUnresolved();

    /**
     * Find alerts for a specific enrollment
     */
    List<DataQualityAlert> findByEnrollmentId(UUID enrollmentId);

    /**
     * Find alerts by type
     */
    List<DataQualityAlert> findByAlertType(DataQualityAlert.AlertType alertType);

    /**
     * Find alerts by severity
     */
    List<DataQualityAlert> findBySeverity(DataQualityAlert.Severity severity);

    /**
     * Count all alerts
     */
    long countAll();

    /**
     * Count unresolved alerts
     */
    long countUnresolved();

    /**
     * Count alerts by severity
     */
    long countBySeverity(DataQualityAlert.Severity severity);

    /**
     * Count alerts created today
     */
    long countCreatedToday();

    /**
     * Find alerts requiring escalation (high severity, unresolved for > X days)
     */
    List<DataQualityAlert> findAlertsRequiringEscalation(int daysThreshold);
}