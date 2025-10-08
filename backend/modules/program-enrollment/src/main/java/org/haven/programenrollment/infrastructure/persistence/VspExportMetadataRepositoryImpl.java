package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.haven.programenrollment.domain.vsp.VspExportMetadata;
import org.haven.programenrollment.domain.vsp.VspExportMetadataRepository;
import org.haven.shared.vo.hmis.VawaRecipientCategory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class VspExportMetadataRepositoryImpl implements VspExportMetadataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public VspExportMetadata save(VspExportMetadata metadata) {
        JpaVspExportMetadataEntity entity = toEntity(metadata);

        if (entityManager.find(JpaVspExportMetadataEntity.class, entity.getExportId()) != null) {
            entity = entityManager.merge(entity);
        } else {
            entityManager.persist(entity);
        }

        entityManager.flush();
        return toDomainModel(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VspExportMetadata> findById(UUID exportId) {
        JpaVspExportMetadataEntity entity = entityManager.find(JpaVspExportMetadataEntity.class, exportId);
        return Optional.ofNullable(entity).map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findByRecipient(String recipient) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.recipient = :recipient ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("recipient", recipient);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findByRecipientAndDateRange(
            String recipient, Instant startDate, Instant endDate) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.recipient = :recipient " +
            "AND e.exportTimestamp >= :startDate AND e.exportTimestamp <= :endDate " +
            "ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("recipient", recipient);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findByRecipientCategory(VawaRecipientCategory category) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.recipientCategory = :category " +
            "ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("category", category);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findActiveExports() {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.status = :status " +
            "AND (e.expiryDate IS NULL OR e.expiryDate > :now) ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("status", VspExportMetadata.ExportStatus.ACTIVE);
        query.setParameter("now", LocalDateTime.now());

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findByPacketHash(String packetHash) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.packetHash = :packetHash " +
            "ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("packetHash", packetHash);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findExpiringBefore(LocalDateTime expiryThreshold) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.status = :status " +
            "AND e.expiryDate IS NOT NULL AND e.expiryDate <= :threshold " +
            "ORDER BY e.expiryDate ASC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("status", VspExportMetadata.ExportStatus.ACTIVE);
        query.setParameter("threshold", expiryThreshold);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRecipientAndDateRange(String recipient, Instant startDate, Instant endDate) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(e) FROM JpaVspExportMetadataEntity e " +
            "WHERE e.recipient = :recipient " +
            "AND e.exportTimestamp >= :startDate AND e.exportTimestamp <= :endDate",
            Long.class
        );
        query.setParameter("recipient", recipient);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findByConsentBasis(String consentBasis) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.consentBasis = :consentBasis " +
            "ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("consentBasis", consentBasis);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(UUID exportId, VspExportMetadata.ExportStatus status) {
        JpaVspExportMetadataEntity entity = entityManager.find(JpaVspExportMetadataEntity.class, exportId);
        if (entity != null) {
            entity.setStatus(status);
            if (status == VspExportMetadata.ExportStatus.EXPIRED) {
                entity.setUpdatedAt(Instant.now());
            }
            entityManager.merge(entity);
        }
    }

    @Override
    public int deleteExpiredOlderThan(Instant cutoffDate) {
        return entityManager.createQuery(
            "DELETE FROM JpaVspExportMetadataEntity e " +
            "WHERE e.status = :status AND e.exportTimestamp < :cutoff"
        )
        .setParameter("status", VspExportMetadata.ExportStatus.EXPIRED)
        .setParameter("cutoff", cutoffDate)
        .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VspExportMetadata> findByCeHashKey(String ceHashKey) {
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.ceHashKey = :ceHashKey " +
            "ORDER BY e.exportTimestamp DESC",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("ceHashKey", ceHashKey);

        return query.getResultList().stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExportStatistics getStatisticsForRecipient(String recipient) {
        // Get all exports for recipient
        TypedQuery<JpaVspExportMetadataEntity> query = entityManager.createQuery(
            "SELECT e FROM JpaVspExportMetadataEntity e WHERE e.recipient = :recipient",
            JpaVspExportMetadataEntity.class
        );
        query.setParameter("recipient", recipient);
        List<JpaVspExportMetadataEntity> exports = query.getResultList();

        if (exports.isEmpty()) {
            return new ExportStatistics(0, 0, 0, 0, null, null, 0.0);
        }

        long totalExports = exports.size();
        long activeExports = exports.stream()
            .filter(e -> e.getStatus() == VspExportMetadata.ExportStatus.ACTIVE)
            .count();
        long revokedExports = exports.stream()
            .filter(e -> e.getStatus() == VspExportMetadata.ExportStatus.REVOKED)
            .count();
        long expiredExports = exports.stream()
            .filter(e -> e.getStatus() == VspExportMetadata.ExportStatus.EXPIRED)
            .count();

        Instant firstExport = exports.stream()
            .map(JpaVspExportMetadataEntity::getExportTimestamp)
            .min(Instant::compareTo)
            .orElse(null);

        Instant lastExport = exports.stream()
            .map(JpaVspExportMetadataEntity::getExportTimestamp)
            .max(Instant::compareTo)
            .orElse(null);

        double averagePerMonth = 0.0;
        if (firstExport != null && lastExport != null) {
            long monthsBetween = ChronoUnit.MONTHS.between(
                firstExport.atZone(java.time.ZoneId.systemDefault()),
                lastExport.atZone(java.time.ZoneId.systemDefault())
            );
            if (monthsBetween > 0) {
                averagePerMonth = (double) totalExports / monthsBetween;
            }
        }

        return new ExportStatistics(
            totalExports,
            activeExports,
            revokedExports,
            expiredExports,
            firstExport,
            lastExport,
            averagePerMonth
        );
    }

    private VspExportMetadata toDomainModel(JpaVspExportMetadataEntity entity) {
        // Convert anonymization rules from Map to domain object
        VspExportMetadata.AnonymizationRules rules = buildAnonymizationRules(entity.getAnonymizationRules());

        // Convert share scopes
        Set<CeShareScope> shareScopes = entity.getShareScopes() != null ?
            Arrays.stream(entity.getShareScopes())
                .map(CeShareScope::valueOf)
                .collect(Collectors.toSet()) :
            new HashSet<>();

        VspExportMetadata metadata = new VspExportMetadata(
            entity.getExportId(),
            entity.getRecipient(),
            entity.getRecipientCategory(),
            entity.getConsentBasis(),
            entity.getPacketHash(),
            entity.getCeHashKey(),
            entity.getExportTimestamp(),
            entity.getExpiryDate(),
            shareScopes,
            rules,
            entity.getMetadata() != null ? entity.getMetadata() : new HashMap<>(),
            entity.getInitiatedBy()
        );

        // Set revocation info if present
        if (entity.getStatus() == VspExportMetadata.ExportStatus.REVOKED) {
            metadata.revoke(entity.getRevokedBy(), entity.getRevocationReason());
        }

        return metadata;
    }

    private JpaVspExportMetadataEntity toEntity(VspExportMetadata domainModel) {
        // Convert share scopes to array
        String[] shareScopes = domainModel.getShareScopes().stream()
            .map(Enum::name)
            .toArray(String[]::new);

        // Convert anonymization rules to Map
        Map<String, Object> rulesMap = serializeAnonymizationRules(domainModel.getAnonymizationRules());

        JpaVspExportMetadataEntity entity = new JpaVspExportMetadataEntity(
            domainModel.getExportId(),
            domainModel.getRecipient(),
            domainModel.getRecipientCategory(),
            domainModel.getConsentBasis(),
            domainModel.getPacketHash(),
            domainModel.getCeHashKey(),
            domainModel.getExportTimestamp(),
            domainModel.getExpiryDate(),
            shareScopes,
            rulesMap,
            domainModel.getMetadata(),
            domainModel.getInitiatedBy(),
            domainModel.getStatus()
        );

        if (domainModel.getStatus() == VspExportMetadata.ExportStatus.REVOKED) {
            entity.setRevokedAt(domainModel.getRevokedAt());
            entity.setRevokedBy(domainModel.getRevokedBy());
            entity.setRevocationReason(domainModel.getRevocationReason());
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    private VspExportMetadata.AnonymizationRules buildAnonymizationRules(Map<String, Object> rulesMap) {
        if (rulesMap == null) {
            return VspExportMetadata.AnonymizationRules.builder().build();
        }

        VspExportMetadata.AnonymizationRules.Builder builder = VspExportMetadata.AnonymizationRules.builder();

        builder.suppressLocationMetadata((Boolean) rulesMap.getOrDefault("suppressLocationMetadata", true));
        builder.replaceHouseholdIds((Boolean) rulesMap.getOrDefault("replaceHouseholdIds", true));
        builder.redactDvIndicators((Boolean) rulesMap.getOrDefault("redactDvIndicators", true));
        builder.anonymizeDates((Boolean) rulesMap.getOrDefault("anonymizeDates", false));

        List<String> fieldsToRedact = (List<String>) rulesMap.get("fieldsToRedact");
        if (fieldsToRedact != null) {
            for (String field : fieldsToRedact) {
                builder.addFieldToRedact(field);
            }
        }

        Map<String, String> fieldMappings = (Map<String, String>) rulesMap.get("fieldMappings");
        if (fieldMappings != null) {
            for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                builder.addFieldMapping(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    private Map<String, Object> serializeAnonymizationRules(VspExportMetadata.AnonymizationRules rules) {
        Map<String, Object> rulesMap = new HashMap<>();

        // Use reflection to extract field values
        try {
            java.lang.reflect.Field[] fields = rules.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                rulesMap.put(field.getName(), field.get(rules));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to serialize anonymization rules", e);
        }

        return rulesMap;
    }
}