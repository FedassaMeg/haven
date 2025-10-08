package org.haven.shared.reporting;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for reporting metadata operations
 * Provides access to HUD field mappings versioned by data standards release
 *
 * Implementations backed by JSONB columns storing field mappings
 * Supports versioning by HUD Data Standards release (2024 v1.0, etc.)
 */
public interface ReportingMetadataRepository {

    /**
     * Find all mappings for a specific HUD specification type
     */
    List<ReportingFieldMapping> findByHudSpecificationType(String hudSpecificationType);

    /**
     * Find mappings active on a specific date
     */
    List<ReportingFieldMapping> findActiveOn(LocalDate date);

    /**
     * Find currently active mappings
     */
    List<ReportingFieldMapping> findCurrentlyActive();

    /**
     * Find active mappings for specific specification type
     */
    List<ReportingFieldMapping> findActiveBySpecType(String type, LocalDate asOfDate);

    /**
     * Find VAWA-sensitive mappings
     */
    List<ReportingFieldMapping> findVawaSensitiveMappings();

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

    /**
     * Save or update a field mapping
     */
    ReportingFieldMapping save(ReportingFieldMapping mapping);

    /**
     * Find mapping by ID
     */
    ReportingFieldMapping findById(UUID mappingId);
}
