package org.haven.programenrollment.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing data quality alerts related to TH/RRH transitions
 */
@Service
@Transactional
public class DataQualityAlertService {

    private final DataQualityAlertRepository alertRepository;
    private final EmailNotificationService emailNotificationService;

    public DataQualityAlertService(DataQualityAlertRepository alertRepository,
                                  EmailNotificationService emailNotificationService) {
        this.alertRepository = alertRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Create a new data quality alert
     */
    public UUID createAlert(DataQualityAlert alert) {
        alertRepository.save(alert);

        // Send immediate notification for high-severity alerts
        if (alert.getSeverity() == DataQualityAlert.Severity.HIGH) {
            emailNotificationService.sendHighSeverityAlert(alert);
        }

        return alert.getId();
    }

    /**
     * Resolve an alert
     */
    public void resolveAlert(UUID alertId, String resolvedBy, String resolutionNotes) {
        DataQualityAlert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        alert.resolve(resolvedBy, resolutionNotes);
        alertRepository.save(alert);
    }

    /**
     * Get unresolved alerts
     */
    @Transactional(readOnly = true)
    public List<DataQualityAlert> getUnresolvedAlerts() {
        return alertRepository.findUnresolved();
    }

    /**
     * Get alerts for a specific enrollment
     */
    @Transactional(readOnly = true)
    public List<DataQualityAlert> getAlertsForEnrollment(UUID enrollmentId) {
        return alertRepository.findByEnrollmentId(enrollmentId);
    }

    /**
     * Get alerts by type
     */
    @Transactional(readOnly = true)
    public List<DataQualityAlert> getAlertsByType(DataQualityAlert.AlertType alertType) {
        return alertRepository.findByAlertType(alertType);
    }

    /**
     * Get alert statistics for dashboard
     */
    @Transactional(readOnly = true)
    public AlertStatistics getAlertStatistics() {
        long totalAlerts = alertRepository.countAll();
        long unresolvedAlerts = alertRepository.countUnresolved();
        long highSeverityAlerts = alertRepository.countBySeverity(DataQualityAlert.Severity.HIGH);
        long alertsToday = alertRepository.countCreatedToday();

        return new AlertStatistics(totalAlerts, unresolvedAlerts, highSeverityAlerts, alertsToday);
    }

    /**
     * Alert statistics for dashboard
     */
    public static class AlertStatistics {
        private long totalAlerts;
        private long unresolvedAlerts;
        private long highSeverityAlerts;
        private long alertsToday;

        public AlertStatistics(long totalAlerts, long unresolvedAlerts, long highSeverityAlerts, long alertsToday) {
            this.totalAlerts = totalAlerts;
            this.unresolvedAlerts = unresolvedAlerts;
            this.highSeverityAlerts = highSeverityAlerts;
            this.alertsToday = alertsToday;
        }

        // Getters
        public long getTotalAlerts() { return totalAlerts; }
        public long getUnresolvedAlerts() { return unresolvedAlerts; }
        public long getHighSeverityAlerts() { return highSeverityAlerts; }
        public long getAlertsToday() { return alertsToday; }
    }
}