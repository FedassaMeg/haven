package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.ConfidentialityGuardrails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ConfidentialityGuardrailsRepositoryImpl implements ConfidentialityGuardrailsRepository {
    
    private final JpaConfidentialityGuardrailsRepository jpaRepository;
    
    @Autowired
    public ConfidentialityGuardrailsRepositoryImpl(JpaConfidentialityGuardrailsRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Optional<ConfidentialityGuardrails> findByClientId(UUID clientId) {
        return jpaRepository.findByClientId(clientId)
                .map(JpaConfidentialityGuardrailsEntity::toDomain);
    }
    
    @Override
    public void save(ConfidentialityGuardrails guardrails) {
        Optional<JpaConfidentialityGuardrailsEntity> existing = jpaRepository.findByClientId(guardrails.getClientId());
        
        if (existing.isPresent()) {
            existing.get().updateFrom(guardrails);
            jpaRepository.save(existing.get());
        } else {
            JpaConfidentialityGuardrailsEntity entity = new JpaConfidentialityGuardrailsEntity(guardrails);
            jpaRepository.save(entity);
        }
    }
    
    @Override
    public void delete(UUID clientId) {
        jpaRepository.deleteById(clientId);
    }
    
    @Override
    public List<ConfidentialityGuardrails> findSafeAtHomeClients() {
        return jpaRepository.findSafeAtHomeClients().stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfidentialityGuardrails> findComparableDbOnlyClients() {
        return jpaRepository.findComparableDbOnlyClients().stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfidentialityGuardrails> findConfidentialLocationClients() {
        return jpaRepository.findConfidentialLocationClients().stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfidentialityGuardrails> findRestrictedDataClients() {
        return jpaRepository.findRestrictedDataClients().stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfidentialityGuardrails> findByVisibilityLevel(ConfidentialityGuardrails.VisibilityLevel level) {
        return jpaRepository.findByVisibilityLevel(level).stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfidentialityGuardrails> findByDataSystem(String system) {
        return jpaRepository.findByDataSystem(system).stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfidentialityGuardrails> findAllWithRestrictions() {
        return jpaRepository.findAllWithRestrictions().stream()
                .map(JpaConfidentialityGuardrailsEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Long countSafeAtHomeClients() {
        return jpaRepository.countSafeAtHomeClients();
    }
    
    @Override
    public Long countComparableDbOnlyClients() {
        return jpaRepository.countComparableDbOnlyClients();
    }
}