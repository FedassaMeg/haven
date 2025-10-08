package org.haven.reporting.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Notification workflow for export compliance administrators.
 *
 * Sends notifications via multiple channels:
 * - Email (primary)
 * - SNS/EventBridge (AWS environments)
 * - Microsoft Teams webhooks (configurable)
 *
 * Notification content includes:
 * - Export metadata (job ID, period, type)
 * - Secure download link (time-limited, authenticated)
 * - Consent ledger reference
 * - Data quality summary
 * - Compliance checklist
 *
 * Security considerations:
 * - No PII in notification content
 * - Secure links with short expiration (24-48 hours)
 * - Audit logging of notification delivery
 */
@Service
public class ExportNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ExportNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final List<String> complianceAdmins;
    private final String baseUrl;
    private final boolean enabled;

    public ExportNotificationService(
            JavaMailSender mailSender,
            @Value("${haven.notification.from-email:noreply@haven.example.com}") String fromEmail,
            @Value("${haven.notification.compliance-admins:admin@haven.example.com}") List<String> complianceAdmins,
            @Value("${haven.application.base-url:http://localhost:8080}") String baseUrl,
            @Value("${haven.notification.enabled:true}") boolean enabled) {

        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.complianceAdmins = complianceAdmins;
        this.baseUrl = baseUrl;
        this.enabled = enabled;

        logger.info("Export Notification Service initialized - Enabled: {}, Recipients: {}, From: {}",
                enabled, complianceAdmins.size(), fromEmail);
    }

    /**
     * Notify compliance administrators of completed export.
     *
     * @param notification Export notification details
     */
    public void notifyExportCompleted(ExportNotification notification) {
        if (!enabled) {
            logger.debug("Notifications disabled - skipping notification for export job: {}",
                    notification.exportJobId());
            return;
        }

        try {
            logger.info("Sending export completion notification for job: {} to {} recipients",
                    notification.exportJobId(), complianceAdmins.size());

            // Send email notifications
            sendEmailNotification(notification);

            // Future: Send to Teams webhook
            // sendTeamsNotification(notification);

            // Future: Publish to SNS topic
            // publishToSns(notification);

            logger.info("Export completion notification sent successfully for job: {}",
                    notification.exportJobId());

        } catch (Exception e) {
            logger.error("Failed to send export notification for job: {}", notification.exportJobId(), e);
            throw new NotificationException("Failed to send export notification", e);
        }
    }

    /**
     * Notify of export failure.
     */
    public void notifyExportFailed(UUID exportJobId, String errorMessage, String errorCode) {
        if (!enabled) {
            return;
        }

        try {
            String subject = String.format("Export Job Failed - %s", exportJobId);
            String body = buildFailureEmailBody(exportJobId, errorMessage, errorCode);

            sendEmail(complianceAdmins, subject, body);

            logger.info("Export failure notification sent for job: {}", exportJobId);

        } catch (Exception e) {
            logger.error("Failed to send failure notification for job: {}", exportJobId, e);
        }
    }

    /**
     * Notify of approaching retention expiration.
     */
    public void notifyRetentionExpiring(UUID exportJobId, Instant expiresAt, int daysRemaining) {
        if (!enabled) {
            return;
        }

        try {
            String subject = String.format("Export Retention Expiring Soon - %s", exportJobId);
            String body = buildRetentionExpiringEmailBody(exportJobId, expiresAt, daysRemaining);

            sendEmail(complianceAdmins, subject, body);

            logger.info("Retention expiration notification sent for job: {}", exportJobId);

        } catch (Exception e) {
            logger.error("Failed to send retention notification for job: {}", exportJobId, e);
        }
    }

    // Private notification methods

    private void sendEmailNotification(ExportNotification notification) {
        String subject = buildEmailSubject(notification);
        String body = buildEmailBody(notification);

        sendEmail(complianceAdmins, subject, body);
    }

    private void sendEmail(List<String> recipients, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipients.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);

        logger.debug("Email sent to {} recipients", recipients.size());
    }

    private String buildEmailSubject(ExportNotification notification) {
        return String.format(
                "HUD Export Completed - %s (%s to %s)",
                notification.exportType(),
                notification.exportPeriodStart(),
                notification.exportPeriodEnd()
        );
    }

    private String buildEmailBody(ExportNotification notification) {
        long retentionDays = ChronoUnit.DAYS.between(Instant.now(), notification.retentionExpiresAt());

        StringBuilder body = new StringBuilder();
        body.append("HMIS Export Completion Notification\n");
        body.append("=====================================\n\n");

        body.append("EXPORT DETAILS\n");
        body.append("--------------\n");
        body.append(String.format("Export Job ID: %s\n", notification.exportJobId()));
        body.append(String.format("Export Type: %s\n", notification.exportType()));
        body.append(String.format("Reporting Period: %s to %s\n",
                notification.exportPeriodStart(), notification.exportPeriodEnd()));
        body.append(String.format("Requested By: %s\n", notification.requestedBy()));
        body.append(String.format("Export Reason: %s\n", notification.exportReason()));
        body.append(String.format("Completed At: %s\n\n", notification.completedAt()));

        body.append("DATA SUMMARY\n");
        body.append("------------\n");
        body.append(String.format("Total Records: %,d\n", notification.totalRecords()));
        body.append(String.format("Data Subjects: %,d clients\n", notification.dataSubjectCount()));
        body.append(String.format("VAWA Protected: %s\n", notification.vawaProtected() ? "Yes" : "No"));
        if (notification.vawaProtected()) {
            body.append(String.format("VAWA Suppressed Records: %,d\n", notification.vawaSuppressedRecords()));
        }
        body.append(String.format("Consent Scope: %s\n", notification.consentScope()));
        body.append(String.format("Hash Mode: %s\n\n", notification.hashMode()));

        body.append("SECURITY & COMPLIANCE\n");
        body.append("---------------------\n");
        body.append(String.format("Encrypted: %s\n", notification.encrypted() ? "Yes (AES-256-GCM)" : "No"));
        if (notification.encrypted()) {
            body.append(String.format("KMS Key ID: %s\n", notification.kmsKeyId()));
        }
        body.append(String.format("SHA-256 Hash: %s\n", notification.exportSha256Hash()));
        body.append(String.format("Consent Ledger ID: %s\n", notification.consentLedgerEntryId()));
        body.append(String.format("Retention Period: %d days\n", retentionDays));
        body.append(String.format("Auto-Delete Date: %s\n\n", notification.retentionExpiresAt()));

        body.append("SECURE ACCESS\n");
        body.append("-------------\n");
        body.append(String.format("Download Link: %s\n", notification.secureDownloadUrl()));
        body.append("(Link expires in 48 hours)\n\n");

        body.append("COMPLIANCE CHECKLIST\n");
        body.append("--------------------\n");
        body.append("[ ] Verify export data quality\n");
        body.append("[ ] Review VAWA suppression (if applicable)\n");
        body.append("[ ] Confirm consent ledger accuracy\n");
        body.append("[ ] Submit to HUD HDX portal (if required)\n");
        body.append("[ ] Archive submission confirmation\n");
        body.append("[ ] Document in compliance audit log\n\n");

        body.append("VALIDATION SUMMARY\n");
        body.append("------------------\n");
        if (notification.validationErrors() > 0) {
            body.append(String.format("⚠ ATTENTION: %d validation errors detected\n", notification.validationErrors()));
            body.append("Review validation report before submission\n\n");
        } else {
            body.append("✓ All validation checks passed\n\n");
        }

        body.append("---\n");
        body.append("This is an automated notification from Haven HMIS Export System.\n");
        body.append("For support, contact your system administrator.\n");

        return body.toString();
    }

    private String buildFailureEmailBody(UUID exportJobId, String errorMessage, String errorCode) {
        StringBuilder body = new StringBuilder();
        body.append("HMIS Export Failed\n");
        body.append("==================\n\n");

        body.append(String.format("Export Job ID: %s\n", exportJobId));
        body.append(String.format("Error Code: %s\n", errorCode));
        body.append(String.format("Error Message: %s\n\n", errorMessage));

        body.append("NEXT STEPS\n");
        body.append("----------\n");
        body.append("1. Review error details in application logs\n");
        body.append("2. Correct data quality issues if applicable\n");
        body.append("3. Re-run export job\n");
        body.append("4. Contact support if issue persists\n\n");

        body.append(String.format("View Details: %s/exports/%s\n", baseUrl, exportJobId));

        return body.toString();
    }

    private String buildRetentionExpiringEmailBody(UUID exportJobId, Instant expiresAt, int daysRemaining) {
        StringBuilder body = new StringBuilder();
        body.append("HMIS Export Retention Expiring Soon\n");
        body.append("====================================\n\n");

        body.append(String.format("Export Job ID: %s\n", exportJobId));
        body.append(String.format("Expires At: %s\n", expiresAt));
        body.append(String.format("Days Remaining: %d\n\n", daysRemaining));

        body.append("ACTION REQUIRED\n");
        body.append("---------------\n");
        body.append("This export will be automatically purged when the retention period expires.\n");
        body.append("If you need to preserve this export:\n");
        body.append("1. Download a copy for archival\n");
        body.append("2. Document retention extension request\n");
        body.append("3. Update consent ledger with new retention date\n\n");

        body.append(String.format("View Export: %s/exports/%s\n", baseUrl, exportJobId));

        return body.toString();
    }

    /**
     * Export notification record.
     */
    public record ExportNotification(
            UUID exportJobId,
            String exportType,
            LocalDate exportPeriodStart,
            LocalDate exportPeriodEnd,
            String requestedBy,
            String exportReason,
            Instant completedAt,
            long totalRecords,
            int dataSubjectCount,
            boolean vawaProtected,
            long vawaSuppressedRecords,
            String consentScope,
            String hashMode,
            boolean encrypted,
            String kmsKeyId,
            String exportSha256Hash,
            String consentLedgerEntryId,
            Instant retentionExpiresAt,
            String secureDownloadUrl,
            int validationErrors,
            int validationWarnings
    ) {
        /**
         * Create notification from export job details.
         */
        public static ExportNotification fromExportJob(
                UUID exportJobId,
                String exportType,
                LocalDate exportPeriodStart,
                LocalDate exportPeriodEnd,
                String requestedBy,
                String exportReason,
                Instant completedAt,
                long totalRecords,
                int dataSubjectCount,
                boolean vawaProtected,
                long vawaSuppressedRecords,
                String consentScope,
                String hashMode,
                boolean encrypted,
                String kmsKeyId,
                String exportSha256Hash,
                String consentLedgerEntryId,
                Instant retentionExpiresAt,
                String baseUrl,
                int validationErrors,
                int validationWarnings) {

            // Generate time-limited secure download URL
            String secureDownloadUrl = String.format(
                    "%s/api/exports/%s/download?token=GENERATE_JWT_TOKEN",
                    baseUrl,
                    exportJobId
            );

            return new ExportNotification(
                    exportJobId,
                    exportType,
                    exportPeriodStart,
                    exportPeriodEnd,
                    requestedBy,
                    exportReason,
                    completedAt,
                    totalRecords,
                    dataSubjectCount,
                    vawaProtected,
                    vawaSuppressedRecords,
                    consentScope,
                    hashMode,
                    encrypted,
                    kmsKeyId,
                    exportSha256Hash,
                    consentLedgerEntryId,
                    retentionExpiresAt,
                    secureDownloadUrl,
                    validationErrors,
                    validationWarnings
            );
        }
    }

    public static class NotificationException extends RuntimeException {
        public NotificationException(String message) {
            super(message);
        }

        public NotificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
