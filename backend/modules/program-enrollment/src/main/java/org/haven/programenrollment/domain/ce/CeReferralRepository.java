package org.haven.programenrollment.domain.ce;

import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CE Referral domain entities.
 */
public interface CeReferralRepository {

    CeReferral save(CeReferral referral);

    Optional<CeReferral> findById(UUID referralId);

    List<CeReferral> findByEnrollmentId(ProgramEnrollmentId enrollmentId);

    List<CeReferral> findByStatus(CeReferralStatus status);

    List<CeReferral> findExpiredReferrals();

    List<CeReferral> findByProjectId(UUID projectId);

    void delete(UUID referralId);

    long countByStatus(CeReferralStatus status);

    List<CeReferral> findPendingReferralsOlderThan(int days);
}