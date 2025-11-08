package org.haven.intake.domain;

import org.haven.shared.domain.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PreIntakeContact aggregate
 */
public interface PreIntakeContactRepository extends Repository<PreIntakeContact, PreIntakeContactId> {

    /**
     * Save or update a pre-intake contact
     */
    void save(PreIntakeContact contact);

    /**
     * Find by ID
     */
    Optional<PreIntakeContact> findById(PreIntakeContactId id);

    /**
     * Find all expired contacts (for cleanup job)
     */
    List<PreIntakeContact> findExpired();

    /**
     * Find contacts expiring soon (for notification)
     */
    List<PreIntakeContact> findExpiringBefore(Instant threshold);

    /**
     * Find contacts by intake worker
     */
    List<PreIntakeContact> findByIntakeWorker(String intakeWorkerName);

    /**
     * Find contacts by alias (partial match)
     */
    List<PreIntakeContact> findByAliasContaining(String alias);

    /**
     * Delete a pre-intake contact
     */
    void delete(PreIntakeContact contact);

    /**
     * Delete by ID
     */
    void deleteById(PreIntakeContactId id);

    /**
     * Generate next ID
     */
    PreIntakeContactId nextId();
}
