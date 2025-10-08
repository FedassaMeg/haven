package org.haven.reportingmetadata.infrastructure.persistence;

import org.haven.reportingmetadata.domain.TransformationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for TransformationRule
 */
@Repository
public interface JpaTransformationRuleRepository extends JpaRepository<TransformationRule, UUID> {

    /**
     * Find rule by unique name
     */
    Optional<TransformationRule> findByRuleName(String ruleName);

    /**
     * Find rules by category
     */
    List<TransformationRule> findByCategory(String category);

    /**
     * Find VAWA-relevant transformation rules
     */
    List<TransformationRule> findByVawaRelevantTrue();
}
