package org.haven.api.enrollments.services;

import org.haven.api.enrollments.dto.IntakePsdeResponse;
import org.haven.programenrollment.application.services.IntakePsdeAuditLogger;
import org.haven.shared.vo.hmis.DvRedactionFlag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Set;

/**
 * API-layer service for applying role-based redaction to Intake PSDE DTOs
 * Implements VAWA confidentiality requirements and role-based access control
 */
@Service
public class IntakePsdeRedactionService {

    private final IntakePsdeAuditLogger auditLogger;

    public IntakePsdeRedactionService(IntakePsdeAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Apply appropriate redaction to PSDE response based on user role and data sensitivity
     */
    public IntakePsdeResponse applyRedaction(IntakePsdeResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // No authentication - redact everything sensitive
            auditLogger.logRedactionApplied(response.recordId(), "UNAUTHENTICATED", "FULL_REDACTION");
            return response.withRedactedDvInformation();
        }

        Set<String> userRoles = extractRoles(auth);
        String userId = auth.getName();

        // Determine redaction level based on user roles and data sensitivity
        RedactionLevel redactionLevel = determineRedactionLevel(userRoles, response.dvRedactionLevel());

        // Apply redaction and log
        IntakePsdeResponse redactedResponse = switch (redactionLevel) {
            case NO_REDACTION -> {
                auditLogger.logDataAccess(response.recordId(), userId, "FULL_ACCESS");
                yield response;
            }
            case PARTIAL_DV_REDACTION -> {
                auditLogger.logRedactionApplied(response.recordId(), userId, "PARTIAL_DV_REDACTION");
                yield response.withPartialDvRedaction();
            }
            case FULL_DV_REDACTION -> {
                auditLogger.logRedactionApplied(response.recordId(), userId, "FULL_DV_REDACTION");
                yield response.withRedactedDvInformation();
            }
        };

        return redactedResponse;
    }

    /**
     * Check if user can access full PSDE record
     */
    public boolean canAccessFullRecord(String userRole, DvRedactionFlag dataRedactionLevel) {
        if (dataRedactionLevel == null) {
            return true;
        }

        return !dataRedactionLevel.blocksAccessForRole(userRole);
    }

    /**
     * Check if user can access any DV data
     */
    public boolean canAccessDvData(Set<String> userRoles) {
        return userRoles.stream().anyMatch(role ->
            role.contains("DV_SPECIALIST") ||
            role.contains("ADMIN") ||
            role.contains("CASE_MANAGER") ||
            role.contains("SAFETY_COORDINATOR"));
    }

    /**
     * Check if user can access sensitive DV details
     */
    public boolean canAccessSensitiveDvData(Set<String> userRoles) {
        return userRoles.stream().anyMatch(role ->
            role.contains("DV_SPECIALIST") ||
            role.contains("ADMIN") ||
            role.contains("SAFETY_COORDINATOR"));
    }

    /**
     * Check if user has administrative override capabilities
     */
    public boolean hasAdministrativeOverride(Set<String> userRoles) {
        return userRoles.stream().anyMatch(role ->
            role.contains("ADMIN") ||
            role.contains("SYSTEM_ADMINISTRATOR") ||
            role.contains("DATA_MANAGER"));
    }

    /**
     * Determine the appropriate redaction level for a user and data combination
     */
    private RedactionLevel determineRedactionLevel(Set<String> userRoles, DvRedactionFlag dataRedactionLevel) {
        // Admin override - full access unless victim specifically requested confidentiality
        if (hasAdministrativeOverride(userRoles)) {
            if (dataRedactionLevel == DvRedactionFlag.VICTIM_REQUESTED_CONFIDENTIALITY) {
                return RedactionLevel.FULL_DV_REDACTION;
            }
            return RedactionLevel.NO_REDACTION;
        }

        // No DV access roles - full redaction
        if (!canAccessDvData(userRoles)) {
            return RedactionLevel.FULL_DV_REDACTION;
        }

        // Apply data-specific redaction rules
        if (dataRedactionLevel != null) {
            return switch (dataRedactionLevel) {
                case NO_REDACTION -> RedactionLevel.NO_REDACTION;
                case REDACT_FOR_GENERAL_STAFF -> {
                    if (canAccessSensitiveDvData(userRoles)) {
                        yield RedactionLevel.NO_REDACTION;
                    } else {
                        yield RedactionLevel.PARTIAL_DV_REDACTION;
                    }
                }
                case REDACT_FOR_NON_DV_SPECIALISTS -> {
                    if (userRoles.contains("DV_SPECIALIST") || hasAdministrativeOverride(userRoles)) {
                        yield RedactionLevel.NO_REDACTION;
                    } else {
                        yield RedactionLevel.PARTIAL_DV_REDACTION;
                    }
                }
                case FULL_REDACTION_REQUIRED, VICTIM_REQUESTED_CONFIDENTIALITY ->
                    RedactionLevel.FULL_DV_REDACTION;
            };
        }

        return RedactionLevel.NO_REDACTION;
    }

    /**
     * Extract user roles from authentication context
     */
    private Set<String> extractRoles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(role -> role.startsWith("ROLE_"))
            .map(role -> role.substring(5)) // Remove "ROLE_" prefix
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Redaction levels for internal processing
     */
    private enum RedactionLevel {
        NO_REDACTION,
        PARTIAL_DV_REDACTION,
        FULL_DV_REDACTION
    }
}