package org.haven.reportingmetadata.application.services;

import org.haven.reportingmetadata.domain.*;
import org.haven.reportingmetadata.infrastructure.persistence.*;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.ConfidentialityPolicyService;
import org.haven.shared.security.PolicyDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for reporting metadata operations
 *
 * Integrates with:
 * - ConfidentialityPolicyService for VAWA compliance
 * - PolicyDecisionLog for audit trail
 * - CQRS read models for source data
 */
@Service
@Transactional(readOnly = true)
public class ReportingMetadataService {

    private final JpaReportingFieldMappingRepository fieldMappingRepository;
    private final JpaReportSpecificationRepository specificationRepository;
    private final JpaTransformationRuleRepository transformationRuleRepository;
    private final ConfidentialityPolicyService confidentialityPolicyService;

    public ReportingMetadataService(
            JpaReportingFieldMappingRepository fieldMappingRepository,
            JpaReportSpecificationRepository specificationRepository,
            JpaTransformationRuleRepository transformationRuleRepository,
            ConfidentialityPolicyService confidentialityPolicyService) {
        this.fieldMappingRepository = fieldMappingRepository;
        this.specificationRepository = specificationRepository;
        this.transformationRuleRepository = transformationRuleRepository;
        this.confidentialityPolicyService = confidentialityPolicyService;
    }

    /**
     * Get active field mappings for specific HUD specification type
     */
    public List<ReportingFieldMapping> getActiveMappings(HudSpecificationType type) {
        return fieldMappingRepository.findActiveBySpecType(type);
    }

    /**
     * Get active report specifications by type
     */
    public List<ReportSpecification> getActiveSpecifications(HudSpecificationType type) {
        return specificationRepository.findActiveByType(type);
    }

    /**
     * Get transformation rule by name
     */
    public TransformationRule getTransformationRule(String ruleName) {
        return transformationRuleRepository.findByRuleName(ruleName)
                .orElseThrow(() -> new IllegalArgumentException("Transformation rule not found: " + ruleName));
    }

    /**
     * Apply transformation rule with parameters
     */
    public String applyTransformation(String ruleName, Map<String, String> parameters) {
        TransformationRule rule = getTransformationRule(ruleName);
        return rule.apply(parameters);
    }

    /**
     * Filter field mappings based on VAWA consent
     * Returns only fields permitted for given client and access context
     */
    public List<ReportingFieldMapping> filterForVawaConsent(
            List<ReportingFieldMapping> mappings,
            UUID clientId,
            boolean isDvVictim,
            boolean consentGiven,
            AccessContext context) {

        return mappings.stream()
                .filter(mapping -> {
                    // Non-VAWA-sensitive fields always included
                    if (!mapping.isVawaSensitiveField()) {
                        return true;
                    }

                    // VAWA-sensitive field - check consent and suppression behavior
                    if (!isDvVictim) {
                        // Not a DV victim - no VAWA restriction
                        return true;
                    }

                    if (consentGiven) {
                        // Consent given - include field
                        return true;
                    }

                    // No consent - check suppression behavior
                    VawaSuppressionBehavior behavior = mapping.getVawaSuppressionBehavior();
                    if (behavior == null) {
                        // Default to SUPPRESS if not specified
                        return false;
                    }

                    switch (behavior) {
                        case SUPPRESS:
                            // Exclude field entirely
                            return false;

                        case AGGREGATE_ONLY:
                            // Include only if this is aggregate query (checked elsewhere)
                            // For individual records, suppress
                            return false;

                        case REDACT:
                            // Include field but value will be redacted (handled in transformation)
                            return true;

                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all VAWA-sensitive field mappings
     */
    public List<ReportingFieldMapping> getVawaSensitiveMappings() {
        return fieldMappingRepository.findByVawaSensitiveFieldTrue();
    }

    /**
     * Get mappings for specific source entity
     */
    public List<ReportingFieldMapping> getMappingsForEntity(String sourceEntity) {
        return fieldMappingRepository.findBySourceEntity(sourceEntity);
    }

    /**
     * Initialize default transformation rules
     * Called on application startup to populate common HUD transformations
     */
    @Transactional
    public void initializeDefaultTransformationRules() {
        // Check if rules already exist
        if (transformationRuleRepository.count() > 0) {
            return;
        }

        // Create and save common HUD transformation rules
        List<TransformationRule> defaultRules = List.of(
                TransformationRule.ageAtEnrollment(),
                TransformationRule.ageAtReportDate(),
                TransformationRule.emergencyShelterFilter(),
                TransformationRule.cocFundedProjectFilter(),
                TransformationRule.headOfHouseholdCheck(),
                TransformationRule.raceWithNoneDefault(),
                TransformationRule.genderWithNoneDefault(),
                TransformationRule.lengthOfStay(),
                TransformationRule.vawaRedaction(),
                TransformationRule.projectTypeGrouping()
        );

        transformationRuleRepository.saveAll(defaultRules);
    }

    /**
     * Validate mapping configuration
     * Checks for consistency, completeness, and compliance
     */
    public List<String> validateMappingConfiguration(HudSpecificationType type) {
        List<String> validationErrors = new java.util.ArrayList<>();
        List<ReportingFieldMapping> mappings = getActiveMappings(type);

        // Check for required fields
        long requiredCount = mappings.stream()
                .filter(m -> m.getRequiredFlag() == 'R')
                .count();

        if (requiredCount == 0) {
            validationErrors.add("No required fields defined for " + type);
        }

        // Check VAWA-sensitive fields have suppression behavior defined
        mappings.stream()
                .filter(ReportingFieldMapping::isVawaSensitiveField)
                .filter(m -> m.getVawaSuppressionBehavior() == null)
                .forEach(m -> validationErrors.add(
                        "VAWA-sensitive field missing suppression behavior: " + m.getSourceField()
                ));

        // Check transformation expressions reference valid rules
        mappings.stream()
                .filter(m -> m.getTransformLanguage() == TransformLanguage.SQL)
                .filter(m -> m.getTransformExpression() != null)
                .forEach(m -> {
                    // Basic validation - could be enhanced
                    if (m.getTransformExpression().contains("${") && !m.getTransformExpression().contains("}")) {
                        validationErrors.add("Malformed transformation expression: " + m.getSourceField());
                    }
                });

        return validationErrors;
    }
}
