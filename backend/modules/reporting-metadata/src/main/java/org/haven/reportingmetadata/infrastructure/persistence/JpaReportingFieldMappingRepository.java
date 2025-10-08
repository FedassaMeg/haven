package org.haven.reportingmetadata.infrastructure.persistence;

import org.haven.reportingmetadata.domain.HudSpecificationType;
import org.haven.reportingmetadata.domain.ReportingFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for ReportingFieldMapping
 * Provides query methods for schema mapping lookups
 */
@Repository
public interface JpaReportingFieldMappingRepository extends JpaRepository<ReportingFieldMapping, UUID> {

    /**
     * Find all mappings for a specific HUD specification type
     */
    List<ReportingFieldMapping> findByHudSpecificationType(HudSpecificationType type);

    /**
     * Find mappings active on a specific date
     */
    @Query("SELECT m FROM ReportingFieldMapping m WHERE m.effectiveFrom <= :date AND (m.effectiveTo IS NULL OR m.effectiveTo >= :date)")
    List<ReportingFieldMapping> findActiveOn(@Param("date") LocalDate date);

    /**
     * Find currently active mappings
     */
    @Query("SELECT m FROM ReportingFieldMapping m WHERE m.effectiveFrom <= CURRENT_DATE AND (m.effectiveTo IS NULL OR m.effectiveTo >= CURRENT_DATE)")
    List<ReportingFieldMapping> findCurrentlyActive();

    /**
     * Find active mappings for specific specification type
     */
    @Query("SELECT m FROM ReportingFieldMapping m WHERE m.hudSpecificationType = :type AND m.effectiveFrom <= CURRENT_DATE AND (m.effectiveTo IS NULL OR m.effectiveTo >= CURRENT_DATE)")
    List<ReportingFieldMapping> findActiveBySpecType(@Param("type") HudSpecificationType type);

    /**
     * Find VAWA-sensitive mappings
     */
    List<ReportingFieldMapping> findByVawaSensitiveFieldTrue();

    /**
     * Find mappings by source entity
     */
    List<ReportingFieldMapping> findBySourceEntity(String sourceEntity);

    /**
     * Find mapping by source field
     */
    List<ReportingFieldMapping> findBySourceField(String sourceField);

    /**
     * Find mappings by target HUD element
     */
    List<ReportingFieldMapping> findByTargetHudElementId(String targetHudElementId);
}
