package org.haven.programenrollment.infrastructure.persistence;

import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.CeReferral;
import org.haven.programenrollment.domain.ce.CeReferralRepository;
import org.haven.programenrollment.domain.ce.CeReferralStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of CeReferralRepository using JPA.
 */
@Repository
public class CeReferralRepositoryImpl implements CeReferralRepository {

    private final JpaCeReferralSpringRepository repository;

    public CeReferralRepositoryImpl(JpaCeReferralSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public CeReferral save(CeReferral referral) {
        JpaCeReferralEntity entity = new JpaCeReferralEntity(referral);
        JpaCeReferralEntity persisted = repository.save(entity);
        return persisted.toDomain();
    }

    @Override
    public Optional<CeReferral> findById(UUID referralId) {
        return repository.findById(referralId).map(JpaCeReferralEntity::toDomain);
    }

    @Override
    public List<CeReferral> findByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        return repository.findByEnrollmentId(enrollmentId.value()).stream()
            .map(JpaCeReferralEntity::toDomain)
            .toList();
    }

    @Override
    public List<CeReferral> findByStatus(CeReferralStatus status) {
        return repository.findByStatus(status).stream()
            .map(JpaCeReferralEntity::toDomain)
            .toList();
    }

    @Override
    public List<CeReferral> findExpiredReferrals() {
        return repository.findExpiredReferrals().stream()
            .map(JpaCeReferralEntity::toDomain)
            .toList();
    }

    @Override
    public List<CeReferral> findByProjectId(UUID projectId) {
        return repository.findByReferredProjectId(projectId).stream()
            .map(JpaCeReferralEntity::toDomain)
            .toList();
    }

    @Override
    public void delete(UUID referralId) {
        repository.deleteById(referralId);
    }

    @Override
    public long countByStatus(CeReferralStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    public List<CeReferral> findPendingReferralsOlderThan(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return repository.findPendingReferralsOlderThan(cutoffDate).stream()
            .map(JpaCeReferralEntity::toDomain)
            .toList();
    }
}