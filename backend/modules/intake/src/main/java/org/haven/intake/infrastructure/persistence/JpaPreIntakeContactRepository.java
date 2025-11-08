package org.haven.intake.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaPreIntakeContactRepository extends JpaRepository<JpaPreIntakeContactEntity, UUID> {

    /**
     * Find all expired contacts
     */
    @Query("SELECT p FROM JpaPreIntakeContactEntity p WHERE p.expired = true OR p.expiresAt < CURRENT_TIMESTAMP")
    List<JpaPreIntakeContactEntity> findExpired();

    /**
     * Find contacts expiring before threshold
     */
    @Query("SELECT p FROM JpaPreIntakeContactEntity p WHERE p.expiresAt < :threshold AND p.promoted = false AND p.expired = false")
    List<JpaPreIntakeContactEntity> findExpiringBefore(@Param("threshold") Instant threshold);

    /**
     * Find by intake worker
     */
    List<JpaPreIntakeContactEntity> findByIntakeWorkerName(String intakeWorkerName);

    /**
     * Find by alias containing (case-insensitive)
     */
    List<JpaPreIntakeContactEntity> findByClientAliasContainingIgnoreCase(String alias);

    /**
     * Find non-promoted, non-expired contacts for a worker
     */
    @Query("SELECT p FROM JpaPreIntakeContactEntity p WHERE p.intakeWorkerName = :worker AND p.promoted = false AND p.expired = false")
    List<JpaPreIntakeContactEntity> findActiveByWorker(@Param("worker") String worker);
}
