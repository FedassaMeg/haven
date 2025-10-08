package org.haven.api.projectlinkage;

import org.haven.programenrollment.application.services.ProjectLinkageService;
import org.haven.programenrollment.application.services.DataQualityDashboardService;
import org.haven.programenrollment.domain.ProjectLinkage;
import org.haven.programenrollment.domain.ProjectLinkageId;
import org.haven.api.projectlinkage.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing TH/RRH project linkages
 */
@RestController
@RequestMapping("/api/v1/project-linkages")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectLinkageController {

    private final ProjectLinkageService linkageService;
    private final DataQualityDashboardService dashboardService;

    public ProjectLinkageController(ProjectLinkageService linkageService,
                                   DataQualityDashboardService dashboardService) {
        this.linkageService = linkageService;
        this.dashboardService = dashboardService;
    }

    /**
     * Create a new project linkage
     */
    @PostMapping
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<CreateProjectLinkageResponse> createLinkage(
            @Valid @RequestBody CreateProjectLinkageRequest request) {

        try {
            ProjectLinkageService.CreateProjectLinkageCommand command =
                new ProjectLinkageService.CreateProjectLinkageCommand(
                    request.getThProjectId(),
                    request.getRrhProjectId(),
                    request.getThHudProjectId(),
                    request.getRrhHudProjectId(),
                    request.getThProjectName(),
                    request.getRrhProjectName(),
                    request.getEffectiveDate(),
                    request.getLinkageReason(),
                    request.getAuthorizedBy(),
                    request.getAuthorizedByUserId()
                );

            ProjectLinkageId linkageId = linkageService.createLinkage(command);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateProjectLinkageResponse(
                    linkageId.value(),
                    "Project linkage created successfully"
                ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CreateProjectLinkageResponse(null, e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CreateProjectLinkageResponse(null, "Failed to create linkage: " + e.getMessage()));
        }
    }

    /**
     * Modify an existing linkage
     */
    @PutMapping("/{linkageId}")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> modifyLinkage(
            @PathVariable UUID linkageId,
            @Valid @RequestBody ModifyProjectLinkageRequest request) {

        try {
            linkageService.modifyLinkage(
                linkageId,
                request.getLinkageReason(),
                request.getLinkageNotes()
            );

            return ResponseEntity.ok(new ApiResponse("Linkage modified successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse("Linkage not found: " + e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Failed to modify linkage: " + e.getMessage()));
        }
    }

    /**
     * Revoke an existing linkage
     */
    @DeleteMapping("/{linkageId}")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> revokeLinkage(
            @PathVariable UUID linkageId,
            @Valid @RequestBody RevokeLinkageRequest request) {

        try {
            linkageService.revokeLinkage(
                linkageId,
                request.getRevocationDate(),
                request.getRevocationReason()
            );

            return ResponseEntity.ok(new ApiResponse("Linkage revoked successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse("Linkage not found: " + e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse("Failed to revoke linkage: " + e.getMessage()));
        }
    }

    /**
     * Get all active linkages for a project
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ProjectLinkageResponse>> getActiveLinkagesForProject(
            @PathVariable UUID projectId) {

        try {
            List<ProjectLinkage> linkages = linkageService.getActiveLinkagesForProject(projectId);
            List<ProjectLinkageResponse> responses = linkages.stream()
                .map(this::toProjectLinkageResponse)
                .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get linkage audit trail for a project
     */
    @GetMapping("/project/{projectId}/audit-trail")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ProjectLinkageResponse>> getLinkageAuditTrail(
            @PathVariable UUID projectId) {

        try {
            List<ProjectLinkage> linkages = linkageService.getLinkageAuditTrail(projectId);
            List<ProjectLinkageResponse> responses = linkages.stream()
                .map(this::toProjectLinkageResponse)
                .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if two projects can be linked
     */
    @GetMapping("/can-link")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<CanLinkProjectsResponse> canLinkProjects(
            @RequestParam UUID thProjectId,
            @RequestParam UUID rrhProjectId) {

        try {
            boolean canLink = linkageService.canLinkProjects(thProjectId, rrhProjectId);
            return ResponseEntity.ok(new CanLinkProjectsResponse(canLink,
                canLink ? "Projects can be linked" : "Projects cannot be linked"));

        } catch (Exception e) {
            return ResponseEntity.ok(new CanLinkProjectsResponse(false, e.getMessage()));
        }
    }

    /**
     * Get linkages requiring review
     */
    @GetMapping("/requiring-review")
    @PreAuthorize("hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ProjectLinkageResponse>> getLinkagesRequiringReview() {

        try {
            List<ProjectLinkage> linkages = linkageService.getLinkagesRequiringReview();
            List<ProjectLinkageResponse> responses = linkages.stream()
                .map(this::toProjectLinkageResponse)
                .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get data quality dashboard overview
     */
    @GetMapping("/dashboard/overview")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DataQualityDashboardService.DashboardOverview> getDashboardOverview() {

        try {
            DataQualityDashboardService.DashboardOverview overview = dashboardService.getDashboardOverview();
            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get alerts summary widget
     */
    @GetMapping("/dashboard/alerts-summary")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DataQualityDashboardService.AlertsSummaryWidget> getAlertsSummaryWidget() {

        try {
            DataQualityDashboardService.AlertsSummaryWidget widget = dashboardService.getAlertsSummaryWidget();
            return ResponseEntity.ok(widget);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get linkage health widget
     */
    @GetMapping("/dashboard/linkage-health")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DataQualityDashboardService.LinkageHealthWidget> getLinkageHealthWidget() {

        try {
            DataQualityDashboardService.LinkageHealthWidget widget = dashboardService.getLinkageHealthWidget();
            return ResponseEntity.ok(widget);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get violation trends widget
     */
    @GetMapping("/dashboard/violation-trends")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('PROGRAM_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DataQualityDashboardService.ViolationTrendsWidget> getViolationTrendsWidget() {

        try {
            DataQualityDashboardService.ViolationTrendsWidget widget = dashboardService.getViolationTrendsWidget();
            return ResponseEntity.ok(widget);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ProjectLinkageResponse toProjectLinkageResponse(ProjectLinkage linkage) {
        return new ProjectLinkageResponse(
            linkage.getId().value(),
            linkage.getThProjectId(),
            linkage.getRrhProjectId(),
            linkage.getThHudProjectId(),
            linkage.getRrhHudProjectId(),
            linkage.getThProjectName(),
            linkage.getRrhProjectName(),
            linkage.getLinkageEffectiveDate(),
            linkage.getLinkageEndDate(),
            linkage.getStatus().name(),
            linkage.getLinkageReason(),
            linkage.getLinkageNotes(),
            linkage.getCreatedBy(),
            linkage.getLastModifiedBy(),
            linkage.getCreatedAt(),
            linkage.getLastModifiedAt()
        );
    }
}