package org.haven.programenrollment.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProjectLinkage aggregate
 */
public interface ProjectLinkageRepository {

    /**
     * Save a project linkage
     */
    void save(ProjectLinkage linkage);

    /**
     * Find linkage by ID
     */
    Optional<ProjectLinkage> findById(ProjectLinkageId linkageId);

    /**
     * Find active linkage between two projects
     */
    Optional<ProjectLinkage> findActiveLinkage(UUID thProjectId, UUID rrhProjectId);

    /**
     * Find all active linkages for a TH project
     */
    List<ProjectLinkage> findActiveLinkagesForThProject(UUID thProjectId);

    /**
     * Find all active linkages for an RRH project
     */
    List<ProjectLinkage> findActiveLinkagesForRrhProject(UUID rrhProjectId);

    /**
     * Find all linkages (active and inactive) for a project
     */
    List<ProjectLinkage> findAllLinkagesForProject(UUID projectId);

    /**
     * Find linkages effective on a specific date
     */
    List<ProjectLinkage> findLinkagesEffectiveOn(LocalDate date);

    /**
     * Find linkages by HUD project identifiers
     */
    Optional<ProjectLinkage> findByHudProjectIds(String thHudProjectId, String rrhHudProjectId);

    /**
     * Find all linkages created by a specific user
     */
    List<ProjectLinkage> findLinkagesCreatedBy(UUID userId);

    /**
     * Find linkages requiring audit review (based on age, modifications, etc.)
     */
    List<ProjectLinkage> findLinkagesRequiringAuditReview();

    /**
     * Check if a TH project has any active linkages
     */
    boolean hasActiveLinkages(UUID thProjectId);

    /**
     * Get count of active linkages system-wide
     */
    long countActiveLinkages();
}