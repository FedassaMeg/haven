package org.haven.programenrollment.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.haven.programenrollment.domain.ProjectLinkage;
import org.haven.programenrollment.domain.ProjectLinkageId;
import org.haven.programenrollment.domain.ProjectLinkageRepository;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link ProjectLinkageRepository} until a persistent adapter is provided.
 * Stores linkage aggregates in a thread-safe map to support application services and jobs.
 */
@Repository
public class InMemoryProjectLinkageRepository implements ProjectLinkageRepository {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final int AUDIT_REVIEW_THRESHOLD_DAYS = 90;

    private final Map<ProjectLinkageId, ProjectLinkage> store = new ConcurrentHashMap<>();

    @Override
    public void save(ProjectLinkage linkage) {
        Objects.requireNonNull(linkage, "linkage must not be null");
        Objects.requireNonNull(linkage.getId(), "linkage id must not be null");
        store.put(linkage.getId(), linkage);
    }

    @Override
    public Optional<ProjectLinkage> findById(ProjectLinkageId linkageId) {
        if (linkageId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(linkageId));
    }

    @Override
    public Optional<ProjectLinkage> findActiveLinkage(UUID thProjectId, UUID rrhProjectId) {
        return store.values().stream()
            .filter(this::isActive)
            .filter(linkage -> Objects.equals(linkage.getThProjectId(), thProjectId))
            .filter(linkage -> Objects.equals(linkage.getRrhProjectId(), rrhProjectId))
            .findFirst();
    }

    @Override
    public List<ProjectLinkage> findActiveLinkagesForThProject(UUID thProjectId) {
        if (thProjectId == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(this::isActive)
            .filter(linkage -> Objects.equals(linkage.getThProjectId(), thProjectId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ProjectLinkage> findActiveLinkagesForRrhProject(UUID rrhProjectId) {
        if (rrhProjectId == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(this::isActive)
            .filter(linkage -> Objects.equals(linkage.getRrhProjectId(), rrhProjectId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ProjectLinkage> findAllLinkagesForProject(UUID projectId) {
        if (projectId == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(linkage -> Objects.equals(linkage.getThProjectId(), projectId)
                || Objects.equals(linkage.getRrhProjectId(), projectId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ProjectLinkage> findLinkagesEffectiveOn(LocalDate date) {
        if (date == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(linkage -> linkage.wasEffectiveOn(date))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ProjectLinkage> findByHudProjectIds(String thHudProjectId, String rrhHudProjectId) {
        if ((thHudProjectId == null || thHudProjectId.isBlank()) || (rrhHudProjectId == null || rrhHudProjectId.isBlank())) {
            return Optional.empty();
        }
        return store.values().stream()
            .filter(linkage -> thHudProjectId.equals(linkage.getThHudProjectId()))
            .filter(linkage -> rrhHudProjectId.equals(linkage.getRrhHudProjectId()))
            .findFirst();
    }

    @Override
    public List<ProjectLinkage> findLinkagesCreatedBy(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return store.values().stream()
            .filter(linkage -> Objects.equals(linkage.getAuthorizedByUserId(), userId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ProjectLinkage> findLinkagesRequiringAuditReview() {
        LocalDate cutoff = LocalDate.now().minusDays(AUDIT_REVIEW_THRESHOLD_DAYS);
        return store.values().stream()
            .filter(linkage -> needsAuditReview(linkage, cutoff))
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasActiveLinkages(UUID thProjectId) {
        if (thProjectId == null) {
            return false;
        }
        return store.values().stream()
            .anyMatch(linkage -> Objects.equals(linkage.getThProjectId(), thProjectId) && isActive(linkage));
    }

    @Override
    public long countActiveLinkages() {
        return store.values().stream()
            .filter(this::isActive)
            .count();
    }

    private boolean isActive(ProjectLinkage linkage) {
        return linkage != null && linkage.isEffective();
    }

    private boolean needsAuditReview(ProjectLinkage linkage, LocalDate cutoffDate) {
        if (linkage == null) {
            return false;
        }

        boolean staleModification = Optional.ofNullable(linkage.getLastModifiedAt())
            .map(instant -> toLocalDate(instant).isBefore(cutoffDate))
            .orElse(true);

        boolean missingNotes = linkage.getLinkageNotes() == null || linkage.getLinkageNotes().isBlank();
        boolean recentlyRevoked = linkage.getStatus() == ProjectLinkage.LinkageStatus.REVOKED
            && Optional.ofNullable(linkage.getLinkageEndDate())
                .map(endDate -> !endDate.isBefore(LocalDate.now().minusDays(30)))
                .orElse(false);

        return staleModification || (linkage.getStatus() == ProjectLinkage.LinkageStatus.ACTIVE && missingNotes) || recentlyRevoked;
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(DEFAULT_ZONE).toLocalDate();
    }
}
