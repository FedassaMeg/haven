package org.haven.shared.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuditLogEntity
 */
@Repository
public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Find audit log by audit id
     */
    AuditLogEntity findByAuditId(UUID auditId);

    /**
     * Find audit logs by user id
     */
    List<AuditLogEntity> findByUserId(UUID userId);

    /**
     * Find audit logs by resource id
     */
    List<AuditLogEntity> findByResourceId(UUID resourceId);

    /**
     * Find audit logs by resource ID within time range
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.resourceId = :resourceId " +
           "AND a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findByResourceIdAndTimestampBetween(
        @Param("resourceId") UUID resourceId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find audit logs by user ID within time range
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.userId = :userId " +
           "AND a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findByUserIdAndTimestampBetween(
        @Param("userId") UUID userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find audit logs by action within time range
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.action = :action " +
           "AND a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findByActionAndTimestampBetween(
        @Param("action") String action,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find audit logs by resource type within time range
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.resourceType = :resourceType " +
           "AND a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findByResourceTypeAndTimestampBetween(
        @Param("resourceType") String resourceType,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count audit logs by user ID within time range
     */
    @Query("SELECT COUNT(a) FROM AuditLogEntity a WHERE a.userId = :userId " +
           "AND a.timestamp BETWEEN :startTime AND :endTime")
    long countByUserIdAndTimestampBetween(
        @Param("userId") UUID userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count all audit logs for a user
     */
    long countByUserId(UUID userId);

    /**
     * Find most recent audit logs
     */
    List<AuditLogEntity> findByOrderByTimestampDesc(Pageable pageable);
}
