package org.haven.shared.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AuditLogRepository using JPA
 * Provides durable persistence for audit trail
 */
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final JpaAuditLogRepository jpaRepository;

    public AuditLogRepositoryImpl(JpaAuditLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AuditService.AuditEntry entry) {
        AuditLogEntity entity = AuditLogEntity.fromAuditEntry(entry);
        jpaRepository.save(entity);
    }

    @Override
    public void save(AuditLogEntity entity) {
        jpaRepository.save(entity);
    }

    @Override
    public AuditLogEntity findByAuditId(UUID auditId) {
        return jpaRepository.findByAuditId(auditId);
    }

    @Override
    public List<AuditLogEntity> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLogEntity> findByResourceId(UUID resourceId) {
        return jpaRepository.findByResourceId(resourceId);
    }

    @Override
    public List<AuditService.AuditEntry> findByResourceId(UUID resourceId, Instant startTime, Instant endTime) {
        return jpaRepository.findByResourceIdAndTimestampBetween(resourceId, startTime, endTime)
            .stream()
            .map(this::toAuditEntry)
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditService.AuditEntry> findByUserId(UUID userId, Instant startTime, Instant endTime) {
        return jpaRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime)
            .stream()
            .map(this::toAuditEntry)
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditService.AuditEntry> findByAction(String action, Instant startTime, Instant endTime) {
        return jpaRepository.findByActionAndTimestampBetween(action, startTime, endTime)
            .stream()
            .map(this::toAuditEntry)
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditService.AuditEntry> findByResourceType(String resourceType, Instant startTime, Instant endTime) {
        return jpaRepository.findByResourceTypeAndTimestampBetween(resourceType, startTime, endTime)
            .stream()
            .map(this::toAuditEntry)
            .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(UUID userId, Instant startTime, Instant endTime) {
        return jpaRepository.countByUserIdAndTimestampBetween(userId, startTime, endTime);
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public List<AuditService.AuditEntry> findRecent(int limit) {
        var pageRequest = PageRequest.of(0, limit);
        return jpaRepository.findByOrderByTimestampDesc(pageRequest)
            .stream()
            .map(this::toAuditEntry)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to domain object
     */
    private AuditService.AuditEntry toAuditEntry(AuditLogEntity entity) {
        return new AuditService.AuditEntry(
            entity.getAuditId(),
            entity.getResourceId(),
            entity.getResourceType(),
            entity.getAction(),
            entity.getUserId() != null ? entity.getUserId().toString() : "SYSTEM",
            entity.getTimestamp(),
            entity.getDetails()
        );
    }
}
