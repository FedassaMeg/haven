package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.PolicyDecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for policy decision audit queries
 * Supports compliance reporting and access reviews
 */
@Repository
public interface PolicyDecisionLogRepository extends JpaRepository<PolicyDecisionLog, UUID> {

    /**
     * Find all decisions for a specific user
     */
    List<PolicyDecisionLog> findByUserIdOrderByDecidedAtDesc(UUID userId);

    /**
     * Find all decisions for a specific resource
     */
    List<PolicyDecisionLog> findByResourceIdAndResourceTypeOrderByDecidedAtDesc(
            UUID resourceId, String resourceType);

    /**
     * Find all denied access attempts
     */
    List<PolicyDecisionLog> findByAllowedFalseOrderByDecidedAtDesc();

    /**
     * Find all decisions by allowed flag
     */
    List<PolicyDecisionLog> findByAllowed(boolean allowed);

    /**
     * Find denied access attempts for a specific user
     */
    List<PolicyDecisionLog> findByUserIdAndAllowedFalseOrderByDecidedAtDesc(UUID userId);

    /**
     * Find decisions by policy rule
     */
    List<PolicyDecisionLog> findByPolicyRuleOrderByDecidedAtDesc(String policyRule);

    /**
     * Find decisions within time range
     */
    @Query("SELECT p FROM PolicyDecisionLog p WHERE p.decidedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY p.decidedAt DESC")
    List<PolicyDecisionLog> findByTimeRange(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find denied access attempts within time range
     */
    @Query("SELECT p FROM PolicyDecisionLog p WHERE p.allowed = false " +
           "AND p.decidedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY p.decidedAt DESC")
    List<PolicyDecisionLog> findDeniedAccessInTimeRange(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find all access decisions for a specific resource by specific user
     */
    @Query("SELECT p FROM PolicyDecisionLog p WHERE p.userId = :userId " +
           "AND p.resourceId = :resourceId AND p.resourceType = :resourceType " +
           "ORDER BY p.decidedAt DESC")
    List<PolicyDecisionLog> findUserAccessToResource(
            @Param("userId") UUID userId,
            @Param("resourceId") UUID resourceId,
            @Param("resourceType") String resourceType);

    /**
     * Count denied access attempts by user
     */
    @Query("SELECT COUNT(p) FROM PolicyDecisionLog p WHERE p.userId = :userId AND p.allowed = false")
    long countDeniedAccessByUser(@Param("userId") UUID userId);

    /**
     * Get access patterns for compliance review
     */
    @Query("SELECT p FROM PolicyDecisionLog p WHERE p.policyRule IN :policyRules " +
           "AND p.decidedAt >= :since ORDER BY p.decidedAt DESC")
    List<PolicyDecisionLog> findByPolicyRulesAndSince(
            @Param("policyRules") List<String> policyRules,
            @Param("since") Instant since);
}
