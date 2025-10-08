package org.haven.reporting.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tenant export configuration
 */
public interface TenantExportConfigurationRepository {

    /**
     * Find configuration by tenant ID
     */
    Optional<TenantExportConfiguration> findByTenantId(UUID tenantId);

    /**
     * Save or update configuration
     */
    void save(TenantExportConfiguration configuration);

    /**
     * Delete configuration
     */
    void delete(UUID tenantId);
}
