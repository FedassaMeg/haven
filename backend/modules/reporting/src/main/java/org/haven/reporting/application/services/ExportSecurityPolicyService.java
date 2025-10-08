package org.haven.reporting.application.services;

import org.haven.reporting.domain.*;
import org.haven.shared.audit.AuditService;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.PolicyDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Security policy enforcement for export hash behavior
 * Implements fail-fast validation with comprehensive audit logging
 *
 * Decision flow:
 * 1. Check tenant hash behavior policy
 * 2. Validate consent scopes if unhashed requested
 * 3. Verify security clearance validity
 * 4. Audit all decisions (permit and deny)
 * 5. Return PolicyDecision with detailed reasoning
 */
@Service
public class ExportSecurityPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(ExportSecurityPolicyService.class);

    private final AuditService auditService;
    private final TenantExportConfigurationRepository configRepository;
    private final ExportSecurityMonitoringService monitoringService;

    public ExportSecurityPolicyService(
            AuditService auditService,
            TenantExportConfigurationRepository configRepository,
            ExportSecurityMonitoringService monitoringService) {
        this.auditService = auditService;
        this.configRepository = configRepository;
        this.monitoringService = monitoringService;
    }

    /**
     * Evaluate export hash policy and return decision
     *
     * @param tenantId Organization/tenant identifier
     * @param requestHashedExport true if requesting hashed PII, false for unhashed
     * @param consentScopes Consent scopes provided by requestor
     * @param clearance Security clearance for unhashed access
     * @param accessContext Request context with user identity and metadata
     * @return PolicyDecision with permit/deny and detailed reasoning
     */
    public PolicyDecision evaluateExportHashPolicy(
            UUID tenantId,
            boolean requestHashedExport,
            Set<ExportConsentScope> consentScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext) {

        Instant evaluationStart = Instant.now();
        logger.info("Evaluating export hash policy for tenant {} by user {} (hashed={})",
                tenantId, accessContext.getUserName(), requestHashedExport);

        // Load tenant configuration
        TenantExportConfiguration config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    logger.warn("No export configuration found for tenant {}, using defaults", tenantId);
                    return TenantExportConfiguration.defaultConfiguration(tenantId, "Unknown");
                });

        // If requesting hashed export, always allow (most secure path)
        if (requestHashedExport) {
            PolicyDecision decision = permitHashedExport(config, accessContext, evaluationStart);
            auditDecision(decision, config, consentScopes, clearance, accessContext);
            return decision;
        }

        // Requesting unhashed export - apply policy enforcement
        PolicyDecision decision = evaluateUnhashedRequest(
                config, consentScopes, clearance, accessContext, evaluationStart);

        auditDecision(decision, config, consentScopes, clearance, accessContext);

        // Log to monitoring system for all unhashed attempts
        monitoringService.logUnhashedExportAttempt(
                tenantId, decision, consentScopes, clearance, accessContext);

        return decision;
    }

    /**
     * Permit hashed export (always safe)
     */
    private PolicyDecision permitHashedExport(
            TenantExportConfiguration config,
            AccessContext accessContext,
            Instant evaluationStart) {

        return PolicyDecision.permit(
            "EXPORT_HASH_POLICY",
            "v1.0",
            "Hashed export requested - complies with all security policies",
            Map.of(
                "tenant_id", config.getTenantId().toString(),
                "organization", config.getOrganizationName(),
                "hash_behavior", config.getHashBehavior().toString(),
                "user_id", accessContext.getUserId().toString(),
                "user_name", accessContext.getUserName(),
                "evaluation_time_ms", Instant.now().toEpochMilli() - evaluationStart.toEpochMilli()
            ),
            accessContext
        );
    }

    /**
     * Evaluate unhashed export request against policy
     */
    private PolicyDecision evaluateUnhashedRequest(
            TenantExportConfiguration config,
            Set<ExportConsentScope> consentScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext,
            Instant evaluationStart) {

        // Check tenant policy allows unhashed
        if (config.getHashBehavior().prohibitsUnhashed()) {
            return denyUnhashedExport(
                config,
                "POLICY_PROHIBITS_UNHASHED",
                "Organization policy prohibits unhashed exports (ALWAYS_HASH mode)",
                consentScopes,
                clearance,
                accessContext,
                evaluationStart
            );
        }

        // If tenant allows unhashed by default, permit
        if (config.getHashBehavior().allowsUnhashedByDefault()) {
            logger.warn("Tenant {} allows unhashed by default - consider tightening security",
                    config.getTenantId());
            return permitUnhashedExport(config, consentScopes, clearance, accessContext, evaluationStart);
        }

        // Consent-based policy - validate requirements
        if (consentScopes == null || consentScopes.isEmpty()) {
            return denyUnhashedExport(
                config,
                "MISSING_CONSENT_SCOPES",
                "No consent scopes provided for unhashed export request",
                consentScopes,
                clearance,
                accessContext,
                evaluationStart
            );
        }

        // Validate all required scopes are present
        Set<ExportConsentScope> requiredScopes = config.getRequiredScopesForUnhashed();
        if (!consentScopes.containsAll(requiredScopes)) {
            Set<ExportConsentScope> missing = new HashSet<>(requiredScopes);
            missing.removeAll(consentScopes);
            return denyUnhashedExport(
                config,
                "INSUFFICIENT_CONSENT_SCOPES",
                "Missing required consent scopes: " + missing,
                consentScopes,
                clearance,
                accessContext,
                evaluationStart
            );
        }

        // Validate security clearance
        if (clearance == null) {
            return denyUnhashedExport(
                config,
                "MISSING_CLEARANCE",
                "No security clearance provided for unhashed export",
                consentScopes,
                clearance,
                accessContext,
                evaluationStart
            );
        }

        if (clearance.isExpired()) {
            return denyUnhashedExport(
                config,
                "CLEARANCE_EXPIRED",
                "Security clearance expired at " + clearance.expiresAt(),
                consentScopes,
                clearance,
                accessContext,
                evaluationStart
            );
        }

        if (!clearance.authorizesUnhashedExports()) {
            return denyUnhashedExport(
                config,
                "CLEARANCE_INSUFFICIENT",
                "Security clearance does not authorize unhashed exports",
                consentScopes,
                clearance,
                accessContext,
                evaluationStart
            );
        }

        // All checks passed - permit unhashed export
        return permitUnhashedExport(config, consentScopes, clearance, accessContext, evaluationStart);
    }

    /**
     * Create permit decision for unhashed export
     */
    private PolicyDecision permitUnhashedExport(
            TenantExportConfiguration config,
            Set<ExportConsentScope> consentScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext,
            Instant evaluationStart) {

        logger.info("UNHASHED EXPORT PERMITTED for user {} in tenant {}",
                accessContext.getUserName(), config.getTenantId());

        return PolicyDecision.permit(
            "EXPORT_HASH_POLICY",
            "v1.0",
            "Unhashed export authorized with valid consent and clearance",
            buildDecisionMetadata(config, consentScopes, clearance, accessContext, evaluationStart),
            accessContext
        );
    }

    /**
     * Create deny decision for unhashed export
     */
    private PolicyDecision denyUnhashedExport(
            TenantExportConfiguration config,
            String errorCode,
            String reason,
            Set<ExportConsentScope> consentScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext,
            Instant evaluationStart) {

        logger.warn("UNHASHED EXPORT DENIED for user {} in tenant {}: {} - {}",
                accessContext.getUserName(), config.getTenantId(), errorCode, reason);

        Map<String, Object> metadata = buildDecisionMetadata(
                config, consentScopes, clearance, accessContext, evaluationStart);
        metadata.put("error_code", errorCode);

        return PolicyDecision.deny(
            "EXPORT_HASH_POLICY",
            "v1.0",
            reason,
            metadata,
            accessContext
        );
    }

    /**
     * Build metadata for decision audit
     */
    private Map<String, Object> buildDecisionMetadata(
            TenantExportConfiguration config,
            Set<ExportConsentScope> consentScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext,
            Instant evaluationStart) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tenant_id", config.getTenantId().toString());
        metadata.put("organization", config.getOrganizationName());
        metadata.put("hash_behavior", config.getHashBehavior().toString());
        metadata.put("user_id", accessContext.getUserId().toString());
        metadata.put("user_name", accessContext.getUserName());
        metadata.put("user_roles", accessContext.getRoleStrings());
        metadata.put("ip_address", accessContext.getIpAddress());
        metadata.put("session_id", accessContext.getSessionId());
        metadata.put("consent_scopes", consentScopes != null ?
                consentScopes.stream().map(Enum::name).toList() : List.of());
        metadata.put("clearance_valid", clearance != null && clearance.isValid());
        metadata.put("clearance_id", clearance != null ? clearance.clearanceId().toString() : null);
        metadata.put("clearance_expires", clearance != null ? clearance.expiresAt().toString() : null);
        metadata.put("evaluation_time_ms",
                Instant.now().toEpochMilli() - evaluationStart.toEpochMilli());
        metadata.put("timestamp", Instant.now().toString());

        return metadata;
    }

    /**
     * Audit policy decision with full context
     */
    private void auditDecision(
            PolicyDecision decision,
            TenantExportConfiguration config,
            Set<ExportConsentScope> consentScopes,
            ExportSecurityClearance clearance,
            AccessContext accessContext) {

        String action = decision.isPermitted() ? "EXPORT_UNHASHED_PERMITTED" : "EXPORT_UNHASHED_DENIED";

        Map<String, Object> auditMetadata = new HashMap<>(decision.getMetadata());
        auditMetadata.put("decision", decision.isPermitted() ? "PERMIT" : "DENY");
        auditMetadata.put("policy", decision.getPolicyName());
        auditMetadata.put("policy_version", decision.getPolicyVersion());
        auditMetadata.put("reason", decision.getReason());

        auditService.logAction(action, auditMetadata);

        logger.info("Export hash policy decision audited: {} for user {} (decision={})",
                action, accessContext.getUserName(), decision.isPermitted() ? "PERMIT" : "DENY");
    }
}
