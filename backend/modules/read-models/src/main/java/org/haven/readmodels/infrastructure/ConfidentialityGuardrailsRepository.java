package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.ConfidentialityGuardrails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfidentialityGuardrailsRepository {
    
    Optional<ConfidentialityGuardrails> findByClientId(UUID clientId);
    
    void save(ConfidentialityGuardrails guardrails);
    
    void delete(UUID clientId);
    
    List<ConfidentialityGuardrails> findSafeAtHomeClients();
    
    List<ConfidentialityGuardrails> findComparableDbOnlyClients();
    
    List<ConfidentialityGuardrails> findConfidentialLocationClients();
    
    List<ConfidentialityGuardrails> findRestrictedDataClients();
    
    List<ConfidentialityGuardrails> findByVisibilityLevel(ConfidentialityGuardrails.VisibilityLevel level);
    
    List<ConfidentialityGuardrails> findByDataSystem(String system);
    
    List<ConfidentialityGuardrails> findAllWithRestrictions();
    
    Long countSafeAtHomeClients();
    
    Long countComparableDbOnlyClients();
}