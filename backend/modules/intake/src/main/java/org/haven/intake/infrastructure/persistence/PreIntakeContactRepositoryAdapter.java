package org.haven.intake.infrastructure.persistence;

import org.haven.intake.domain.PreIntakeContact;
import org.haven.intake.domain.PreIntakeContactId;
import org.haven.intake.domain.PreIntakeContactRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter that bridges the domain PreIntakeContactRepository interface with JPA
 */
@Repository
public class PreIntakeContactRepositoryAdapter implements PreIntakeContactRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final JpaPreIntakeContactRepository jpaRepository;

    public PreIntakeContactRepositoryAdapter(JpaPreIntakeContactRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(PreIntakeContact contact) {
        JpaPreIntakeContactEntity entity = JpaPreIntakeContactEntity.fromDomain(contact);

        // Check if entity exists in the persistence context or database
        JpaPreIntakeContactEntity existingEntity = entityManager.find(JpaPreIntakeContactEntity.class, entity.getId());

        if (existingEntity != null) {
            // Entity exists - update it
            updateEntityFromDomain(existingEntity, entity);
            entityManager.flush();
        } else {
            // New entity - persist it
            entityManager.persist(entity);
            entityManager.flush();
        }

        // Clear pending events
        contact.clearPendingEvents();
    }

    @Override
    public Optional<PreIntakeContact> findById(PreIntakeContactId id) {
        return jpaRepository.findById(id.value())
                .map(JpaPreIntakeContactEntity::toDomain);
    }

    @Override
    public List<PreIntakeContact> findExpired() {
        return jpaRepository.findExpired()
                .stream()
                .map(JpaPreIntakeContactEntity::toDomain)
                .toList();
    }

    @Override
    public List<PreIntakeContact> findExpiringBefore(Instant threshold) {
        return jpaRepository.findExpiringBefore(threshold)
                .stream()
                .map(JpaPreIntakeContactEntity::toDomain)
                .toList();
    }

    @Override
    public List<PreIntakeContact> findByIntakeWorker(String intakeWorkerName) {
        return jpaRepository.findActiveByWorker(intakeWorkerName)
                .stream()
                .map(JpaPreIntakeContactEntity::toDomain)
                .toList();
    }

    @Override
    public List<PreIntakeContact> findByAliasContaining(String alias) {
        return jpaRepository.findByClientAliasContainingIgnoreCase(alias)
                .stream()
                .map(JpaPreIntakeContactEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(PreIntakeContact contact) {
        jpaRepository.deleteById(contact.getId().value());
    }

    @Override
    @Transactional
    public void deleteById(PreIntakeContactId id) {
        jpaRepository.deleteById(id.value());
    }

    @Override
    public PreIntakeContactId nextId() {
        return PreIntakeContactId.generate();
    }

    private void updateEntityFromDomain(JpaPreIntakeContactEntity target, JpaPreIntakeContactEntity source) {
        target.setClientAlias(source.getClientAlias());
        target.setContactDate(source.getContactDate());
        target.setReferralSource(source.getReferralSource());
        target.setIntakeWorkerName(source.getIntakeWorkerName());
        target.setWorkflowData(source.getWorkflowData());
        target.setCurrentStep(source.getCurrentStep());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setExpired(source.isExpired());
        target.setPromoted(source.isPromoted());
        target.setPromotedClientId(source.getPromotedClientId());
    }
}
