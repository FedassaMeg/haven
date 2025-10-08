package org.haven.reporting.application.services;

import org.haven.reporting.domain.ExportConsentScope;
import org.haven.reporting.domain.ExportSecurityClearance;
import org.haven.shared.audit.AuditService;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.PolicyDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Monitoring and alerting service for export security events
 * Integrates with external monitoring systems (Splunk, Application Insights, etc.)
 *
 * Logs security-critical events:
 * - All unhashed export attempts (permit and deny)
 * - Policy violations with full context
 * - Clearance validation failures
 * - Suspicious access patterns
 *
 * Metrics exposed:
 * - export.security.unhashed.attempt (counter)
 * - export.security.policy.violation (counter)
 * - export.security.clearance.expired (counter)
 * - export.security.decision.latency (histogram)
 */
@Service
public class ExportSecurityMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ExportSecurityMonitoringService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final AuditService auditService;
    private final boolean enableSplunkLogging;
    private final boolean enableAppInsights;
    private final String environmentName;

    public ExportSecurityMonitoringService(
            AuditService auditService,
            @Value("${export.monitoring.splunk.enabled:true}") boolean enableSplunkLogging,
            @Value("${export.monitoring.appinsights.enabled:false}") boolean enableAppInsights,
            @Value("${spring.profiles.active:dev}") String environmentName) {
        this.auditService = auditService;
        this.enableSplunkLogging = enableSplunkLogging;
        this.enableAppInsights = enableAppInsights;
        this.environmentName = environmentName;
    }

    /**
     * Log unhashed export attempt with full context
     * Called for every unhashed export request, regardless of outcome
     */
    public void logUnhashedExportAttempt(
            UUID tenantId,
            PolicyDecision decision,
            Set<ExportConsentScope> requestedScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext) {

        Instant timestamp = Instant.now();
        String outcome = decision.isPermitted() ? "PERMITTED" : "DENIED";

        // Structured log for SIEM ingestion
        Map<String, Object> event = buildSecurityEvent(
                "UNHASHED_EXPORT_ATTEMPT",
                outcome,
                tenantId,
                decision,
                requestedScopes,
                clearance,
                accessContext,
                timestamp
        );

        // Log to security audit logger (picked up by log aggregators)
        logToSecurityAudit(event);

        // Log to Splunk if enabled
        if (enableSplunkLogging) {
            logToSplunk(event);
        }

        // Log to Application Insights if enabled
        if (enableAppInsights) {
            logToAppInsights(event);
        }

        // Persist to database audit trail
        auditService.logAction("UNHASHED_EXPORT_ATTEMPT", event);

        // Increment metrics
        incrementCounter("export.security.unhashed.attempt", outcome);

        // Alert on policy violations in production
        if (!decision.isPermitted() && "production".equalsIgnoreCase(environmentName)) {
            sendSecurityAlert(event);
        }
    }

    /**
     * Log policy violation with escalation
     */
    public void logPolicyViolation(
            UUID tenantId,
            String violationType,
            String reason,
            AccessContext accessContext) {

        Map<String, Object> event = new HashMap<>();
        event.put("event_type", "EXPORT_POLICY_VIOLATION");
        event.put("violation_type", violationType);
        event.put("reason", reason);
        event.put("tenant_id", tenantId.toString());
        event.put("user_id", accessContext.getUserId().toString());
        event.put("user_name", accessContext.getUserName());
        event.put("user_roles", accessContext.getRoleStrings());
        event.put("ip_address", accessContext.getIpAddress());
        event.put("session_id", accessContext.getSessionId());
        event.put("timestamp", Instant.now().toString());
        event.put("environment", environmentName);
        event.put("severity", "HIGH");

        logToSecurityAudit(event);
        auditService.logAction("EXPORT_POLICY_VIOLATION", event);
        incrementCounter("export.security.policy.violation", violationType);

        // Always alert on violations
        sendSecurityAlert(event);
    }

    /**
     * Log clearance validation failure
     */
    public void logClearanceValidationFailure(
            UUID tenantId,
            String failureReason,
            ExportSecurityClearance clearance,
            AccessContext accessContext) {

        Map<String, Object> event = new HashMap<>();
        event.put("event_type", "CLEARANCE_VALIDATION_FAILURE");
        event.put("failure_reason", failureReason);
        event.put("tenant_id", tenantId.toString());
        event.put("user_id", accessContext.getUserId().toString());
        event.put("user_name", accessContext.getUserName());
        event.put("clearance_id", clearance != null ? clearance.clearanceId().toString() : null);
        event.put("clearance_expired", clearance != null && clearance.isExpired());
        event.put("clearance_expires_at", clearance != null ? clearance.expiresAt().toString() : null);
        event.put("timestamp", Instant.now().toString());
        event.put("environment", environmentName);
        event.put("severity", "MEDIUM");

        logToSecurityAudit(event);
        auditService.logAction("CLEARANCE_VALIDATION_FAILURE", event);
        incrementCounter("export.security.clearance.expired", failureReason);
    }

    /**
     * Build comprehensive security event for SIEM
     */
    private Map<String, Object> buildSecurityEvent(
            String eventType,
            String outcome,
            UUID tenantId,
            PolicyDecision decision,
            Set<ExportConsentScope> requestedScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext,
            Instant timestamp) {

        Map<String, Object> event = new HashMap<>();
        event.put("event_type", eventType);
        event.put("outcome", outcome);
        event.put("tenant_id", tenantId.toString());
        event.put("policy_name", decision.getPolicyName());
        event.put("policy_version", decision.getPolicyVersion());
        event.put("decision_reason", decision.getReason());
        event.put("user_id", accessContext.getUserId().toString());
        event.put("user_name", accessContext.getUserName());
        event.put("user_roles", accessContext.getRoleStrings());
        event.put("ip_address", accessContext.getIpAddress());
        event.put("session_id", accessContext.getSessionId());
        event.put("user_agent", accessContext.getUserAgent());
        event.put("access_reason", accessContext.getAccessReason());
        event.put("requested_scopes", requestedScopes != null ?
                requestedScopes.stream().map(Enum::name).toList() : List.of());
        event.put("clearance_valid", clearance != null && clearance.isValid());
        event.put("clearance_id", clearance != null ? clearance.clearanceId().toString() : null);
        event.put("clearance_granted_by", clearance != null ? clearance.grantedBy() : null);
        event.put("clearance_justification", clearance != null ? clearance.justification() : null);
        event.put("clearance_expires_at", clearance != null ? clearance.expiresAt().toString() : null);
        event.put("timestamp", timestamp.toString());
        event.put("environment", environmentName);
        event.put("severity", outcome.equals("DENIED") ? "HIGH" : "MEDIUM");
        event.putAll(decision.getMetadata());

        return event;
    }

    /**
     * Log to dedicated security audit logger
     * Picked up by log aggregators (Splunk, ELK, etc.)
     */
    private void logToSecurityAudit(Map<String, Object> event) {
        // Structured JSON logging for SIEM ingestion
        securityLogger.info("SECURITY_EVENT: {}", formatAsJson(event));
    }

    /**
     * Log to Splunk HEC (HTTP Event Collector)
     * TODO: Integrate with actual Splunk HEC endpoint
     */
    private void logToSplunk(Map<String, Object> event) {
        // Production implementation would use Splunk HEC client
        logger.debug("Would send to Splunk: {}", event);
    }

    /**
     * Log to Azure Application Insights
     * TODO: Integrate with Application Insights SDK
     */
    private void logToAppInsights(Map<String, Object> event) {
        // Production implementation would use App Insights SDK
        logger.debug("Would send to Application Insights: {}", event);
    }

    /**
     * Send security alert for high-severity events
     * TODO: Integrate with alerting system (PagerDuty, email, Slack, etc.)
     */
    private void sendSecurityAlert(Map<String, Object> event) {
        logger.warn("SECURITY ALERT: {}", event);
        // Production implementation would send alerts via configured channels
    }

    /**
     * Increment monitoring counter
     * TODO: Integrate with metrics system (Prometheus, CloudWatch, etc.)
     */
    private void incrementCounter(String metricName, String... tags) {
        logger.debug("Metric: {} [{}]", metricName, String.join(", ", tags));
        // Production implementation would use metrics library
    }

    /**
     * Format event as JSON for structured logging
     */
    private String formatAsJson(Map<String, Object> event) {
        // Simple JSON formatting - production would use Jackson or similar
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : event.entrySet()) {
            if (!first) json.append(", ");
            json.append("\"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof Collection) {
                json.append("[");
                Collection<?> coll = (Collection<?>) entry.getValue();
                boolean firstItem = true;
                for (Object item : coll) {
                    if (!firstItem) json.append(", ");
                    json.append("\"").append(item).append("\"");
                    firstItem = false;
                }
                json.append("]");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}
