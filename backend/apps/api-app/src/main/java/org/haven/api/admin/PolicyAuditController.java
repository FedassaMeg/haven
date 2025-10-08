package org.haven.api.admin;

import org.haven.readmodels.domain.PolicyDecisionLog;
import org.haven.readmodels.infrastructure.PolicyDecisionLogRepository;
import org.haven.shared.security.PolicyChangeNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API for policy decision audit trail and compliance reporting
 * Restricted to administrators and compliance officers
 */
@RestController
@RequestMapping("/api/admin/policy-decisions")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'COMPLIANCE_OFFICER', 'SUPERVISOR')")
public class PolicyAuditController {

    private final PolicyDecisionLogRepository policyDecisionLogRepository;
    private final PolicyChangeNotificationService notificationService;

    @Autowired
    public PolicyAuditController(PolicyDecisionLogRepository policyDecisionLogRepository,
                                PolicyChangeNotificationService notificationService) {
        this.policyDecisionLogRepository = policyDecisionLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Get policy decisions within time range
     */
    @GetMapping
    public ResponseEntity<List<PolicyDecisionLog>> getPolicyDecisions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID userId) {

        List<PolicyDecisionLog> decisions;

        if (userId != null) {
            decisions = policyDecisionLogRepository.findByUserIdOrderByDecidedAtDesc(userId);
        } else if (startDate != null && endDate != null) {
            decisions = policyDecisionLogRepository.findByTimeRange(startDate, endDate);
        } else {
            // Default: last 24 hours
            Instant yesterday = Instant.now().minusSeconds(86400);
            decisions = policyDecisionLogRepository.findByTimeRange(yesterday, Instant.now());
        }

        return ResponseEntity.ok(decisions);
    }

    /**
     * Get all denied access attempts
     */
    @GetMapping("/denied")
    public ResponseEntity<List<PolicyDecisionLog>> getDeniedAccess(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        Instant startTime = since != null ? since : Instant.now().minusSeconds(604800); // Default: last week
        List<PolicyDecisionLog> denied = policyDecisionLogRepository.findDeniedAccessInTimeRange(
                startTime, Instant.now());

        return ResponseEntity.ok(denied);
    }

    /**
     * Get policy decisions for specific resource
     */
    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<PolicyDecisionLog>> getResourceAccessHistory(
            @PathVariable UUID resourceId,
            @RequestParam String resourceType) {

        List<PolicyDecisionLog> decisions = policyDecisionLogRepository
                .findByResourceIdAndResourceTypeOrderByDecidedAtDesc(resourceId, resourceType);

        return ResponseEntity.ok(decisions);
    }

    /**
     * Get user's access history
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PolicyDecisionLog>> getUserAccessHistory(
            @PathVariable UUID userId) {

        List<PolicyDecisionLog> decisions = policyDecisionLogRepository
                .findByUserIdOrderByDecidedAtDesc(userId);

        return ResponseEntity.ok(decisions);
    }

    /**
     * Get denied access count by user
     */
    @GetMapping("/user/{userId}/denied-count")
    public ResponseEntity<Long> getUserDeniedAccessCount(@PathVariable UUID userId) {
        long count = policyDecisionLogRepository.countDeniedAccessByUser(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get policy decisions by specific rule
     */
    @GetMapping("/rule/{policyRule}")
    public ResponseEntity<List<PolicyDecisionLog>> getDecisionsByRule(
            @PathVariable String policyRule) {

        List<PolicyDecisionLog> decisions = policyDecisionLogRepository
                .findByPolicyRuleOrderByDecidedAtDesc(policyRule);

        return ResponseEntity.ok(decisions);
    }

    /**
     * Get high-risk policy decisions (VAWA, sealed notes, attorney-client)
     */
    @GetMapping("/high-risk")
    public ResponseEntity<List<PolicyDecisionLog>> getHighRiskDecisions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        Instant startTime = since != null ? since : Instant.now().minusSeconds(604800); // Default: last week
        List<String> highRiskRules = List.of(
                "SEALED_NOTE_RESTRICTION",
                "PRIVILEGED_COUNSELING_ACCESS",
                "VSP_VAWA_RESTRICTION",
                "SCOPE_ATTORNEY_CLIENT"
        );

        List<PolicyDecisionLog> decisions = policyDecisionLogRepository
                .findByPolicyRulesAndSince(highRiskRules, startTime);

        return ResponseEntity.ok(decisions);
    }

    /**
     * Get recent policy notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<PolicyChangeNotificationService.PolicyNotification>> getNotifications(
            @RequestParam(defaultValue = "50") int limit) {

        List<PolicyChangeNotificationService.PolicyNotification> notifications =
                notificationService.getRecentNotifications(limit);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications by severity
     */
    @GetMapping("/notifications/severity/{severity}")
    public ResponseEntity<List<PolicyChangeNotificationService.PolicyNotification>> getNotificationsBySeverity(
            @PathVariable PolicyChangeNotificationService.NotificationSeverity severity) {

        List<PolicyChangeNotificationService.PolicyNotification> notifications =
                notificationService.getNotificationsBySeverity(severity);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get user-to-resource access history
     */
    @GetMapping("/access-history")
    public ResponseEntity<List<PolicyDecisionLog>> getUserResourceAccessHistory(
            @RequestParam UUID userId,
            @RequestParam UUID resourceId,
            @RequestParam String resourceType) {

        List<PolicyDecisionLog> decisions = policyDecisionLogRepository
                .findUserAccessToResource(userId, resourceId, resourceType);

        return ResponseEntity.ok(decisions);
    }
}
