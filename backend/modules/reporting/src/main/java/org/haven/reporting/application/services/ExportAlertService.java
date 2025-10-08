package org.haven.reporting.application.services;

import org.haven.shared.security.PolicyChangeNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Alert service for export generation monitoring.
 * Extends PolicyChangeNotificationService with export-specific alerts.
 *
 * Alerts on:
 * - SLA violations (>5 minutes)
 * - Data quality failures
 * - Excessive consent restrictions (>10%)
 * - Validation warnings
 */
@Service
public class ExportAlertService {

    private static final Logger logger = LoggerFactory.getLogger(ExportAlertService.class);

    private static final long SLA_THRESHOLD_MINUTES = 5;
    private static final double CONSENT_RESTRICTION_THRESHOLD = 0.10; // 10%
    private static final int WARNING_THRESHOLD = 10;

    private final PolicyChangeNotificationService notificationService;

    public ExportAlertService(PolicyChangeNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Check and alert on SLA violation
     */
    public void checkSLAViolation(UUID exportJobId, String exportType, Instant startTime, Instant endTime) {
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();

        if (durationMinutes > SLA_THRESHOLD_MINUTES) {
            String message = String.format(
                    "EXPORT SLA VIOLATION: Export job %s (%s) exceeded %d-minute SLA. " +
                    "Duration: %d minutes. Investigate performance bottlenecks.",
                    exportJobId, exportType, SLA_THRESHOLD_MINUTES, durationMinutes
            );

            logger.error(message);

            // Create notification
            PolicyChangeNotificationService.PolicyNotification notification =
                    new PolicyChangeNotificationService.PolicyNotification(
                            UUID.randomUUID(),
                            PolicyChangeNotificationService.NotificationType.POLICY_VIOLATION,
                            PolicyChangeNotificationService.NotificationSeverity.HIGH,
                            message,
                            null,
                            exportJobId,
                            "EXPORT_SLA",
                            Instant.now()
                    );

            // In production: send email/Slack alert
            sendAlert(notification);
        }
    }

    /**
     * Alert on data quality check failures
     */
    public void alertDataQualityFailure(
            UUID exportJobId,
            String exportType,
            int errorCount,
            List<String> errors) {

        String sampleErrors = errors.stream()
                .limit(5)
                .reduce((a, b) -> a + "; " + b)
                .orElse("No errors");

        String message = String.format(
                "DATA QUALITY FAILURE: Export job %s (%s) failed mandatory element thresholds. " +
                "Errors: %d. Sample errors: %s",
                exportJobId, exportType, errorCount, sampleErrors
        );

        logger.error(message);

        PolicyChangeNotificationService.PolicyNotification notification =
                new PolicyChangeNotificationService.PolicyNotification(
                        UUID.randomUUID(),
                        PolicyChangeNotificationService.NotificationType.POLICY_VIOLATION,
                        PolicyChangeNotificationService.NotificationSeverity.CRITICAL,
                        message,
                        null,
                        exportJobId,
                        "DATA_QUALITY",
                        Instant.now()
                );

        sendAlert(notification);
    }

    /**
     * Alert on excessive consent restrictions
     */
    public void checkExcessiveConsentRestrictions(
            UUID exportJobId,
            String exportType,
            long totalRecords,
            long excludedRecords) {

        if (totalRecords == 0) return;

        double exclusionRate = (double) excludedRecords / totalRecords;

        if (exclusionRate > CONSENT_RESTRICTION_THRESHOLD) {
            String message = String.format(
                    "EXCESSIVE CONSENT RESTRICTIONS: Export job %s (%s) excluded %.1f%% of records " +
                    "due to consent restrictions (%d of %d). Threshold: %.1f%%. " +
                    "Possible configuration error - verify VSP flags and consent settings.",
                    exportJobId, exportType, exclusionRate * 100, excludedRecords, totalRecords,
                    CONSENT_RESTRICTION_THRESHOLD * 100
            );

            logger.warn(message);

            PolicyChangeNotificationService.PolicyNotification notification =
                    new PolicyChangeNotificationService.PolicyNotification(
                            UUID.randomUUID(),
                            PolicyChangeNotificationService.NotificationType.UNUSUAL_PATTERN,
                            PolicyChangeNotificationService.NotificationSeverity.HIGH,
                            message,
                            null,
                            exportJobId,
                            "CONSENT_RESTRICTION",
                            Instant.now()
                    );

            sendAlert(notification);
        }
    }

    /**
     * Alert on validation warnings
     */
    public void checkValidationWarnings(
            UUID exportJobId,
            String exportType,
            int warningCount,
            List<String> warnings) {

        if (warningCount > WARNING_THRESHOLD) {
            String sampleWarnings = warnings.stream()
                    .limit(5)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("No warnings");

            String message = String.format(
                    "EXPORT VALIDATION WARNINGS: Export job %s (%s) generated %d validation warnings " +
                    "(threshold: %d). Review data quality issues before HUD submission. " +
                    "Sample warnings: %s",
                    exportJobId, exportType, warningCount, WARNING_THRESHOLD, sampleWarnings
            );

            logger.warn(message);

            PolicyChangeNotificationService.PolicyNotification notification =
                    new PolicyChangeNotificationService.PolicyNotification(
                            UUID.randomUUID(),
                            PolicyChangeNotificationService.NotificationType.POLICY_VIOLATION,
                            PolicyChangeNotificationService.NotificationSeverity.MEDIUM,
                            message,
                            null,
                            exportJobId,
                            "VALIDATION",
                            Instant.now()
                    );

            sendAlert(notification);
        }
    }

    /**
     * Alert on completeness rate failures
     */
    public void alertCompletenessFailure(
            UUID exportJobId,
            String exportType,
            String dataElement,
            double completenessRate,
            double threshold) {

        String message = String.format(
                "COMPLETENESS RATE FAILURE: Export job %s (%s) - %s completeness %.1f%% " +
                "below required threshold %.1f%%. HUD submission may be rejected.",
                exportJobId, exportType, dataElement, completenessRate * 100, threshold * 100
        );

        logger.error(message);

        PolicyChangeNotificationService.PolicyNotification notification =
                new PolicyChangeNotificationService.PolicyNotification(
                        UUID.randomUUID(),
                        PolicyChangeNotificationService.NotificationType.POLICY_VIOLATION,
                        PolicyChangeNotificationService.NotificationSeverity.CRITICAL,
                        message,
                        null,
                        exportJobId,
                        "COMPLETENESS",
                        Instant.now()
                );

        sendAlert(notification);
    }

    /**
     * Alert on referential integrity errors
     */
    public void alertReferentialIntegrityErrors(
            UUID exportJobId,
            String exportType,
            int errorCount,
            List<String> errors) {

        String sampleErrors = errors.stream()
                .limit(5)
                .reduce((a, b) -> a + "; " + b)
                .orElse("No errors");

        String message = String.format(
                "REFERENTIAL INTEGRITY ERRORS: Export job %s (%s) has %d broken references. " +
                "Sample errors: %s. Export is invalid and cannot be submitted to HUD.",
                exportJobId, exportType, errorCount, sampleErrors
        );

        logger.error(message);

        PolicyChangeNotificationService.PolicyNotification notification =
                new PolicyChangeNotificationService.PolicyNotification(
                        UUID.randomUUID(),
                        PolicyChangeNotificationService.NotificationType.POLICY_VIOLATION,
                        PolicyChangeNotificationService.NotificationSeverity.CRITICAL,
                        message,
                        null,
                        exportJobId,
                        "REFERENTIAL_INTEGRITY",
                        Instant.now()
                );

        sendAlert(notification);
    }

    /**
     * Alert on export package integrity failure
     */
    public void alertPackageIntegrityFailure(
            UUID exportJobId,
            String reason) {

        String message = String.format(
                "EXPORT PACKAGE INTEGRITY FAILURE: Export job %s failed integrity check. " +
                "Reason: %s. Package may be corrupted or tampered with.",
                exportJobId, reason
        );

        logger.error(message);

        PolicyChangeNotificationService.PolicyNotification notification =
                new PolicyChangeNotificationService.PolicyNotification(
                        UUID.randomUUID(),
                        PolicyChangeNotificationService.NotificationType.POLICY_VIOLATION,
                        PolicyChangeNotificationService.NotificationSeverity.CRITICAL,
                        message,
                        null,
                        exportJobId,
                        "PACKAGE_INTEGRITY",
                        Instant.now()
                );

        sendAlert(notification);
    }

    /**
     * Send alert through configured channels
     */
    private void sendAlert(PolicyChangeNotificationService.PolicyNotification notification) {
        // Log to notification service
        logger.warn("ALERT [{}]: {}", notification.getSeverity(), notification.getMessage());

        // In production, integrate with:
        // - Email (compliance team, data quality team)
        // - Slack/Teams webhooks
        // - Monitoring dashboard (Grafana, DataDog)
        // - PagerDuty for critical alerts

        // Placeholder for actual implementation:
        // if (notification.getSeverity() == NotificationSeverity.CRITICAL) {
        //     pagerDutyService.triggerIncident(notification);
        // }
        // slackService.sendToChannel("#data-quality", notification);
        // emailService.sendToGroup("data-quality@haven.org", notification);
    }

    /**
     * Generate dashboard metrics for monitoring
     */
    public ExportMetrics getExportMetrics(UUID exportJobId) {
        // Return metrics for monitoring dashboard
        return new ExportMetrics(
                exportJobId,
                0L,  // duration
                0,   // error count
                0,   // warning count
                0.0, // completeness rate
                false // SLA violated
        );
    }

    public record ExportMetrics(
            UUID exportJobId,
            long durationMillis,
            int errorCount,
            int warningCount,
            double completenessRate,
            boolean slaViolated
    ) {}
}
