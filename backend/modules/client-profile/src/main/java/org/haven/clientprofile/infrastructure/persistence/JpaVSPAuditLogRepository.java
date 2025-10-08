package org.haven.clientprofile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for VSP access audit logs
 */
@Repository
public interface JpaVSPAuditLogRepository extends JpaRepository<VSPAuditLogEntity, Long> {

    /**
     * Find VSP access logs by user ID within time range
     */
    @Query("SELECT v FROM VSPAuditLogEntity v WHERE v.userId = :userId " +
           "AND v.accessTime BETWEEN :startTime AND :endTime ORDER BY v.accessTime DESC")
    List<VSPAuditLogEntity> findByUserIdAndAccessTimeBetween(
        @Param("userId") UUID userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find VSP access logs by client ID within time range
     */
    @Query("SELECT v FROM VSPAuditLogEntity v WHERE v.clientId = :clientId " +
           "AND v.accessTime BETWEEN :startTime AND :endTime ORDER BY v.accessTime DESC")
    List<VSPAuditLogEntity> findByClientIdAndAccessTimeBetween(
        @Param("clientId") UUID clientId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find unauthorized VSP access attempts within time range
     */
    @Query("SELECT v FROM VSPAuditLogEntity v WHERE v.accessGranted = false " +
           "AND v.accessTime BETWEEN :startTime AND :endTime ORDER BY v.accessTime DESC")
    List<VSPAuditLogEntity> findUnauthorizedAccessAttempts(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find VAWA-protected data access logs
     */
    @Query("SELECT v FROM VSPAuditLogEntity v WHERE v.vawaProtected = true " +
           "AND v.accessTime BETWEEN :startTime AND :endTime ORDER BY v.accessTime DESC")
    List<VSPAuditLogEntity> findVawaProtectedAccess(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count VSP access attempts by user
     */
    @Query("SELECT COUNT(v) FROM VSPAuditLogEntity v WHERE v.userId = :userId " +
           "AND v.accessTime BETWEEN :startTime AND :endTime")
    long countByUserIdAndAccessTimeBetween(
        @Param("userId") UUID userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
