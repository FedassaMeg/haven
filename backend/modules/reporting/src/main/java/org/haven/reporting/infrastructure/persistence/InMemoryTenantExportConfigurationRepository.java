package org.haven.reporting.infrastructure.persistence;

import org.haven.reporting.domain.TenantExportConfiguration;
import org.haven.reporting.domain.TenantExportConfigurationRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of tenant export configuration repository
 * TODO: Replace with JPA/database implementation for production
 */
@Repository
public class InMemoryTenantExportConfigurationRepository implements TenantExportConfigurationRepository {

    private final Map<UUID, TenantExportConfiguration> configurations = new ConcurrentHashMap<>();

    @Override
    public Optional<TenantExportConfiguration> findByTenantId(UUID tenantId) {
        return Optional.ofNullable(configurations.get(tenantId));
    }

    @Override
    public void save(TenantExportConfiguration configuration) {
        configurations.put(configuration.getTenantId(), configuration);
    }

    @Override
    public void delete(UUID tenantId) {
        configurations.remove(tenantId);
    }
}
