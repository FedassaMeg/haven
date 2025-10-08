package org.haven.reporting.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ExportJob aggregate
 * Implementations use event store pattern for persistence
 */
public interface ExportJobRepository {

    /**
     * Save export job aggregate (appends new events to event store)
     */
    void save(ExportJobAggregate aggregate);

    /**
     * Find export job by ID (reconstructs from event history)
     */
    Optional<ExportJobAggregate> findById(UUID exportJobId);

    /**
     * Check if export job exists
     */
    boolean exists(UUID exportJobId);

    /**
     * Find all export jobs by user ID
     */
    List<ExportJobAggregate> findByRequestedByUserId(String userId);
}
