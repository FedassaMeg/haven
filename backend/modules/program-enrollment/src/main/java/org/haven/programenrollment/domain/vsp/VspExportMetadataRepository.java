package org.haven.programenrollment.domain.vsp;

import org.haven.shared.vo.hmis.VawaRecipientCategory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for VSP export metadata
 */
public interface VspExportMetadataRepository {

    /**
     * Save export metadata
     */
    VspExportMetadata save(VspExportMetadata metadata);

    /**
     * Find export by ID
     */
    Optional<VspExportMetadata> findById(UUID exportId);

    /**
     * Find all exports for a specific recipient
     */
    List<VspExportMetadata> findByRecipient(String recipient);

    /**
     * Find exports by recipient and date range
     */
    List<VspExportMetadata> findByRecipientAndDateRange(
        String recipient,
        Instant startDate,
        Instant endDate
    );

    /**
     * Find exports by VAWA recipient category
     */
    List<VspExportMetadata> findByRecipientCategory(VawaRecipientCategory category);

    /**
     * Find active exports (not revoked or expired)
     */
    List<VspExportMetadata> findActiveExports();

    /**
     * Find exports by packet hash
     */
    List<VspExportMetadata> findByPacketHash(String packetHash);

    /**
     * Find exports expiring soon
     */
    List<VspExportMetadata> findExpiringBefore(LocalDateTime expiryThreshold);

    /**
     * Count exports by recipient in date range
     */
    long countByRecipientAndDateRange(
        String recipient,
        Instant startDate,
        Instant endDate
    );

    /**
     * Find exports with specific consent basis
     */
    List<VspExportMetadata> findByConsentBasis(String consentBasis);

    /**
     * Update export status
     */
    void updateStatus(UUID exportId, VspExportMetadata.ExportStatus status);

    /**
     * Delete expired exports older than retention period
     */
    int deleteExpiredOlderThan(Instant cutoffDate);

    /**
     * Find exports by CE hash key
     */
    List<VspExportMetadata> findByCeHashKey(String ceHashKey);

    /**
     * Get export statistics for a recipient
     */
    ExportStatistics getStatisticsForRecipient(String recipient);

    /**
     * Record for export statistics
     */
    record ExportStatistics(
        long totalExports,
        long activeExports,
        long revokedExports,
        long expiredExports,
        Instant firstExportDate,
        Instant lastExportDate,
        double averageExportsPerMonth
    ) {}
}