package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Application service for managing TH/RRH project linkages
 */
@Service
@Transactional
public class ProjectLinkageService {

    private final ProjectLinkageRepository linkageRepository;
    private final ProgramEnrollmentRepository enrollmentRepository;
    private final UserContext userContext;

    public ProjectLinkageService(ProjectLinkageRepository linkageRepository,
                                ProgramEnrollmentRepository enrollmentRepository,
                                UserContext userContext) {
        this.linkageRepository = linkageRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userContext = userContext;
    }

    /**
     * Create a new project linkage
     */
    public ProjectLinkageId createLinkage(CreateProjectLinkageCommand command) {
        // Validate user authorization
        validateUserAuthorization(command.getAuthorizedByUserId());

        // Check if linkage already exists
        var existingLinkage = linkageRepository.findActiveLinkage(
            command.getThProjectId(),
            command.getRrhProjectId()
        );

        if (existingLinkage.isPresent()) {
            throw new IllegalStateException(
                "Active linkage already exists between TH project " +
                command.getThProjectId() + " and RRH project " + command.getRrhProjectId()
            );
        }

        // Validate project types and eligibility
        validateProjectsForLinkage(command.getThProjectId(), command.getRrhProjectId());

        // Create the linkage
        ProjectLinkage linkage = ProjectLinkage.create(
            command.getThProjectId(),
            command.getRrhProjectId(),
            command.getThHudProjectId(),
            command.getRrhHudProjectId(),
            command.getThProjectName(),
            command.getRrhProjectName(),
            command.getEffectiveDate(),
            command.getLinkageReason(),
            command.getAuthorizedBy(),
            command.getAuthorizedByUserId()
        );

        linkageRepository.save(linkage);
        return linkage.getId();
    }

    /**
     * Modify an existing linkage
     */
    public void modifyLinkage(UUID linkageId,
                             String newLinkageReason,
                             String newLinkageNotes) {
        ProjectLinkage linkage = linkageRepository.findById(ProjectLinkageId.of(linkageId))
            .orElseThrow(() -> new IllegalArgumentException("Linkage not found: " + linkageId));

        String currentUser = userContext.getCurrentUser();
        UUID currentUserId = userContext.getCurrentUserId();

        // Validate user authorization
        validateUserAuthorization(currentUserId);

        linkage.modifyLinkage(newLinkageReason, newLinkageNotes, currentUser, currentUserId);
        linkageRepository.save(linkage);
    }

    /**
     * Revoke an existing linkage
     */
    public void revokeLinkage(UUID linkageId,
                             LocalDate revocationDate,
                             String revocationReason) {
        ProjectLinkage linkage = linkageRepository.findById(ProjectLinkageId.of(linkageId))
            .orElseThrow(() -> new IllegalArgumentException("Linkage not found: " + linkageId));

        String currentUser = userContext.getCurrentUser();
        UUID currentUserId = userContext.getCurrentUserId();

        // Validate user authorization
        validateUserAuthorization(currentUserId);

        linkage.revokeLinkage(revocationDate, revocationReason, currentUser, currentUserId);
        linkageRepository.save(linkage);
    }

    /**
     * Validate TH to RRH transition for linked projects
     */
    public void validateTransition(UUID thEnrollmentId,
                                  UUID rrhEnrollmentId,
                                  LocalDate thExitDate,
                                  LocalDate rrhMoveInDate) {

        // Get enrollments
        ProgramEnrollment thEnrollment = enrollmentRepository.findById(ProgramEnrollmentId.of(thEnrollmentId))
            .orElseThrow(() -> new IllegalArgumentException("TH enrollment not found: " + thEnrollmentId));

        ProgramEnrollment rrhEnrollment = enrollmentRepository.findById(ProgramEnrollmentId.of(rrhEnrollmentId))
            .orElseThrow(() -> new IllegalArgumentException("RRH enrollment not found: " + rrhEnrollmentId));

        // Find active linkage between the projects
        var linkageOpt = linkageRepository.findActiveLinkage(
            thEnrollment.getProgramId(),
            rrhEnrollment.getProgramId()
        );

        if (linkageOpt.isEmpty()) {
            throw new ProjectLinkageViolationException(
                "No active linkage found between TH project " + thEnrollment.getProgramId() +
                " and RRH project " + rrhEnrollment.getProgramId(),
                ProjectLinkageViolationException.ViolationType.MISSING_PREDECESSOR,
                null,
                thEnrollment.getProgramId(),
                rrhEnrollment.getProgramId()
            );
        }

        ProjectLinkage linkage = linkageOpt.get();

        // Validate transition constraints
        linkage.validateTransitionConstraints(thExitDate, rrhMoveInDate);
    }

    /**
     * Get all active linkages for a project
     */
    @Transactional(readOnly = true)
    public List<ProjectLinkage> getActiveLinkagesForProject(UUID projectId) {
        List<ProjectLinkage> thLinkages = linkageRepository.findActiveLinkagesForThProject(projectId);
        List<ProjectLinkage> rrhLinkages = linkageRepository.findActiveLinkagesForRrhProject(projectId);

        // Combine and deduplicate
        return Stream.concat(thLinkages.stream(), rrhLinkages.stream())
            .distinct()
            .toList();
    }

    /**
     * Get linkage audit trail for a project
     */
    @Transactional(readOnly = true)
    public List<ProjectLinkage> getLinkageAuditTrail(UUID projectId) {
        return linkageRepository.findAllLinkagesForProject(projectId);
    }

    /**
     * Check if projects can be linked
     */
    @Transactional(readOnly = true)
    public boolean canLinkProjects(UUID thProjectId, UUID rrhProjectId) {
        try {
            validateProjectsForLinkage(thProjectId, rrhProjectId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get linkages requiring review
     */
    @Transactional(readOnly = true)
    public List<ProjectLinkage> getLinkagesRequiringReview() {
        return linkageRepository.findLinkagesRequiringAuditReview();
    }

    private void validateUserAuthorization(UUID userId) {
        if (!userContext.hasRole("PROGRAM_MANAGER") && !userContext.hasRole("ADMIN")) {
            throw new ProjectLinkageViolationException(
                "User does not have permission to manage project linkages",
                ProjectLinkageViolationException.ViolationType.UNAUTHORIZED_MODIFICATION,
                null,
                null,
                null
            );
        }
    }

    private void validateProjectsForLinkage(UUID thProjectId, UUID rrhProjectId) {
        // This would typically validate:
        // 1. TH project exists and is of type TH or JOINT_TH_RRH
        // 2. RRH project exists and is of type RRH or JOINT_TH_RRH
        // 3. Projects are within same organization/jurisdiction
        // 4. Projects have compatible service models

        // For now, we'll assume projects are valid if they exist
        // In a real implementation, you'd query a project repository
    }

    /**
     * Command object for creating project linkages
     */
    public static class CreateProjectLinkageCommand {
        private UUID thProjectId;
        private UUID rrhProjectId;
        private String thHudProjectId;
        private String rrhHudProjectId;
        private String thProjectName;
        private String rrhProjectName;
        private LocalDate effectiveDate;
        private String linkageReason;
        private String authorizedBy;
        private UUID authorizedByUserId;

        // Constructor, getters, and setters
        public CreateProjectLinkageCommand(UUID thProjectId, UUID rrhProjectId,
                                         String thHudProjectId, String rrhHudProjectId,
                                         String thProjectName, String rrhProjectName,
                                         LocalDate effectiveDate, String linkageReason,
                                         String authorizedBy, UUID authorizedByUserId) {
            this.thProjectId = thProjectId;
            this.rrhProjectId = rrhProjectId;
            this.thHudProjectId = thHudProjectId;
            this.rrhHudProjectId = rrhHudProjectId;
            this.thProjectName = thProjectName;
            this.rrhProjectName = rrhProjectName;
            this.effectiveDate = effectiveDate;
            this.linkageReason = linkageReason;
            this.authorizedBy = authorizedBy;
            this.authorizedByUserId = authorizedByUserId;
        }

        // Getters
        public UUID getThProjectId() { return thProjectId; }
        public UUID getRrhProjectId() { return rrhProjectId; }
        public String getThHudProjectId() { return thHudProjectId; }
        public String getRrhHudProjectId() { return rrhHudProjectId; }
        public String getThProjectName() { return thProjectName; }
        public String getRrhProjectName() { return rrhProjectName; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public String getLinkageReason() { return linkageReason; }
        public String getAuthorizedBy() { return authorizedBy; }
        public UUID getAuthorizedByUserId() { return authorizedByUserId; }
    }
}