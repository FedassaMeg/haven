package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.ConfidentialityGuardrails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaConfidentialityGuardrailsRepository extends JpaRepository<JpaConfidentialityGuardrailsEntity, UUID> {
    
    Optional<JpaConfidentialityGuardrailsEntity> findByClientId(UUID clientId);
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE c.isSafeAtHome = true")
    List<JpaConfidentialityGuardrailsEntity> findSafeAtHomeClients();
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE c.isComparableDbOnly = true")
    List<JpaConfidentialityGuardrailsEntity> findComparableDbOnlyClients();
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE c.hasConfidentialLocation = true")
    List<JpaConfidentialityGuardrailsEntity> findConfidentialLocationClients();
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE c.hasRestrictedData = true")
    List<JpaConfidentialityGuardrailsEntity> findRestrictedDataClients();
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE c.visibilityLevel = :level")
    List<JpaConfidentialityGuardrailsEntity> findByVisibilityLevel(@Param("level") ConfidentialityGuardrails.VisibilityLevel level);
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE c.dataSystem = :system")
    List<JpaConfidentialityGuardrailsEntity> findByDataSystem(@Param("system") String system);
    
    @Query("SELECT c FROM JpaConfidentialityGuardrailsEntity c WHERE " +
           "c.isSafeAtHome = true OR c.isComparableDbOnly = true OR " +
           "c.hasConfidentialLocation = true OR c.hasRestrictedData = true")
    List<JpaConfidentialityGuardrailsEntity> findAllWithRestrictions();
    
    @Query("SELECT COUNT(c) FROM JpaConfidentialityGuardrailsEntity c WHERE c.isSafeAtHome = true")
    Long countSafeAtHomeClients();
    
    @Query("SELECT COUNT(c) FROM JpaConfidentialityGuardrailsEntity c WHERE c.isComparableDbOnly = true")
    Long countComparableDbOnlyClients();
}