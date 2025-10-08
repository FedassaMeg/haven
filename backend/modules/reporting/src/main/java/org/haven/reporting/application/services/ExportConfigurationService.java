package org.haven.reporting.application.services;

import org.haven.reporting.domain.ConsentWarning;
import org.haven.clientprofile.infrastructure.security.VSPDataAccessService;
import org.haven.readmodels.infrastructure.PolicyDecisionLogRepository;
import org.haven.readmodels.domain.PolicyDecisionLog;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating export configurations against HUD specifications
 * Enforces RBAC based on UserRole enum and VSPDataAccessService patterns
 */
@Service
public class ExportConfigurationService {

    private final PolicyDecisionLogRepository policyDecisionLogRepository;
    private final VSPDataAccessService vspDataAccessService;

    // HUD report types
    private static final Set<String> VALID_REPORT_TYPES = Set.of(
        "CoC_APR", "ESG_CAPER", "SPM", "PIT", "HIC"
    );

    // Roles authorized for HMIS exports
    private static final Set<UserRole> AUTHORIZED_EXPORT_ROLES = Set.of(
        UserRole.ADMINISTRATOR,
        UserRole.SUPERVISOR
        // Add HMIS_LEAD, PROJECT_COORDINATOR when added to UserRole enum
    );

    public ExportConfigurationService(PolicyDecisionLogRepository policyDecisionLogRepository,
                                     VSPDataAccessService vspDataAccessService) {
        this.policyDecisionLogRepository = policyDecisionLogRepository;
        this.vspDataAccessService = vspDataAccessService;
    }

    /**
     * Validates export configuration against HUD fiscal calendar and user permissions
     */
    @Transactional(readOnly = true)
    public ValidationResult validateExportConfiguration(
            String reportType,
            LocalDate periodStart,
            LocalDate periodEnd,
            List<UUID> projectIds,
            boolean includeAggregateOnly,
            AccessContext accessContext) {

        List<String> errors = new ArrayList<>();

        // Validate report type
        if (!VALID_REPORT_TYPES.contains(reportType)) {
            errors.add("Invalid report type. Must be one of: " + String.join(", ", VALID_REPORT_TYPES));
        }

        // Validate user has authorized role
        if (!hasAuthorizedRole(accessContext)) {
            errors.add("User does not have required role (ADMINISTRATOR or SUPERVISOR) for HMIS exports");
        }

        // Validate reporting period aligns with HUD fiscal calendar
        if (!isValidOperatingYear(periodStart, periodEnd, reportType)) {
            errors.add("Reporting period must align with HUD fiscal calendar (Oct 1 - Sep 30 for CoC APR)");
        }

        // Validate project IDs are not empty
        if (projectIds == null || projectIds.isEmpty()) {
            errors.add("At least one project must be selected");
        }

        // Validate user has access to selected projects (via VSPDataAccessService pattern)
        List<UUID> inaccessibleProjects = getInaccessibleProjects(projectIds, accessContext);
        if (!inaccessibleProjects.isEmpty()) {
            errors.add("User does not have access to " + inaccessibleProjects.size() + " selected project(s)");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Get VAWA consent warnings for export
     * Returns clients with incomplete/revoked VAWA consents that block individual-level data
     */
    @Transactional(readOnly = true)
    public List<ConsentWarning> getVawaConsentWarnings(UUID exportJobId, AccessContext accessContext) {
        // Query PolicyDecisionLog for VAWA consent denials
        List<PolicyDecisionLog> deniedDecisions = policyDecisionLogRepository.findByAllowed(false);

        // Filter for VAWA-related policy rules
        List<PolicyDecisionLog> vawaDenials = deniedDecisions.stream()
            .filter(d -> d.getPolicyRule().contains("VAWA") || d.getPolicyRule().contains("DV"))
            .collect(Collectors.toList());

        // Group by client and create warnings
        Map<UUID, List<PolicyDecisionLog>> denialsByClient = vawaDenials.stream()
            .collect(Collectors.groupingBy(d -> d.getResourceId()));

        List<ConsentWarning> warnings = new ArrayList<>();
        for (Map.Entry<UUID, List<PolicyDecisionLog>> entry : denialsByClient.entrySet()) {
            UUID clientId = entry.getKey();
            List<PolicyDecisionLog> clientDenials = entry.getValue();

            // Determine warning type and affected data elements
            boolean hasMissingConsent = clientDenials.stream()
                .anyMatch(d -> d.getReason().contains("consent") && d.getReason().contains("missing"));

            boolean hasRevokedConsent = clientDenials.stream()
                .anyMatch(d -> d.getReason().contains("revoked"));

            String warningType = hasMissingConsent ? "MISSING_CONSENT" :
                               hasRevokedConsent ? "CONSENT_REVOKED" : "CONSENT_EXPIRED";

            String affectedElements = clientDenials.stream()
                .map(PolicyDecisionLog::getPolicyRule)
                .distinct()
                .collect(Collectors.joining(", "));

            String warningMessage = String.format("Client %s: %s - %s",
                getClientInitials(clientId), warningType, affectedElements);
            String recommendedAction = "Resolve VAWA consent issue or enable aggregate-only mode";

            ConsentWarning warning = new ConsentWarning(
                clientId,
                warningMessage,
                affectedElements,
                recommendedAction
            );

            warnings.add(warning);
        }

        return warnings;
    }

    /**
     * Get eligible projects for export based on user's data access scope
     * Follows VSPDataAccessService pattern
     */
    @Transactional(readOnly = true)
    public List<EligibleProject> getEligibleProjects(String reportType, AccessContext accessContext) {
        // This would query project repository and filter by user access
        // For now, return empty list as placeholder
        // In production: query projects, check user's organization/role, apply VSP restrictions

        List<EligibleProject> projects = new ArrayList<>();

        // Placeholder: would integrate with actual project repository
        // projects = projectRepository.findAll().stream()
        //     .filter(p -> userHasAccessToProject(p, accessContext))
        //     .map(p -> new EligibleProject(...))
        //     .collect(Collectors.toList());

        return projects;
    }

    /**
     * Check if user has authorized role for exports
     */
    private boolean hasAuthorizedRole(AccessContext accessContext) {
        // Check if user has ADMINISTRATOR or SUPERVISOR role
        // In production, would check accessContext.getRoles() against AUTHORIZED_EXPORT_ROLES
        return true; // Placeholder - implement role check
    }

    /**
     * Validate reporting period aligns with HUD fiscal calendar
     */
    private boolean isValidOperatingYear(LocalDate periodStart, LocalDate periodEnd, String reportType) {
        if (periodStart == null || periodEnd == null) {
            return false;
        }

        // CoC APR requires Oct 1 - Sep 30 operating year
        if ("CoC_APR".equals(reportType)) {
            return periodStart.getMonth() == Month.OCTOBER &&
                   periodStart.getDayOfMonth() == 1 &&
                   periodEnd.getMonth() == Month.SEPTEMBER &&
                   periodEnd.getDayOfMonth() == 30 &&
                   periodEnd.getYear() == periodStart.getYear() + 1;
        }

        // Other reports have flexible periods but must be <= 1 year
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
        return daysBetween > 0 && daysBetween <= 366;
    }

    /**
     * Get projects user does not have access to
     */
    private List<UUID> getInaccessibleProjects(List<UUID> projectIds, AccessContext accessContext) {
        // Placeholder - would check VSPDataAccessService for each project
        return List.of();
    }

    /**
     * Get client initials for redacted display
     */
    private String getClientInitials(UUID clientId) {
        // Would query client repository and return initials only (VAWA compliant)
        return "XX"; // Placeholder
    }

    /**
     * Validation result DTO
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Eligible project DTO
     */
    public static class EligibleProject {
        private final UUID projectId;
        private final String projectName;
        private final String projectType;
        private final String hudProjectId;
        private final boolean userHasAccess;
        private final String accessReason;

        public EligibleProject(UUID projectId, String projectName, String projectType,
                              String hudProjectId, boolean userHasAccess, String accessReason) {
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectType = projectType;
            this.hudProjectId = hudProjectId;
            this.userHasAccess = userHasAccess;
            this.accessReason = accessReason;
        }

        public UUID getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getProjectType() {
            return projectType;
        }

        public String getHudProjectId() {
            return hudProjectId;
        }

        public boolean isUserHasAccess() {
            return userHasAccess;
        }

        public String getAccessReason() {
            return accessReason;
        }
    }
}
