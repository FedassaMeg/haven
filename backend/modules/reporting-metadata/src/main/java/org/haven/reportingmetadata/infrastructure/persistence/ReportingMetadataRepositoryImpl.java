package org.haven.reportingmetadata.infrastructure.persistence;

import org.haven.shared.reporting.ReportingMetadataRepository;
import org.haven.shared.reporting.ReportingFieldMapping;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ReportingMetadataRepository backed by JPA
 * Adapts domain entity to shared-kernel value object
 */
@Repository
public class ReportingMetadataRepositoryImpl implements ReportingMetadataRepository {

    private final JpaReportingFieldMappingRepository jpaRepository;

    public ReportingMetadataRepositoryImpl(JpaReportingFieldMappingRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ReportingFieldMapping> findByHudSpecificationType(String hudSpecificationType) {
        try {
            org.haven.reportingmetadata.domain.HudSpecificationType type =
                    org.haven.reportingmetadata.domain.HudSpecificationType.valueOf(hudSpecificationType);
            return jpaRepository.findByHudSpecificationType(type)
                    .stream()
                    .map(this::toValueObject)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public List<ReportingFieldMapping> findActiveOn(LocalDate date) {
        return jpaRepository.findActiveOn(date)
                .stream()
                .map(this::toValueObject)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportingFieldMapping> findCurrentlyActive() {
        return jpaRepository.findCurrentlyActive()
                .stream()
                .map(this::toValueObject)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportingFieldMapping> findActiveBySpecType(String type, LocalDate asOfDate) {
        try {
            org.haven.reportingmetadata.domain.HudSpecificationType hudType =
                    org.haven.reportingmetadata.domain.HudSpecificationType.valueOf(type);
            return jpaRepository.findActiveBySpecType(hudType)
                    .stream()
                    .filter(m -> m.isActiveOn(asOfDate))
                    .map(this::toValueObject)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public List<ReportingFieldMapping> findVawaSensitiveMappings() {
        return jpaRepository.findByVawaSensitiveFieldTrue()
                .stream()
                .map(this::toValueObject)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportingFieldMapping> findBySourceEntity(String sourceEntity) {
        return jpaRepository.findBySourceEntity(sourceEntity)
                .stream()
                .map(this::toValueObject)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportingFieldMapping> findBySourceField(String sourceField) {
        return jpaRepository.findBySourceField(sourceField)
                .stream()
                .map(this::toValueObject)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportingFieldMapping> findByTargetHudElementId(String targetHudElementId) {
        return jpaRepository.findByTargetHudElementId(targetHudElementId)
                .stream()
                .map(this::toValueObject)
                .collect(Collectors.toList());
    }

    @Override
    public ReportingFieldMapping save(ReportingFieldMapping mapping) {
        org.haven.reportingmetadata.domain.ReportingFieldMapping entity = toEntity(mapping);
        org.haven.reportingmetadata.domain.ReportingFieldMapping saved = jpaRepository.save(entity);
        return toValueObject(saved);
    }

    @Override
    public ReportingFieldMapping findById(UUID mappingId) {
        return jpaRepository.findById(mappingId)
                .map(this::toValueObject)
                .orElse(null);
    }

    /**
     * Convert JPA entity to shared-kernel value object
     */
    private ReportingFieldMapping toValueObject(org.haven.reportingmetadata.domain.ReportingFieldMapping entity) {
        return new ReportingFieldMapping(
                entity.getMappingId(),
                entity.getSourceField(),
                entity.getSourceEntity(),
                entity.getTargetHudElementId(),
                entity.getHudSpecificationType().name(),
                entity.getTargetDataType(),
                entity.getTransformExpression(),
                entity.getTransformLanguage().name(),
                entity.isVawaSensitiveField(),
                entity.getVawaSuppressionBehavior() != null ?
                        entity.getVawaSuppressionBehavior().name() : null,
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getHudNoticeReference(),
                entity.getRequiredFlag(),
                entity.getCsvFieldName()
        );
    }

    /**
     * Convert shared-kernel value object to JPA entity
     */
    private org.haven.reportingmetadata.domain.ReportingFieldMapping toEntity(ReportingFieldMapping vo) {
        return org.haven.reportingmetadata.domain.ReportingFieldMapping.builder()
                .sourceField(vo.getSourceField())
                .sourceEntity(vo.getSourceEntity())
                .targetHudElementId(vo.getTargetHudElementId())
                .hudSpecificationType(org.haven.reportingmetadata.domain.HudSpecificationType.valueOf(
                        vo.getHudSpecificationType()))
                .targetDataType(vo.getTargetDataType())
                .transformExpression(vo.getTransformExpression())
                .transformLanguage(org.haven.reportingmetadata.domain.TransformLanguage.valueOf(
                        vo.getTransformLanguage()))
                .vawaSensitiveField(vo.isVawaSensitiveField())
                .vawaSuppressionBehavior(vo.getVawaSuppressionBehavior() != null ?
                        org.haven.reportingmetadata.domain.VawaSuppressionBehavior.valueOf(
                                vo.getVawaSuppressionBehavior()) : null)
                .effectiveFrom(vo.getEffectiveFrom())
                .effectiveTo(vo.getEffectiveTo())
                .hudNoticeReference(vo.getHudNoticeReference())
                .requiredFlag(vo.getRequiredFlag())
                .csvFieldName(vo.getCsvFieldName())
                .build();
    }
}
