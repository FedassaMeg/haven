package org.haven.shared.reporting;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Value object representing a field mapping from Haven domain model to HUD specifications
 * Provides traceability between internal fields and HUD reporting elements
 */
public class ReportingFieldMapping {

    private final UUID mappingId;
    private final String sourceField;
    private final String sourceEntity;
    private final String targetHudElementId;
    private final String hudSpecificationType;
    private final String targetDataType;
    private final String transformExpression;
    private final String transformLanguage;
    private final boolean vawaSensitiveField;
    private final String vawaSuppressionBehavior;
    private final LocalDate effectiveFrom;
    private final LocalDate effectiveTo;
    private final String hudNoticeReference;
    private final Character requiredFlag;
    private final String csvFieldName;

    public ReportingFieldMapping(
            UUID mappingId,
            String sourceField,
            String sourceEntity,
            String targetHudElementId,
            String hudSpecificationType,
            String targetDataType,
            String transformExpression,
            String transformLanguage,
            boolean vawaSensitiveField,
            String vawaSuppressionBehavior,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String hudNoticeReference,
            Character requiredFlag,
            String csvFieldName) {
        this.mappingId = mappingId;
        this.sourceField = sourceField;
        this.sourceEntity = sourceEntity;
        this.targetHudElementId = targetHudElementId;
        this.hudSpecificationType = hudSpecificationType;
        this.targetDataType = targetDataType;
        this.transformExpression = transformExpression;
        this.transformLanguage = transformLanguage;
        this.vawaSensitiveField = vawaSensitiveField;
        this.vawaSuppressionBehavior = vawaSuppressionBehavior;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.hudNoticeReference = hudNoticeReference;
        this.requiredFlag = requiredFlag;
        this.csvFieldName = csvFieldName;
    }

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

    public String getHudSpecificationType() {
        return hudSpecificationType;
    }

    public String getTargetDataType() {
        return targetDataType;
    }

    public String getTransformExpression() {
        return transformExpression;
    }

    public String getTransformLanguage() {
        return transformLanguage;
    }

    public boolean isVawaSensitiveField() {
        return vawaSensitiveField;
    }

    public String getVawaSuppressionBehavior() {
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

    public Character getRequiredFlag() {
        return requiredFlag;
    }

    public String getCsvFieldName() {
        return csvFieldName;
    }

    public Integer getCsvFieldOrder() {
        // For now, return null. This should be added to the constructor when field ordering is implemented
        return null;
    }

    public boolean isActiveOn(LocalDate date) {
        boolean afterStart = !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return afterStart && beforeEnd;
    }

    public boolean isCurrentlyActive() {
        return isActiveOn(LocalDate.now());
    }
}
