package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.ClientRepository;
import org.haven.shared.vo.HumanName;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter that bridges the domain ClientRepository interface with JPA
 * Handles proper Hibernate session management for event-sourced aggregates
 */
@Repository
public class ClientRepositoryAdapter implements ClientRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final JpaClientRepository jpaRepository;

    public ClientRepositoryAdapter(JpaClientRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(Client client) {
        JpaClientEntity entity = JpaClientEntity.fromDomain(client);

        // Check if entity exists in the persistence context or database
        JpaClientEntity existingEntity = entityManager.find(JpaClientEntity.class, entity.getId());

        if (existingEntity != null) {
            // Entity exists - update it to avoid DuplicateKeyException
            updateEntityFromDomain(existingEntity, entity);
            // No need to call merge/persist - JPA will auto-detect changes
            entityManager.flush();
        } else {
            // New entity - persist it
            entityManager.persist(entity);
            entityManager.flush();
        }
    }

    @Override
    public Optional<Client> findById(ClientId id) {
        return jpaRepository.findById(id.value())
                .map(JpaClientEntity::toDomain);
    }

    @Override
    public List<Client> findByName(HumanName name) {
        return jpaRepository.findByFirstNameAndLastName(name.getFirstName(), name.getLastName())
                .stream()
                .map(JpaClientEntity::toDomain)
                .toList();
    }

    @Override
    public List<Client> findByNameContaining(String nameFragment) {
        return jpaRepository.findByFirstNameContainingOrLastNameContaining(nameFragment, nameFragment)
                .stream()
                .map(JpaClientEntity::toDomain)
                .toList();
    }

    @Override
    public List<Client> findActiveClients() {
        return jpaRepository.findAll()
                .stream()
                .map(JpaClientEntity::toDomain)
                .filter(Client::isActive)
                .toList();
    }

    @Override
    public Optional<Client> findByExternalId(String externalId) {
        // Not implemented yet - would need external_id column
        return Optional.empty();
    }

    @Override
    public List<Client> findByCreatedAtBetween(Instant startDate, Instant endDate) {
        // For now, filter in memory - in production would add query method
        return jpaRepository.findAll()
                .stream()
                .map(JpaClientEntity::toDomain)
                .filter(c -> c.getCreatedAt() != null &&
                           !c.getCreatedAt().isBefore(startDate) &&
                           !c.getCreatedAt().isAfter(endDate))
                .toList();
    }

    @Override
    public void delete(Client client) {
        jpaRepository.deleteById(client.getId().value());
    }

    @Override
    public ClientId nextId() {
        return ClientId.generate();
    }

    private void updateEntityFromDomain(JpaClientEntity target, JpaClientEntity source) {
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setCreatedAt(source.getCreatedAt());
        target.setAliasName(source.getAliasName());
        target.setDataSystem(source.getDataSystem());
        target.setHmisClientKey(source.getHmisClientKey());
        target.setSafeAtHomeParticipant(source.getSafeAtHomeParticipant());
        target.setIsConfidentialLocation(source.getIsConfidentialLocation());
        target.setSubstituteAddressLine1(source.getSubstituteAddressLine1());
        target.setSubstituteAddressLine2(source.getSubstituteAddressLine2());
        target.setSubstituteCity(source.getSubstituteCity());
        target.setSubstituteState(source.getSubstituteState());
        target.setSubstitutePostalCode(source.getSubstitutePostalCode());
        target.setSubstituteCountry(source.getSubstituteCountry());
        target.setOkToText(source.getOkToText());
        target.setOkToVoicemail(source.getOkToVoicemail());
        target.setContactCodeWord(source.getContactCodeWord());
        target.setQuietHoursStart(source.getQuietHoursStart());
        target.setQuietHoursEnd(source.getQuietHoursEnd());
    }
}
