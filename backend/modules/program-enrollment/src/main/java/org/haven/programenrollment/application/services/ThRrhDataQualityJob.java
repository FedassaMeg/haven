package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.programenrollment.domain.ProjectLinkage;
import org.haven.programenrollment.domain.ProjectLinkageRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Nightly job to scan for TH/RRH transition violations and generate data quality reports
 */
@Service
@Transactional
public class ThRrhDataQualityJob {

    private final ThRrhTransitionValidationService validationService;
    private final DataQualityAlertService alertService;
    private final EmailNotificationService emailService;
    private final DataQualityDashboardService dashboardService;
    private final ProgramEnrollmentRepository enrollmentRepository;
    private final ProjectLinkageRepository linkageRepository;

    public ThRrhDataQualityJob(ThRrhTransitionValidationService validationService,
                              DataQualityAlertService alertService,
                              EmailNotificationService emailService,
                              DataQualityDashboardService dashboardService,
                              ProgramEnrollmentRepository enrollmentRepository,
                              ProjectLinkageRepository linkageRepository) {
        this.validationService = validationService;
        this.alertService = alertService;
        this.emailService = emailService;
        this.dashboardService = dashboardService;
        this.enrollmentRepository = enrollmentRepository;
        this.linkageRepository = linkageRepository;
    }

    /**
     * Run nightly data quality scan
     * Scheduled to run at 2:00 AM daily
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runNightlyDataQualityScan() {
        System.out.println("Starting nightly TH/RRH data quality scan at " + LocalDateTime.now());

        try {
            DataQualityJobResult result = performDataQualityScan();

            // Update dashboard widgets
            dashboardService.updateDataQualityMetrics(result);

            // Send email digest if there are issues
            if (result.hasViolations()) {
                emailService.sendDailyDigest(result.getNewAlerts());
            }

            System.out.printf("Nightly scan completed: %d violations found, %d alerts created%n",
                result.getTotalViolations(), result.getNewAlerts().size());

        } catch (Exception e) {
            System.err.println("Error during nightly data quality scan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run weekly escalation check
     * Scheduled to run every Monday at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void runWeeklyEscalationCheck() {
        System.out.println("Running weekly escalation check at " + LocalDateTime.now());

        try {
            // Find alerts that have been unresolved for more than 7 days
            List<DataQualityAlert> escalatedAlerts = alertService.getUnresolvedAlerts()
                .stream()
                .filter(alert -> alert.getCreatedDate().isBefore(LocalDate.now().minusDays(7)))
                .toList();

            if (!escalatedAlerts.isEmpty()) {
                emailService.sendEscalationNotification(escalatedAlerts);
                System.out.printf("Escalated %d unresolved alerts to management%n", escalatedAlerts.size());
            }

        } catch (Exception e) {
            System.err.println("Error during weekly escalation check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Perform comprehensive data quality scan
     */
    public DataQualityJobResult performDataQualityScan() {
        List<DataQualityAlert> newAlerts = new ArrayList<>();
        List<ThRrhTransitionValidationService.TransitionViolation> violations = new ArrayList<>();

        // Get all transition violations
        violations.addAll(validationService.findAllTransitionViolations());

        // Scan for missing predecessors
        List<MissingPredecessorViolation> missingPredecessors = findMissingPredecessorViolations();
        violations.addAll(convertToTransitionViolations(missingPredecessors));

        // Scan for overlapping enrollments
        List<OverlappingEnrollmentViolation> overlappingEnrollments = findOverlappingEnrollmentViolations();
        violations.addAll(convertToTransitionViolations(overlappingEnrollments));

        // Create alerts for new violations
        for (ThRrhTransitionValidationService.TransitionViolation violation : violations) {
            if (!isExistingViolation(violation)) {
                DataQualityAlert alert = createAlertFromViolation(violation);
                newAlerts.add(alert);
                alertService.createAlert(alert);
            }
        }

        // Generate metrics for dashboard
        DataQualityMetrics metrics = generateDataQualityMetrics(violations);

        return new DataQualityJobResult(
            violations.size(),
            newAlerts,
            metrics,
            LocalDateTime.now()
        );
    }

    /**
     * Find RRH enrollments missing predecessor TH enrollment references
     */
    private List<MissingPredecessorViolation> findMissingPredecessorViolations() {
        List<MissingPredecessorViolation> violations = new ArrayList<>();

        // Get all active linkages
        List<ProjectLinkage> linkages = linkageRepository.findLinkagesEffectiveOn(LocalDate.now());

        for (ProjectLinkage linkage : linkages) {
            // Find RRH enrollments for this linkage
            List<ProgramEnrollment> rrhEnrollments = enrollmentRepository
                .findByProgramId(linkage.getRrhProjectId());

            for (ProgramEnrollment rrhEnrollment : rrhEnrollments) {
                // Check if RRH enrollment has a predecessor reference
                if (rrhEnrollment.getPredecessorEnrollmentId() == null) {
                    // Look for potential TH enrollment for same client
                    Optional<ProgramEnrollment> potentialThEnrollment = enrollmentRepository
                        .findByClientIdAndProgramId(rrhEnrollment.getClientId(), linkage.getThProjectId());

                    if (potentialThEnrollment.isPresent()) {
                        violations.add(new MissingPredecessorViolation(
                            rrhEnrollment.getId().value(),
                            linkage.getId().value(),
                            potentialThEnrollment.get().getId().value(),
                            "RRH enrollment missing predecessor TH enrollment reference"
                        ));
                    }
                }
            }
        }

        return violations;
    }

    /**
     * Find overlapping TH and RRH enrollments for the same client
     */
    private List<OverlappingEnrollmentViolation> findOverlappingEnrollmentViolations() {
        List<OverlappingEnrollmentViolation> violations = new ArrayList<>();

        // Get all active linkages
        List<ProjectLinkage> linkages = linkageRepository.findLinkagesEffectiveOn(LocalDate.now());

        for (ProjectLinkage linkage : linkages) {
            // Get all clients who have enrollments in both projects
            List<ProgramEnrollment> thEnrollments = enrollmentRepository
                .findByProgramId(linkage.getThProjectId());

            for (ProgramEnrollment thEnrollment : thEnrollments) {
                Optional<ProgramEnrollment> rrhEnrollmentOpt = enrollmentRepository
                    .findByClientIdAndProgramId(thEnrollment.getClientId(), linkage.getRrhProjectId());

                if (rrhEnrollmentOpt.isPresent()) {
                    ProgramEnrollment rrhEnrollment = rrhEnrollmentOpt.get();
                    if (hasOverlappingPeriods(thEnrollment, rrhEnrollment)) {
                        violations.add(new OverlappingEnrollmentViolation(
                            thEnrollment.getId().value(),
                            rrhEnrollment.getId().value(),
                            linkage.getId().value(),
                            "Client has overlapping TH and RRH enrollment periods"
                        ));
                    }
                }
            }
        }

        return violations;
    }

    private boolean hasOverlappingPeriods(ProgramEnrollment thEnrollment, ProgramEnrollment rrhEnrollment) {
        LocalDate thStart = thEnrollment.getEnrollmentDate();
        LocalDate thEnd = thEnrollment.getExitDate() != null ? thEnrollment.getExitDate() : LocalDate.now();
        LocalDate rrhStart = rrhEnrollment.getEnrollmentDate();
        LocalDate rrhEnd = rrhEnrollment.getExitDate() != null ? rrhEnrollment.getExitDate() : LocalDate.now();

        return !thEnd.isBefore(rrhStart) && !rrhEnd.isBefore(thStart);
    }

    private List<ThRrhTransitionValidationService.TransitionViolation> convertToTransitionViolations(
            List<? extends DataQualityViolation> violations) {
        return violations.stream()
            .map(this::convertToTransitionViolation)
            .toList();
    }

    private ThRrhTransitionValidationService.TransitionViolation convertToTransitionViolation(
            DataQualityViolation violation) {
        if (violation instanceof MissingPredecessorViolation mpv) {
            return new ThRrhTransitionValidationService.TransitionViolation(
                mpv.getThEnrollmentId(),
                mpv.getRrhEnrollmentId(),
                mpv.getLinkageId(),
                null,
                null,
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.MISSING_PREDECESSOR,
                mpv.getDescription()
            );
        } else if (violation instanceof OverlappingEnrollmentViolation oev) {
            return new ThRrhTransitionValidationService.TransitionViolation(
                oev.getThEnrollmentId(),
                oev.getRrhEnrollmentId(),
                oev.getLinkageId(),
                null,
                null,
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.OVERLAPPING_ENROLLMENTS,
                oev.getDescription()
            );
        }
        throw new IllegalArgumentException("Unknown violation type: " + violation.getClass());
    }

    private boolean isExistingViolation(ThRrhTransitionValidationService.TransitionViolation violation) {
        // Check if we already have an unresolved alert for this violation
        List<DataQualityAlert> existingAlerts = alertService.getAlertsForEnrollment(violation.getRrhEnrollmentId());
        return existingAlerts.stream()
            .anyMatch(alert -> !alert.isResolved() &&
                alert.getAlertType().name().equals(violation.getViolationType().name()));
    }

    private DataQualityAlert createAlertFromViolation(ThRrhTransitionValidationService.TransitionViolation violation) {
        DataQualityAlert.AlertType alertType = switch (violation.getViolationType()) {
            case MOVE_IN_DATE_CONSTRAINT -> DataQualityAlert.AlertType.MOVE_IN_DATE_VIOLATION;
            case EXCESSIVE_TRANSITION_GAP -> DataQualityAlert.AlertType.EXCESSIVE_TRANSITION_GAP;
            case MISSING_PREDECESSOR -> DataQualityAlert.AlertType.MISSING_PREDECESSOR;
            case OVERLAPPING_ENROLLMENTS -> DataQualityAlert.AlertType.OVERLAPPING_ENROLLMENTS;
        };

        DataQualityAlert.Severity severity = switch (violation.getViolationType()) {
            case MOVE_IN_DATE_CONSTRAINT, OVERLAPPING_ENROLLMENTS -> DataQualityAlert.Severity.HIGH;
            case MISSING_PREDECESSOR -> DataQualityAlert.Severity.MEDIUM;
            case EXCESSIVE_TRANSITION_GAP -> DataQualityAlert.Severity.LOW;
        };

        return new DataQualityAlert(
            UUID.randomUUID(),
            alertType,
            violation.getDescription(),
            violation.getRrhEnrollmentId(),
            severity,
            LocalDate.now(),
            false,
            null,
            null
        );
    }

    private DataQualityMetrics generateDataQualityMetrics(
            List<ThRrhTransitionValidationService.TransitionViolation> violations) {

        long moveInDateViolations = violations.stream()
            .filter(v -> v.getViolationType() == ThRrhTransitionValidationService.TransitionViolation.ViolationType.MOVE_IN_DATE_CONSTRAINT)
            .count();

        long excessiveGapViolations = violations.stream()
            .filter(v -> v.getViolationType() == ThRrhTransitionValidationService.TransitionViolation.ViolationType.EXCESSIVE_TRANSITION_GAP)
            .count();

        long missingPredecessorViolations = violations.stream()
            .filter(v -> v.getViolationType() == ThRrhTransitionValidationService.TransitionViolation.ViolationType.MISSING_PREDECESSOR)
            .count();

        long overlappingEnrollmentViolations = violations.stream()
            .filter(v -> v.getViolationType() == ThRrhTransitionValidationService.TransitionViolation.ViolationType.OVERLAPPING_ENROLLMENTS)
            .count();

        return new DataQualityMetrics(
            violations.size(),
            moveInDateViolations,
            excessiveGapViolations,
            missingPredecessorViolations,
            overlappingEnrollmentViolations,
            LocalDate.now()
        );
    }

    // Supporting classes
    private interface DataQualityViolation {
        UUID getRrhEnrollmentId();
        UUID getLinkageId();
        String getDescription();
    }

    private static class MissingPredecessorViolation implements DataQualityViolation {
        private UUID rrhEnrollmentId;
        private UUID linkageId;
        private UUID thEnrollmentId;
        private String description;

        public MissingPredecessorViolation(UUID rrhEnrollmentId, UUID linkageId, UUID thEnrollmentId, String description) {
            this.rrhEnrollmentId = rrhEnrollmentId;
            this.linkageId = linkageId;
            this.thEnrollmentId = thEnrollmentId;
            this.description = description;
        }

        @Override
        public UUID getRrhEnrollmentId() { return rrhEnrollmentId; }
        @Override
        public UUID getLinkageId() { return linkageId; }
        @Override
        public String getDescription() { return description; }
        public UUID getThEnrollmentId() { return thEnrollmentId; }
    }

    private static class OverlappingEnrollmentViolation implements DataQualityViolation {
        private UUID thEnrollmentId;
        private UUID rrhEnrollmentId;
        private UUID linkageId;
        private String description;

        public OverlappingEnrollmentViolation(UUID thEnrollmentId, UUID rrhEnrollmentId, UUID linkageId, String description) {
            this.thEnrollmentId = thEnrollmentId;
            this.rrhEnrollmentId = rrhEnrollmentId;
            this.linkageId = linkageId;
            this.description = description;
        }

        @Override
        public UUID getRrhEnrollmentId() { return rrhEnrollmentId; }
        @Override
        public UUID getLinkageId() { return linkageId; }
        @Override
        public String getDescription() { return description; }
        public UUID getThEnrollmentId() { return thEnrollmentId; }
    }

    /**
     * Result of data quality job execution
     */
    public static class DataQualityJobResult {
        private int totalViolations;
        private List<DataQualityAlert> newAlerts;
        private DataQualityMetrics metrics;
        private LocalDateTime executionTime;

        public DataQualityJobResult(int totalViolations, List<DataQualityAlert> newAlerts,
                                   DataQualityMetrics metrics, LocalDateTime executionTime) {
            this.totalViolations = totalViolations;
            this.newAlerts = newAlerts;
            this.metrics = metrics;
            this.executionTime = executionTime;
        }

        public boolean hasViolations() {
            return totalViolations > 0;
        }

        // Getters
        public int getTotalViolations() { return totalViolations; }
        public List<DataQualityAlert> getNewAlerts() { return newAlerts; }
        public DataQualityMetrics getMetrics() { return metrics; }
        public LocalDateTime getExecutionTime() { return executionTime; }
    }

    /**
     * Data quality metrics for dashboard
     */
    public static class DataQualityMetrics {
        private long totalViolations;
        private long moveInDateViolations;
        private long excessiveGapViolations;
        private long missingPredecessorViolations;
        private long overlappingEnrollmentViolations;
        private LocalDate metricsDate;

        public DataQualityMetrics(long totalViolations, long moveInDateViolations,
                                 long excessiveGapViolations, long missingPredecessorViolations,
                                 long overlappingEnrollmentViolations, LocalDate metricsDate) {
            this.totalViolations = totalViolations;
            this.moveInDateViolations = moveInDateViolations;
            this.excessiveGapViolations = excessiveGapViolations;
            this.missingPredecessorViolations = missingPredecessorViolations;
            this.overlappingEnrollmentViolations = overlappingEnrollmentViolations;
            this.metricsDate = metricsDate;
        }

        // Getters
        public long getTotalViolations() { return totalViolations; }
        public long getMoveInDateViolations() { return moveInDateViolations; }
        public long getExcessiveGapViolations() { return excessiveGapViolations; }
        public long getMissingPredecessorViolations() { return missingPredecessorViolations; }
        public long getOverlappingEnrollmentViolations() { return overlappingEnrollmentViolations; }
        public LocalDate getMetricsDate() { return metricsDate; }
    }
}