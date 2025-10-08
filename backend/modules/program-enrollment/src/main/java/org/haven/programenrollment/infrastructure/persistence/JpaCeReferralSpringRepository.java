package org.haven.programenrollment.infrastructure.persistence;

import org.haven.programenrollment.domain.ce.CeReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for CE referral entities.
 */
@Repository
public interface JpaCeReferralSpringRepository extends JpaRepository<JpaCeReferralEntity, UUID> {

    List<JpaCeReferralEntity> findByEnrollmentId(UUID enrollmentId);

    List<JpaCeReferralEntity> findByStatus(CeReferralStatus status);

    List<JpaCeReferralEntity> findByReferredProjectId(UUID projectId);

    long countByStatus(CeReferralStatus status);

    @Query("SELECT r FROM JpaCeReferralEntity r WHERE r.expirationDate IS NOT NULL AND r.expirationDate < CURRENT_DATE AND r.status = 'PENDING'")
    List<JpaCeReferralEntity> findExpiredReferrals();

    @Query("SELECT r FROM JpaCeReferralEntity r WHERE r.status = 'PENDING' AND r.referralDate < :cutoffDate")
    List<JpaCeReferralEntity> findPendingReferralsOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}