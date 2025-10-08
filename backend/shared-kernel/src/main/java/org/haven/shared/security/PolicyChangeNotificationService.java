package org.haven.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for notifying compliance team of policy changes and anomalies
 * Monitors access patterns and alerts on policy violations or unusual activity
 */
@Service
public class PolicyChangeNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyChangeNotificationService.class);

    // In production, this would use email/Slack/other notification channels
    private final List<PolicyNotification> notificationLog = new ArrayList<>();

    /**
     * Listen for policy decision events and alert on denied access
     */
    @EventListener
    public void onPolicyDecision(org.haven.shared.events.PolicyDecisionMade event) {
        // Alert on denied access to sensitive resources
        if (!event.isAllowed()) {
            handleDeniedAccess(event);
        }

        // Alert on high-risk access patterns
        if (isHighRiskAccess(event)) {
            handleHighRiskAccess(event);
        }
    }

    /**
     * Handle denied access attempts
     */
    private void handleDeniedAccess(org.haven.shared.events.PolicyDecisionMade event) {
        String message = String.format(
                "DENIED ACCESS: User %s (%s) attempted to access %s (ID: %s). " +
                "Rule: %s. Reason: %s. IP: %s",
                event.getUserName(),
                event.getUserId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getPolicyRule(),
                event.getReason(),
                event.getIpAddress()
        );

        PolicyNotification notification = new PolicyNotification(
                UUID.randomUUID(),
                NotificationType.DENIED_ACCESS,
                NotificationSeverity.MEDIUM,
                message,
                event.getUserId(),
                event.getResourceId(),
                event.getPolicyRule(),
                Instant.now()
        );

        logNotification(notification);

        // In production, send to compliance team
        // emailService.sendToComplianceTeam(notification);
        // slackService.sendAlert(notification);
    }

    /**
     * Handle high-risk access attempts
     */
    private void handleHighRiskAccess(org.haven.shared.events.PolicyDecisionMade event) {
        String message = String.format(
                "HIGH RISK ACCESS: User %s (%s) accessed %s (ID: %s) with rule %s. " +
                "Context: %s",
                event.getUserName(),
                event.getUserId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getPolicyRule(),
                event.getDecisionContext()
        );

        PolicyNotification notification = new PolicyNotification(
                UUID.randomUUID(),
                NotificationType.HIGH_RISK_ACCESS,
                NotificationSeverity.HIGH,
                message,
                event.getUserId(),
                event.getResourceId(),
                event.getPolicyRule(),
                Instant.now()
        );

        logNotification(notification);

        // In production, send immediate alert
        // emailService.sendUrgentAlert(notification);
        // slackService.sendUrgentAlert(notification);
    }

    /**
     * Determine if access is high-risk
     */
    private boolean isHighRiskAccess(org.haven.shared.events.PolicyDecisionMade event) {
        // High-risk rules that require notification
        return event.getPolicyRule().equals("SEALED_NOTE_RESTRICTION") ||
               event.getPolicyRule().equals("PRIVILEGED_COUNSELING_ACCESS") ||
               event.getPolicyRule().equals("VSP_VAWA_RESTRICTION") ||
               event.getPolicyRule().equals("SCOPE_ATTORNEY_CLIENT");
    }

    /**
     * Log notification for audit trail
     */
    private void logNotification(PolicyNotification notification) {
        notificationLog.add(notification);
        logger.warn("POLICY_NOTIFICATION: {} - {}", notification.severity, notification.message);

        // In production, persist to database for compliance reporting
        // notificationRepository.save(notification);
    }

    /**
     * Get recent notifications for compliance dashboard
     */
    public List<PolicyNotification> getRecentNotifications(int limit) {
        return notificationLog.stream()
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .toList();
    }

    /**
     * Get notifications by severity
     */
    public List<PolicyNotification> getNotificationsBySeverity(NotificationSeverity severity) {
        return notificationLog.stream()
                .filter(n -> n.severity == severity)
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .toList();
    }

    /**
     * Policy notification record
     */
    public static class PolicyNotification {
        private final UUID notificationId;
        private final NotificationType type;
        private final NotificationSeverity severity;
        private final String message;
        private final UUID userId;
        private final UUID resourceId;
        private final String policyRule;
        private final Instant timestamp;

        public PolicyNotification(UUID notificationId, NotificationType type,
                                NotificationSeverity severity, String message,
                                UUID userId, UUID resourceId, String policyRule,
                                Instant timestamp) {
            this.notificationId = notificationId;
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.userId = userId;
            this.resourceId = resourceId;
            this.policyRule = policyRule;
            this.timestamp = timestamp;
        }

        public UUID getNotificationId() { return notificationId; }
        public NotificationType getType() { return type; }
        public NotificationSeverity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public UUID getUserId() { return userId; }
        public UUID getResourceId() { return resourceId; }
        public String getPolicyRule() { return policyRule; }
        public Instant getTimestamp() { return timestamp; }
    }

    public enum NotificationType {
        DENIED_ACCESS,
        HIGH_RISK_ACCESS,
        POLICY_VIOLATION,
        UNUSUAL_PATTERN
    }

    public enum NotificationSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
