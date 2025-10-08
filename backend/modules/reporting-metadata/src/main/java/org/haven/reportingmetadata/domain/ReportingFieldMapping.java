package org.haven.reportingmetadata.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Captures schema mapping from Haven domain model to HUD HMIS CSV, APR, CAPER, SPM, PIT/HIC specifications
 *
 * Provides bidirectional traceability:
 * - Source domain field → Target HUD element
 * - Transformation logic (SQL/Java expressions)
 * - VAWA sensitivity flags for runtime redaction
 * - Effective date ranges tracking HUD spec versions
 *
 * Compliance references:
 * - HUD HMIS Data Standards (HDX 2024)
 * - HUD CSV Format Specifications
 * - 24 CFR 578 (CoC Program)
 * - VAWA Confidentiality Requirements
 */
@Entity
@Table(name = "reporting_field_mapping", indexes = {
    @Index(name = "idx_mapping_source_field", columnList = "source_field"),
    @Index(name = "idx_mapping_target_element", columnList = "target_hud_element_id"),
    @Index(name = "idx_mapping_effective", columnList = "effective_from, effective_to"),
    @Index(name = "idx_mapping_vawa", columnList = "vawa_sensitive_field"),
    @Index(name = "idx_mapping_spec", columnList = "hud_specification_type")
})
public class ReportingFieldMapping {

    @Id
    @GeneratedValue
    private UUID mappingId;

    /**
     * Source field from Haven domain model
     * Format: entity.field or entity.nestedObject.field
     * Examples: "ClientProfile.dateOfBirth", "ServiceEpisode.serviceType", "Enrollment.relationshipToHoH"
     */
    @NotNull
    @Column(nullable = false, length = 200)
    private String sourceField;

    /**
     * Source entity (domain aggregate root)
     * Examples: "ClientProfile", "ServiceEpisode", "Enrollment", "CaseRecord"
     */
    @NotNull
    @Column(nullable = false, length = 100)
    private String sourceEntity;

    /**
     * Target HUD element identifier
     * Format: spec:element or spec:table.field
     * Examples: "CSV:Client.DOB", "APR:Q04a", "CAPER:4.1", "SPM:Metric1a", "PIT:S1.PersonalID"
     */
    @NotNull
    @Column(nullable = false, length = 200)
    private String targetHudElementId;

    /**
     * HUD specification type this mapping applies to
     * Values: HMIS_CSV, CoC_APR, ESG_CAPER, SYSTEM_PERFORMANCE_MEASURES, PIT_HIC
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HudSpecificationType hudSpecificationType;

    /**
     * Data type of target HUD field
     * Values match HUD CSV spec: String, Integer, Date, DateTime, Decimal, Boolean, Code
     */
    @NotNull
    @Column(nullable = false, length = 50)
    private String targetDataType;

    /**
     * Transformation expression applied to source field
     * Supports SQL fragments or Java expression language
     *
     * Examples:
     * - "TIMESTAMPDIFF(YEAR, dateOfBirth, CURDATE())" for age calculation
     * - "CASE WHEN projectType IN (1,2,3,8) THEN 'ES' ELSE 'Other' END" for project type grouping
     * - "COALESCE(race, 8)" for handling null as RaceNone per HUD spec
     * - "IF(dvVictim AND NOT consentGiven, NULL, field)" for VAWA redaction
     *
     * NULL indicates direct field mapping (no transformation)
     */
    @Column(length = 2000)
    private String transformExpression;

    /**
     * Transformation language: SQL, JAVA_EL, NONE
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransformLanguage transformLanguage;

    /**
     * VAWA-sensitive field requiring consent check before export
     * If true, must verify PolicyDecisionLog before including in report
     * Individual-level data suppressed without explicit consent
     */
    @NotNull
    @Column(nullable = false)
    private boolean vawaSensitiveField;

    /**
     * If vawaSensitiveField=true, specifies behavior when consent not given
     * Values: SUPPRESS (exclude from export), AGGREGATE_ONLY (allow in counts), REDACT (show as null/9/99)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VawaSuppressionBehavior vawaSuppressionBehavior;

    /**
     * Effective start date for this mapping
     * Corresponds to HUD specification version effective date
     * Example: 2024-10-01 for HDX 2024 specifications
     */
    @NotNull
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Effective end date (null = currently active)
     * Set when HUD publishes new spec version superseding this mapping
     */
    @Column
    private LocalDate effectiveTo;

    /**
     * HUD notice or specification version reference
     * Examples: "HDX 2024", "Notice CPD-23-08", "HMIS CSV Format Spec v2024"
     */
    @Column(length = 200)
    private String hudNoticeReference;

    /**
     * Justification or additional context for this mapping
     * Links to HUD documentation explaining field purpose/calculation
     */
    @Column(length = 1000)
    private String justification;

    /**
     * Whether field is required in HUD export
     * Per HUD CSV spec: R=required, C=conditionally required, O=optional
     */
    @NotNull
    @Column(nullable = false, length = 1)
    private Character requiredFlag;

    /**
     * Field validation rules from HUD spec
     * Examples: "Must be YYYY-MM-DD", "Integer 1-5", "Code from List 1.7"
     */
    @Column(length = 500)
    private String validationRules;

    /**
     * HUD CSV export field name (for CSV exports)
     * Examples: "PersonalID", "ProjectID", "DateOfBirth"
     */
    @Column(length = 100)
    private String csvFieldName;

    /**
     * Display order for CSV export (null if not applicable)
     */
    @Column
    private Integer csvFieldOrder;

    protected ReportingFieldMapping() {
        // JPA constructor
    }

    private ReportingFieldMapping(Builder builder) {
        this.sourceField = builder.sourceField;
        this.sourceEntity = builder.sourceEntity;
        this.targetHudElementId = builder.targetHudElementId;
        this.hudSpecificationType = builder.hudSpecificationType;
        this.targetDataType = builder.targetDataType;
        this.transformExpression = builder.transformExpression;
        this.transformLanguage = builder.transformLanguage;
        this.vawaSensitiveField = builder.vawaSensitiveField;
        this.vawaSuppressionBehavior = builder.vawaSuppressionBehavior;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
        this.hudNoticeReference = builder.hudNoticeReference;
        this.justification = builder.justification;
        this.requiredFlag = builder.requiredFlag;
        this.validationRules = builder.validationRules;
        this.csvFieldName = builder.csvFieldName;
        this.csvFieldOrder = builder.csvFieldOrder;
    }

    // Getters
    public UUID getMappingId() {
        return mappingId;
    }

    public String getSourceField() {
        return sourceField;
    }

    public String getSourceEntity() {
        return sourceEntity;
    }

    public String getTargetHudElementId() {
        return targetHudElementId;
    }

    public HudSpecificationType getHudSpecificationType() {
        return hudSpecificationType;
    }

    public String getTargetDataType() {
        return targetDataType;
    }

    public String getTransformExpression() {
        return transformExpression;
    }

    public TransformLanguage getTransformLanguage() {
        return transformLanguage;
    }

    public boolean isVawaSensitiveField() {
        return vawaSensitiveField;
    }

    public VawaSuppressionBehavior getVawaSuppressionBehavior() {
        return vawaSuppressionBehavior;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public String getHudNoticeReference() {
        return hudNoticeReference;
    }

    public String getJustification() {
        return justification;
    }

    public Character getRequiredFlag() {
        return requiredFlag;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public String getCsvFieldName() {
        return csvFieldName;
    }

    public Integer getCsvFieldOrder() {
        return csvFieldOrder;
    }

    /**
     * Check if mapping is active on given date
     */
    public boolean isActiveOn(LocalDate date) {
        boolean afterStart = !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return afterStart && beforeEnd;
    }

    /**
     * Check if mapping is currently active
     */
    public boolean isCurrentlyActive() {
        return isActiveOn(LocalDate.now());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceField;
        private String sourceEntity;
        private String targetHudElementId;
        private HudSpecificationType hudSpecificationType;
        private String targetDataType;
        private String transformExpression;
        private TransformLanguage transformLanguage = TransformLanguage.NONE;
        private boolean vawaSensitiveField = false;
        private VawaSuppressionBehavior vawaSuppressionBehavior;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String hudNoticeReference;
        private String justification;
        private Character requiredFlag = 'O';
        private String validationRules;
        private String csvFieldName;
        private Integer csvFieldOrder;

        public Builder sourceField(String sourceField) {
            this.sourceField = sourceField;
            return this;
        }

        public Builder sourceEntity(String sourceEntity) {
            this.sourceEntity = sourceEntity;
            return this;
        }

        public Builder targetHudElementId(String targetHudElementId) {
            this.targetHudElementId = targetHudElementId;
            return this;
        }

        public Builder hudSpecificationType(HudSpecificationType hudSpecificationType) {
            this.hudSpecificationType = hudSpecificationType;
            return this;
        }

        public Builder targetDataType(String targetDataType) {
            this.targetDataType = targetDataType;
            return this;
        }

        public Builder transformExpression(String transformExpression) {
            this.transformExpression = transformExpression;
            return this;
        }

        public Builder transformLanguage(TransformLanguage transformLanguage) {
            this.transformLanguage = transformLanguage;
            return this;
        }

        public Builder vawaSensitiveField(boolean vawaSensitiveField) {
            this.vawaSensitiveField = vawaSensitiveField;
            return this;
        }

        public Builder vawaSuppressionBehavior(VawaSuppressionBehavior vawaSuppressionBehavior) {
            this.vawaSuppressionBehavior = vawaSuppressionBehavior;
            return this;
        }

        public Builder effectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(LocalDate effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public Builder hudNoticeReference(String hudNoticeReference) {
            this.hudNoticeReference = hudNoticeReference;
            return this;
        }

        public Builder justification(String justification) {
            this.justification = justification;
            return this;
        }

        public Builder requiredFlag(Character requiredFlag) {
            this.requiredFlag = requiredFlag;
            return this;
        }

        public Builder validationRules(String validationRules) {
            this.validationRules = validationRules;
            return this;
        }

        public Builder csvFieldName(String csvFieldName) {
            this.csvFieldName = csvFieldName;
            return this;
        }

        public Builder csvFieldOrder(Integer csvFieldOrder) {
            this.csvFieldOrder = csvFieldOrder;
            return this;
        }

        public ReportingFieldMapping build() {
            return new ReportingFieldMapping(this);
        }
    }
}
