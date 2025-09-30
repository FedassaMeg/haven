package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.shared.vo.hmis.DvRedactionFlag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Set;

/**
 * Domain service for determining redaction rules for Intake PSDE data
 * Implements VAWA confidentiality requirements and role-based access control logic
 * Note: This service works with domain objects only - DTO redaction is handled in API layer
 */
@Service
public class IntakePsdeDomainRedactionService {

    private final IntakePsdeAuditLogger auditLogger;

    public IntakePsdeDomainRedactionService(IntakePsdeAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Apply appropriate redaction to domain record based on user role and data sensitivity
     */
    public IntakePsdeRecord applyRedaction(IntakePsdeRecord record) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // No authentication - redact everything sensitive
            auditLogger.logRedactionApplied(record.getRecordId().toString(), "UNAUTHENTICATED", "FULL_REDACTION");
            return createRedactedDomainRecord(record);
        }

        Set<String> userRoles = extractRoles(auth);
        String userId = auth.getName();

        // Determine redaction level based on user roles and data sensitivity
        RedactionLevel redactionLevel = determineRedactionLevel(userRoles, record.getDvRedactionLevel());

        // Apply redaction and log
        IntakePsdeRecord redactedRecord = switch (redactionLevel) {
            case NO_REDACTION -> {
                auditLogger.logDataAccess(record.getRecordId().toString(), userId, "FULL_ACCESS");
                yield record;
            }
            case PARTIAL_DV_REDACTION -> {
                auditLogger.logRedactionApplied(record.getRecordId().toString(), userId, "PARTIAL_DV_REDACTION");
                yield createPartiallyRedactedDomainRecord(record);
            }
            case FULL_DV_REDACTION -> {
                auditLogger.logRedactionApplied(record.getRecordId().toString(), userId, "FULL_DV_REDACTION");
                yield createRedactedDomainRecord(record);
            }
        };

        return redactedRecord;
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
     * Apply redaction to domain record for service layer operations
     */
    public IntakePsdeRecord applyDomainRedaction(IntakePsdeRecord record) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return createRedactedDomainRecord(record);
        }

        Set<String> userRoles = extractRoles(auth);
        RedactionLevel redactionLevel = determineRedactionLevel(userRoles, record.getDvRedactionLevel());

        return switch (redactionLevel) {
            case NO_REDACTION -> record;
            case PARTIAL_DV_REDACTION -> createPartiallyRedactedDomainRecord(record);
            case FULL_DV_REDACTION -> createRedactedDomainRecord(record);
        };
    }

    /**
     * Create fully redacted domain record
     */
    private IntakePsdeRecord createRedactedDomainRecord(IntakePsdeRecord original) {
        IntakePsdeRecord redacted = new IntakePsdeRecord();
        // Copy non-sensitive fields
        copyNonSensitiveFields(original, redacted);
        // Set DV fields to "data not collected"
        redacted.updateDomesticViolenceInformation(
            org.haven.shared.vo.hmis.DomesticViolence.DATA_NOT_COLLECTED,
            org.haven.shared.vo.hmis.DomesticViolenceRecency.DATA_NOT_COLLECTED,
            org.haven.shared.vo.hmis.HmisFivePoint.DATA_NOT_COLLECTED,
            DvRedactionFlag.FULL_REDACTION_REQUIRED,
            false
        );
        return redacted;
    }

    /**
     * Create partially redacted domain record (basic DV status visible)
     */
    private IntakePsdeRecord createPartiallyRedactedDomainRecord(IntakePsdeRecord original) {
        IntakePsdeRecord redacted = new IntakePsdeRecord();
        copyNonSensitiveFields(original, redacted);
        // Keep basic DV status, redact details
        redacted.updateDomesticViolenceInformation(
            original.getDomesticViolence(), // Keep basic yes/no
            org.haven.shared.vo.hmis.DomesticViolenceRecency.DATA_NOT_COLLECTED, // Redact recency
            org.haven.shared.vo.hmis.HmisFivePoint.DATA_NOT_COLLECTED, // Redact fleeing status
            DvRedactionFlag.REDACT_FOR_GENERAL_STAFF,
            false // Don't show confidentiality request status
        );
        return redacted;
    }

    /**
     * Copy non-sensitive fields between records
     */
    private void copyNonSensitiveFields(IntakePsdeRecord source, IntakePsdeRecord target) {
        // Income information (generally not sensitive)
        target.updateIncomeInformation(
            source.getTotalMonthlyIncome(),
            source.getIncomeFromAnySource(),
            null, // Don't copy imputation flags for security
            null
        );

        // Health insurance (may be sensitive if VAWA-protected)
        if (source.getHasVawaProtectedHealthInfo() != null && !source.getHasVawaProtectedHealthInfo()) {
            target.updateHealthInsurance(
                source.getCoveredByHealthInsurance(),
                null, // Redact reason for privacy
                false
            );
        }

        // Disability information (may be sensitive if VAWA-related)
        if (source.getHasDisabilityRelatedVawaInfo() != null && !source.getHasDisabilityRelatedVawaInfo()) {
            target.updateDisabilityInformation(
                source.getPhysicalDisability(),
                source.getDevelopmentalDisability(),
                source.getChronicHealthCondition(),
                source.getHivAids(),
                source.getMentalHealthDisorder(),
                source.getSubstanceUseDisorder(),
                false
            );
        }

        // RRH move-in information (generally not sensitive)
        target.updateRrhMoveInInformation(
            source.getResidentialMoveInDate(),
            source.getMoveInType(),
            source.getIsSubsidizedByRrh()
        );
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