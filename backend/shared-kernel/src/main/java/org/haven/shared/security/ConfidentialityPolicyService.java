package org.haven.shared.security;

import org.haven.shared.audit.AuditService;
import org.haven.shared.reporting.ReportingMetadataRepository;
import org.haven.shared.reporting.ReportingFieldMapping;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized confidentiality policy enforcement service
 * Consolidates all privacy and access control rules for HAVEN system
 *
 * Enforces:
 * - VAWA (Violence Against Women Act) protections
 * - HUD confidentiality requirements
 * - VSP data sharing restrictions
 * - Role-based access control
 * - Note visibility scopes
 */
@Service
public class ConfidentialityPolicyService {

    private final AuditService auditService;
    private final ReportingMetadataRepository reportingMetadataRepository;

    public ConfidentialityPolicyService(
            AuditService auditService,
            ReportingMetadataRepository reportingMetadataRepository) {
        this.auditService = auditService;
        this.reportingMetadataRepository = reportingMetadataRepository;
    }

    /**
     * Check if user can access a restricted note
     * Centralized policy enforcement replacing embedded logic in RestrictedNote
     */
    public PolicyDecision canAccessNote(
            UUID noteId,
            UUID authorId,
            String noteType,
            String visibilityScope,
            boolean isSealed,
            UUID sealedBy,
            java.util.List<UUID> authorizedViewers,
            AccessContext context
    ) {
        UUID userId = context.getUserId();

        // Rule 1: Sealed notes - only sealer can access
        if (isSealed && !userId.equals(sealedBy)) {
            PolicyDecision decision = PolicyDecision.deny(
                    "Note is sealed by another user",
                    "SEALED_NOTE_RESTRICTION",
                    userId,
                    noteId,
                    "RestrictedNote",
                    buildDecisionContext(context, "isSealed=true", "sealedBy=" + sealedBy)
            );
            auditPolicyDecision(decision, context);
            return decision;
        }

        // Rule 2: Custom authorized viewers list takes precedence
        if (authorizedViewers != null && !authorizedViewers.isEmpty()) {
            if (authorizedViewers.contains(userId)) {
                PolicyDecision decision = PolicyDecision.allow(
                        "User in authorized viewers list",
                        "CUSTOM_AUTHORIZED_VIEWERS",
                        userId,
                        noteId,
                        "RestrictedNote",
                        buildDecisionContext(context, "authorizedViewers.size=" + authorizedViewers.size())
                );
                auditPolicyDecision(decision, context);
                return decision;
            } else {
                PolicyDecision decision = PolicyDecision.deny(
                        "User not in authorized viewers list",
                        "CUSTOM_AUTHORIZED_VIEWERS",
                        userId,
                        noteId,
                        "RestrictedNote",
                        buildDecisionContext(context, "authorizedViewers.size=" + authorizedViewers.size())
                );
                auditPolicyDecision(decision, context);
                return decision;
            }
        }

        // Rule 3: Special handling for PRIVILEGED_COUNSELING note type
        if ("PRIVILEGED_COUNSELING".equals(noteType)) {
            boolean hasAccess = context.hasAnyRole(UserRole.DV_COUNSELOR, UserRole.LICENSED_CLINICIAN)
                             || userId.equals(authorId);

            PolicyDecision decision = hasAccess
                    ? PolicyDecision.allow(
                            "DV counselor, licensed clinician, or note author",
                            "PRIVILEGED_COUNSELING_ACCESS",
                            userId,
                            noteId,
                            "RestrictedNote",
                            buildDecisionContext(context, "noteType=PRIVILEGED_COUNSELING", "authorId=" + authorId)
                    )
                    : PolicyDecision.deny(
                            "Requires DV counselor, licensed clinician role, or be note author",
                            "PRIVILEGED_COUNSELING_ACCESS",
                            userId,
                            noteId,
                            "RestrictedNote",
                            buildDecisionContext(context, "noteType=PRIVILEGED_COUNSELING", "authorId=" + authorId)
                    );

            auditPolicyDecision(decision, context);
            return decision;
        }

        // Rule 4: Visibility scope-based access
        PolicyDecision decision = evaluateVisibilityScope(visibilityScope, userId, authorId, context, noteId);
        auditPolicyDecision(decision, context);
        return decision;
    }

    /**
     * Evaluate visibility scope rules
     */
    private PolicyDecision evaluateVisibilityScope(String scope, UUID userId, UUID authorId,
                                                  AccessContext context, UUID noteId) {
        switch (scope) {
            case "PUBLIC":
                return PolicyDecision.allow(
                        "Public visibility scope",
                        "SCOPE_PUBLIC",
                        userId,
                        noteId,
                        "RestrictedNote",
                        buildDecisionContext(context, "scope=PUBLIC")
                );

            case "CASE_TEAM":
                boolean isCaseTeam = context.hasAnyRole(UserRole.CASE_MANAGER, UserRole.SUPERVISOR);
                return isCaseTeam
                        ? PolicyDecision.allow("User has case team role", "SCOPE_CASE_TEAM", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=CASE_TEAM"))
                        : PolicyDecision.deny("Requires case team role", "SCOPE_CASE_TEAM", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=CASE_TEAM"));

            case "CLINICAL_ONLY":
                boolean isClinical = context.hasAnyRole(
                        UserRole.CLINICIAN, UserRole.THERAPIST, UserRole.COUNSELOR, UserRole.DV_COUNSELOR
                );
                return isClinical
                        ? PolicyDecision.allow("User has clinical role", "SCOPE_CLINICAL_ONLY", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=CLINICAL_ONLY"))
                        : PolicyDecision.deny("Requires clinical role", "SCOPE_CLINICAL_ONLY", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=CLINICAL_ONLY"));

            case "LEGAL_TEAM":
                boolean isLegal = context.hasAnyRole(UserRole.LEGAL_ADVOCATE, UserRole.ATTORNEY);
                return isLegal
                        ? PolicyDecision.allow("User has legal role", "SCOPE_LEGAL_TEAM", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=LEGAL_TEAM"))
                        : PolicyDecision.deny("Requires legal role", "SCOPE_LEGAL_TEAM", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=LEGAL_TEAM"));

            case "SAFETY_TEAM":
                boolean isSafety = context.hasAnyRole(UserRole.SAFETY_SPECIALIST, UserRole.CRISIS_COUNSELOR);
                return isSafety
                        ? PolicyDecision.allow("User has safety role", "SCOPE_SAFETY_TEAM", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=SAFETY_TEAM"))
                        : PolicyDecision.deny("Requires safety role", "SCOPE_SAFETY_TEAM", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=SAFETY_TEAM"));

            case "MEDICAL_TEAM":
                boolean isMedical = context.hasAnyRole(UserRole.NURSE, UserRole.DOCTOR, UserRole.MEDICAL_ADVOCATE);
                return isMedical
                        ? PolicyDecision.allow("User has medical role", "SCOPE_MEDICAL_TEAM", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=MEDICAL_TEAM"))
                        : PolicyDecision.deny("Requires medical role", "SCOPE_MEDICAL_TEAM", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=MEDICAL_TEAM"));

            case "ADMIN_ONLY":
                boolean isAdmin = context.hasAnyRole(UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
                return isAdmin
                        ? PolicyDecision.allow("User has admin role", "SCOPE_ADMIN_ONLY", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=ADMIN_ONLY"))
                        : PolicyDecision.deny("Requires admin role", "SCOPE_ADMIN_ONLY", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=ADMIN_ONLY"));

            case "AUTHOR_ONLY":
                boolean isAuthor = userId.equals(authorId);
                return isAuthor
                        ? PolicyDecision.allow("User is note author", "SCOPE_AUTHOR_ONLY", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=AUTHOR_ONLY", "authorId=" + authorId))
                        : PolicyDecision.deny("Only author can access", "SCOPE_AUTHOR_ONLY", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=AUTHOR_ONLY", "authorId=" + authorId));

            case "ATTORNEY_CLIENT":
                boolean isAttorneyOrAuthor = context.hasRole(UserRole.ATTORNEY) || userId.equals(authorId);
                return isAttorneyOrAuthor
                        ? PolicyDecision.allow("User is attorney or client (author)", "SCOPE_ATTORNEY_CLIENT", userId, noteId,
                                              "RestrictedNote", buildDecisionContext(context, "scope=ATTORNEY_CLIENT", "authorId=" + authorId))
                        : PolicyDecision.deny("Requires attorney or be note author", "SCOPE_ATTORNEY_CLIENT", userId, noteId,
                                            "RestrictedNote", buildDecisionContext(context, "scope=ATTORNEY_CLIENT", "authorId=" + authorId));

            case "CUSTOM":
                return PolicyDecision.deny(
                        "Custom scope requires authorized viewers list",
                        "SCOPE_CUSTOM_NO_VIEWERS",
                        userId,
                        noteId,
                        "RestrictedNote",
                        buildDecisionContext(context, "scope=CUSTOM", "authorizedViewers=null")
                );

            default:
                return PolicyDecision.deny(
                        "Unknown visibility scope: " + scope,
                        "UNKNOWN_SCOPE",
                        userId,
                        noteId,
                        "RestrictedNote",
                        buildDecisionContext(context, "scope=" + scope)
                );
        }
    }

    /**
     * Check if VSP user can access client data
     * Enforces HUD ComparableDB restrictions
     */
    public PolicyDecision canVSPAccessClient(UUID clientId, boolean isDVVictim,
                                            String dataSystem, AccessContext context) {
        if (!context.isExternalPartner()) {
            return PolicyDecision.allow(
                    "Non-VSP user - normal permissions apply",
                    "NON_VSP_ACCESS",
                    context.getUserId(),
                    clientId,
                    "ClientProfile",
                    buildDecisionContext(context, "isVSP=false")
            );
        }

        // VSP cannot access HMIS data for DV victims (VAWA protection)
        if (isDVVictim && "HMIS".equals(dataSystem)) {
            PolicyDecision decision = PolicyDecision.deny(
                    "VSP cannot access HMIS data for DV victims (VAWA protection)",
                    "VSP_VAWA_RESTRICTION",
                    context.getUserId(),
                    clientId,
                    "ClientProfile",
                    buildDecisionContext(context, "isVSP=true", "isDVVictim=true", "dataSystem=HMIS")
            );
            auditPolicyDecision(decision, context);
            return decision;
        }

        // VSP can access ComparableDB data only
        if ("COMPARABLE_DB".equals(dataSystem)) {
            PolicyDecision decision = PolicyDecision.allow(
                    "VSP accessing ComparableDB data (permitted)",
                    "VSP_COMPARABLE_DB_ACCESS",
                    context.getUserId(),
                    clientId,
                    "ClientProfile",
                    buildDecisionContext(context, "isVSP=true", "dataSystem=COMPARABLE_DB")
            );
            auditPolicyDecision(decision, context);
            return decision;
        }

        // Default deny for VSP
        PolicyDecision decision = PolicyDecision.deny(
                "VSP can only access ComparableDB data",
                "VSP_DATA_SYSTEM_RESTRICTION",
                context.getUserId(),
                clientId,
                "ClientProfile",
                buildDecisionContext(context, "isVSP=true", "dataSystem=" + dataSystem)
        );
        auditPolicyDecision(decision, context);
        return decision;
    }

    /**
     * Build decision context string from parameters
     */
    private String buildDecisionContext(AccessContext context, String... additionalContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("user=").append(context.getUserId());
        sb.append(", roles=").append(context.getRoleStrings());
        sb.append(", reason=").append(context.getAccessReason());

        if (additionalContext != null && additionalContext.length > 0) {
            for (String ctx : additionalContext) {
                sb.append(", ").append(ctx);
            }
        }

        return sb.toString();
    }

    /**
     * Audit policy decision for compliance
     */
    private void auditPolicyDecision(PolicyDecision decision, AccessContext context) {
        Map<String, Object> auditMetadata = new HashMap<>();
        auditMetadata.put("decisionId", decision.getDecisionId());
        auditMetadata.put("allowed", decision.isAllowed());
        auditMetadata.put("reason", decision.getReason());
        auditMetadata.put("policyRule", decision.getPolicyRule());
        auditMetadata.put("resourceType", decision.getResourceType());
        auditMetadata.put("resourceId", decision.getResourceId());
        auditMetadata.put("ipAddress", context.getIpAddress());
        auditMetadata.put("sessionId", context.getSessionId());
        auditMetadata.put("userAgent", context.getUserAgent());

        auditService.logAccess(
                decision.getUserId(),
                context.getUserName(),
                decision.getResourceType(),
                decision.getResourceId(),
                decision.isAllowed() ? "POLICY_ALLOW" : "POLICY_DENY",
                decision.getReason(),
                auditMetadata
        );
    }

    /**
     * Check if a specific field is VAWA-protected based on reporting metadata
     * Uses HUD field mapping definitions to determine VAWA sensitivity
     *
     * @param entityName Source entity (e.g., "CurrentLivingSituation", "ServiceEpisode")
     * @param fieldName Field name
     * @return true if field is VAWA-sensitive according to HUD mappings
     */
    public boolean isVawaProtectedField(String entityName, String fieldName) {
        List<ReportingFieldMapping> mappings = reportingMetadataRepository
                .findBySourceEntity(entityName);

        return mappings.stream()
                .filter(m -> m.getSourceField().equals(fieldName) ||
                            m.getSourceField().equals(entityName + "." + fieldName))
                .anyMatch(ReportingFieldMapping::isVawaSensitiveField);
    }

    /**
     * Get all VAWA-protected fields for an entity
     *
     * @param entityName Source entity name
     * @return List of field names that are VAWA-protected
     */
    public List<String> getVawaProtectedFields(String entityName) {
        List<ReportingFieldMapping> mappings = reportingMetadataRepository
                .findBySourceEntity(entityName);

        return mappings.stream()
                .filter(ReportingFieldMapping::isVawaSensitiveField)
                .map(ReportingFieldMapping::getSourceField)
                .collect(Collectors.toList());
    }

    /**
     * Check if CurrentLivingSituation record should be VAWA-protected
     * Protected when linked to DV services or has DV indicators
     *
     * @param currentLivingSituationId ID of the record
     * @param isDvVictim Whether client is DV victim
     * @param hasConsentForSharing Whether client has given consent for data sharing
     * @param context Access context
     * @return Policy decision for accessing CurrentLivingSituation
     */
    public PolicyDecision canAccessCurrentLivingSituation(
            UUID currentLivingSituationId,
            UUID clientId,
            boolean isDvVictim,
            boolean hasConsentForSharing,
            AccessContext context) {

        // Non-DV victims - normal access
        if (!isDvVictim) {
            return PolicyDecision.allow(
                    "Client is not flagged as DV victim",
                    "NON_DV_VICTIM",
                    context.getUserId(),
                    currentLivingSituationId,
                    "CurrentLivingSituation",
                    buildDecisionContext(context, "isDvVictim=false")
            );
        }

        // DV victim - check consent for VAWA compliance
        if (hasConsentForSharing) {
            PolicyDecision decision = PolicyDecision.allow(
                    "DV victim with consent for data sharing",
                    "VAWA_CONSENT_GIVEN",
                    context.getUserId(),
                    currentLivingSituationId,
                    "CurrentLivingSituation",
                    buildDecisionContext(context, "isDvVictim=true", "hasConsent=true")
            );
            auditPolicyDecision(decision, context);
            return decision;
        }

        // DV victim without consent - VAWA protection applies
        PolicyDecision decision = PolicyDecision.deny(
                "VAWA protection: CurrentLivingSituation for DV victim without consent",
                "VAWA_CURRENT_LIVING_SITUATION_RESTRICTION",
                context.getUserId(),
                currentLivingSituationId,
                "CurrentLivingSituation",
                buildDecisionContext(context, "isDvVictim=true", "hasConsent=false")
        );
        auditPolicyDecision(decision, context);
        return decision;
    }

    /**
     * Check if service record should be VAWA-protected
     * Protected when service type is Health & DV (14) or other DV-related services
     *
     * @param serviceId Service episode ID
     * @param serviceType HUD service type code
     * @param isDvVictim Whether client is DV victim
     * @param hasConsentForSharing Whether client has given consent
     * @param context Access context
     * @return Policy decision for accessing service record
     */
    public PolicyDecision canAccessServiceRecord(
            UUID serviceId,
            UUID clientId,
            Integer serviceType,
            boolean isDvVictim,
            boolean hasConsentForSharing,
            AccessContext context) {

        // Check if service type is DV-related (Type 14 = Health & DV services)
        boolean isDvService = serviceType != null && serviceType == 14;

        if (!isDvService && !isDvVictim) {
            return PolicyDecision.allow(
                    "Non-DV service for non-DV client",
                    "NON_DV_SERVICE",
                    context.getUserId(),
                    serviceId,
                    "ServiceEpisode",
                    buildDecisionContext(context, "serviceType=" + serviceType, "isDvService=false")
            );
        }

        // DV-related service or DV victim - check consent
        if (hasConsentForSharing) {
            PolicyDecision decision = PolicyDecision.allow(
                    "DV-related service with consent for data sharing",
                    "VAWA_DV_SERVICE_CONSENT_GIVEN",
                    context.getUserId(),
                    serviceId,
                    "ServiceEpisode",
                    buildDecisionContext(context, "serviceType=" + serviceType,
                                       "isDvService=" + isDvService, "hasConsent=true")
            );
            auditPolicyDecision(decision, context);
            return decision;
        }

        // DV service without consent - VAWA protection applies
        PolicyDecision decision = PolicyDecision.deny(
                "VAWA protection: Health & DV service record without consent (HUD Type 14)",
                "VAWA_DV_SERVICE_RESTRICTION",
                context.getUserId(),
                serviceId,
                "ServiceEpisode",
                buildDecisionContext(context, "serviceType=" + serviceType,
                                   "isDvService=" + isDvService, "hasConsent=false")
        );
        auditPolicyDecision(decision, context);
        return decision;
    }

    /**
     * Get VAWA suppression behavior for a field
     *
     * @param entityName Entity name
     * @param fieldName Field name
     * @return Suppression behavior (SUPPRESS, REDACT, AGGREGATE_ONLY) or null
     */
    public String getVawaSuppressionBehavior(String entityName, String fieldName) {
        List<ReportingFieldMapping> mappings = reportingMetadataRepository
                .findBySourceEntity(entityName);

        return mappings.stream()
                .filter(m -> m.getSourceField().equals(fieldName) ||
                            m.getSourceField().equals(entityName + "." + fieldName))
                .filter(ReportingFieldMapping::isVawaSensitiveField)
                .map(ReportingFieldMapping::getVawaSuppressionBehavior)
                .findFirst()
                .orElse(null);
    }
}
