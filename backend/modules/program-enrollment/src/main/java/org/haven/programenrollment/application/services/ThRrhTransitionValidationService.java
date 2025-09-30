package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for validating TH/RRH transition constraints and generating data quality alerts
 */
@Service
@Transactional(readOnly = true)
public class ThRrhTransitionValidationService {

    private final ProjectLinkageRepository linkageRepository;
    private final ProgramEnrollmentRepository enrollmentRepository;
    private final DataQualityAlertService dataQualityAlertService;

    public ThRrhTransitionValidationService(ProjectLinkageRepository linkageRepository,
                                          ProgramEnrollmentRepository enrollmentRepository,
                                          DataQualityAlertService dataQualityAlertService) {
        this.linkageRepository = linkageRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.dataQualityAlertService = dataQualityAlertService;
    }

    /**
     * Validate move-in date constraints for TH to RRH transition
     */
    public TransitionValidationResult validateTransition(TransitionValidationRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        List<DataQualityAlert> alerts = new ArrayList<>();

        try {
            // Get the enrollments
            ProgramEnrollment thEnrollment = getEnrollment(request.getThEnrollmentId(), "TH");
            ProgramEnrollment rrhEnrollment = getEnrollment(request.getRrhEnrollmentId(), "RRH");

            // Check for active linkage
            var linkageOpt = linkageRepository.findActiveLinkage(
                thEnrollment.getProgramId(),
                rrhEnrollment.getProgramId()
            );

            if (linkageOpt.isEmpty()) {
                ValidationError error = new ValidationError(
                    ValidationError.ErrorType.MISSING_LINKAGE,
                    "No active linkage found between TH and RRH projects",
                    "TH Project: " + thEnrollment.getProgramId() + ", RRH Project: " + rrhEnrollment.getProgramId()
                );
                errors.add(error);

                // Create data quality alert
                alerts.add(createDataQualityAlert(
                    DataQualityAlert.AlertType.MISSING_PREDECESSOR,
                    "RRH enrollment missing required TH predecessor linkage",
                    request.getRrhEnrollmentId(),
                    DataQualityAlert.Severity.HIGH
                ));

                return new TransitionValidationResult(false, errors, alerts);
            }

            ProjectLinkage linkage = linkageOpt.get();

            // Validate core constraint: RRH move-in cannot precede TH exit
            if (request.getRrhMoveInDate() != null && request.getThExitDate() != null) {
                if (request.getRrhMoveInDate().isBefore(request.getThExitDate())) {
                    ValidationError error = new ValidationError(
                        ValidationError.ErrorType.MOVE_IN_DATE_CONSTRAINT,
                        "RRH move-in date cannot precede TH exit date",
                        String.format("TH Exit: %s, RRH Move-in: %s",
                            request.getThExitDate(), request.getRrhMoveInDate())
                    );
                    errors.add(error);

                    // Create high-severity alert
                    alerts.add(createDataQualityAlert(
                        DataQualityAlert.AlertType.MOVE_IN_DATE_VIOLATION,
                        "RRH move-in date precedes TH exit date",
                        request.getRrhEnrollmentId(),
                        DataQualityAlert.Severity.HIGH
                    ));
                }

                // Check for excessive transition gap
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    request.getThExitDate(), request.getRrhMoveInDate());

                if (daysBetween > 30) {
                    ValidationError warning = new ValidationError(
                        ValidationError.ErrorType.EXCESSIVE_TRANSITION_GAP,
                        "Transition gap exceeds 30 days",
                        String.format("Gap: %d days between TH exit and RRH move-in", daysBetween)
                    );
                    errors.add(warning);

                    // Create medium-severity alert
                    alerts.add(createDataQualityAlert(
                        DataQualityAlert.AlertType.EXCESSIVE_TRANSITION_GAP,
                        String.format("Transition gap of %d days exceeds threshold", daysBetween),
                        request.getRrhEnrollmentId(),
                        DataQualityAlert.Severity.MEDIUM
                    ));
                }

                if (daysBetween > 90) {
                    ValidationError error = new ValidationError(
                        ValidationError.ErrorType.EXCESSIVE_TRANSITION_GAP,
                        "Transition gap exceeds 90 days - requires documentation",
                        String.format("Gap: %d days requires case manager review", daysBetween)
                    );
                    errors.add(error);

                    // Upgrade to high severity for extreme gaps
                    alerts.add(createDataQualityAlert(
                        DataQualityAlert.AlertType.EXCESSIVE_TRANSITION_GAP,
                        String.format("Extreme transition gap of %d days requires review", daysBetween),
                        request.getRrhEnrollmentId(),
                        DataQualityAlert.Severity.HIGH
                    ));
                }
            }

            // Validate household consistency
            if (!validateHouseholdConsistency(thEnrollment, rrhEnrollment)) {
                ValidationError error = new ValidationError(
                    ValidationError.ErrorType.HOUSEHOLD_INCONSISTENCY,
                    "Household composition inconsistency between TH and RRH enrollments",
                    "TH and RRH enrollments should maintain household composition"
                );
                errors.add(error);

                alerts.add(createDataQualityAlert(
                    DataQualityAlert.AlertType.HOUSEHOLD_INCONSISTENCY,
                    "Household composition differs between linked TH and RRH enrollments",
                    request.getRrhEnrollmentId(),
                    DataQualityAlert.Severity.MEDIUM
                ));
            }

            // Check for overlapping enrollments
            if (hasOverlappingEnrollments(thEnrollment, rrhEnrollment)) {
                ValidationError error = new ValidationError(
                    ValidationError.ErrorType.OVERLAPPING_ENROLLMENTS,
                    "Client has overlapping TH and RRH enrollments",
                    "TH and RRH enrollment periods should not overlap"
                );
                errors.add(error);

                alerts.add(createDataQualityAlert(
                    DataQualityAlert.AlertType.OVERLAPPING_ENROLLMENTS,
                    "Client has overlapping TH and RRH enrollment periods",
                    request.getRrhEnrollmentId(),
                    DataQualityAlert.Severity.HIGH
                ));
            }

            // Validate RRH enrollment has predecessor reference
            if (rrhEnrollment.getPredecessorEnrollmentId() == null ||
                !rrhEnrollment.getPredecessorEnrollmentId().equals(request.getThEnrollmentId())) {

                ValidationError warning = new ValidationError(
                    ValidationError.ErrorType.MISSING_PREDECESSOR_REFERENCE,
                    "RRH enrollment missing predecessor TH enrollment reference",
                    "RRH enrollment should reference the TH enrollment ID"
                );
                errors.add(warning);

                alerts.add(createDataQualityAlert(
                    DataQualityAlert.AlertType.MISSING_PREDECESSOR,
                    "RRH enrollment missing predecessor reference",
                    request.getRrhEnrollmentId(),
                    DataQualityAlert.Severity.MEDIUM
                ));
            }

        } catch (IllegalArgumentException e) {
            ValidationError error = new ValidationError(
                ValidationError.ErrorType.ENROLLMENT_NOT_FOUND,
                "Required enrollment not found",
                e.getMessage()
            );
            errors.add(error);
        }

        // Generate data quality alerts in the system
        for (DataQualityAlert alert : alerts) {
            dataQualityAlertService.createAlert(alert);
        }

        boolean isValid = errors.stream().noneMatch(e ->
            e.getErrorType() == ValidationError.ErrorType.MOVE_IN_DATE_CONSTRAINT ||
            e.getErrorType() == ValidationError.ErrorType.OVERLAPPING_ENROLLMENTS ||
            e.getErrorType() == ValidationError.ErrorType.MISSING_LINKAGE
        );

        return new TransitionValidationResult(isValid, errors, alerts);
    }

    /**
     * Batch validate multiple transitions (for data quality jobs)
     */
    public List<TransitionValidationResult> validateTransitions(List<TransitionValidationRequest> requests) {
        return requests.stream()
            .map(this::validateTransition)
            .toList();
    }

    /**
     * Find all transition violations in the system
     */
    public List<TransitionViolation> findAllTransitionViolations() {
        List<TransitionViolation> violations = new ArrayList<>();

        // Get all active linkages
        List<ProjectLinkage> linkages = linkageRepository.findLinkagesEffectiveOn(LocalDate.now());

        for (ProjectLinkage linkage : linkages) {
            // Find all TH enrollments for this project
            List<ProgramEnrollment> thEnrollments = enrollmentRepository
                .findByProgramId(linkage.getThProjectId());

            for (ProgramEnrollment thEnrollment : thEnrollments) {
                // Find corresponding RRH enrollment
                Optional<ProgramEnrollment> rrhEnrollmentOpt = enrollmentRepository
                    .findByClientIdAndProgramId(thEnrollment.getClientId(), linkage.getRrhProjectId());

                if (rrhEnrollmentOpt.isPresent()) {
                    ProgramEnrollment rrhEnrollment = rrhEnrollmentOpt.get();
                    if (rrhEnrollment.getPredecessorEnrollmentId() != null &&
                        rrhEnrollment.getPredecessorEnrollmentId().equals(thEnrollment.getId().value())) {

                        // Check for violations
                        LocalDate thExitDate = thEnrollment.getExitDate();
                        LocalDate rrhMoveInDate = rrhEnrollment.getResidentialMoveInDate();

                        if (thExitDate != null && rrhMoveInDate != null &&
                            rrhMoveInDate.isBefore(thExitDate)) {

                            violations.add(new TransitionViolation(
                                thEnrollment.getId().value(),
                                rrhEnrollment.getId().value(),
                                linkage.getId().value(),
                                thExitDate,
                                rrhMoveInDate,
                                TransitionViolation.ViolationType.MOVE_IN_DATE_CONSTRAINT,
                                "RRH move-in date precedes TH exit date"
                            ));
                        }

                        // Check for excessive gaps
                        if (thExitDate != null && rrhMoveInDate != null) {
                            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(thExitDate, rrhMoveInDate);
                            if (daysBetween > 90) {
                                violations.add(new TransitionViolation(
                                    thEnrollment.getId().value(),
                                    rrhEnrollment.getId().value(),
                                    linkage.getId().value(),
                                    thExitDate,
                                    rrhMoveInDate,
                                    TransitionViolation.ViolationType.EXCESSIVE_TRANSITION_GAP,
                                    String.format("Transition gap of %d days exceeds threshold", daysBetween)
                                ));
                            }
                        }
                    }
                }
            }
        }

        return violations;
    }

    private ProgramEnrollment getEnrollment(UUID enrollmentId, String type) {
        return enrollmentRepository.findById(ProgramEnrollmentId.of(enrollmentId))
            .orElseThrow(() -> new IllegalArgumentException(type + " enrollment not found: " + enrollmentId));
    }

    private boolean validateHouseholdConsistency(ProgramEnrollment thEnrollment, ProgramEnrollment rrhEnrollment) {
        // Check if household composition IDs match
        return thEnrollment.getHouseholdCompositionId() != null &&
               thEnrollment.getHouseholdCompositionId().equals(rrhEnrollment.getHouseholdCompositionId());
    }

    private boolean hasOverlappingEnrollments(ProgramEnrollment thEnrollment, ProgramEnrollment rrhEnrollment) {
        LocalDate thStart = thEnrollment.getEnrollmentDate();
        LocalDate thEnd = thEnrollment.getExitDate();
        LocalDate rrhStart = rrhEnrollment.getEnrollmentDate();
        LocalDate rrhEnd = rrhEnrollment.getExitDate();

        // If TH hasn't exited, use current date
        if (thEnd == null) {
            thEnd = LocalDate.now();
        }

        // If RRH hasn't exited, use current date
        if (rrhEnd == null) {
            rrhEnd = LocalDate.now();
        }

        // Check for overlap
        return !thEnd.isBefore(rrhStart) && !rrhEnd.isBefore(thStart);
    }

    private DataQualityAlert createDataQualityAlert(DataQualityAlert.AlertType alertType,
                                                   String message,
                                                   UUID enrollmentId,
                                                   DataQualityAlert.Severity severity) {
        return new DataQualityAlert(
            UUID.randomUUID(),
            alertType,
            message,
            enrollmentId,
            severity,
            LocalDate.now(),
            false,
            null,
            null
        );
    }

    /**
     * Request object for transition validation
     */
    public static class TransitionValidationRequest {
        private UUID thEnrollmentId;
        private UUID rrhEnrollmentId;
        private LocalDate thExitDate;
        private LocalDate rrhMoveInDate;

        public TransitionValidationRequest(UUID thEnrollmentId, UUID rrhEnrollmentId,
                                         LocalDate thExitDate, LocalDate rrhMoveInDate) {
            this.thEnrollmentId = thEnrollmentId;
            this.rrhEnrollmentId = rrhEnrollmentId;
            this.thExitDate = thExitDate;
            this.rrhMoveInDate = rrhMoveInDate;
        }

        // Getters
        public UUID getThEnrollmentId() { return thEnrollmentId; }
        public UUID getRrhEnrollmentId() { return rrhEnrollmentId; }
        public LocalDate getThExitDate() { return thExitDate; }
        public LocalDate getRrhMoveInDate() { return rrhMoveInDate; }
    }

    /**
     * Result object for transition validation
     */
    public static class TransitionValidationResult {
        private boolean isValid;
        private List<ValidationError> errors;
        private List<DataQualityAlert> alerts;

        public TransitionValidationResult(boolean isValid, List<ValidationError> errors, List<DataQualityAlert> alerts) {
            this.isValid = isValid;
            this.errors = errors;
            this.alerts = alerts;
        }

        // Getters
        public boolean isValid() { return isValid; }
        public List<ValidationError> getErrors() { return errors; }
        public List<DataQualityAlert> getAlerts() { return alerts; }
    }

    /**
     * Validation error details
     */
    public static class ValidationError {
        private ErrorType errorType;
        private String message;
        private String details;

        public ValidationError(ErrorType errorType, String message, String details) {
            this.errorType = errorType;
            this.message = message;
            this.details = details;
        }

        public enum ErrorType {
            MOVE_IN_DATE_CONSTRAINT,
            EXCESSIVE_TRANSITION_GAP,
            MISSING_LINKAGE,
            HOUSEHOLD_INCONSISTENCY,
            OVERLAPPING_ENROLLMENTS,
            MISSING_PREDECESSOR_REFERENCE,
            ENROLLMENT_NOT_FOUND
        }

        // Getters
        public ErrorType getErrorType() { return errorType; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
    }

    /**
     * Transition violation details
     */
    public static class TransitionViolation {
        private UUID thEnrollmentId;
        private UUID rrhEnrollmentId;
        private UUID linkageId;
        private LocalDate thExitDate;
        private LocalDate rrhMoveInDate;
        private ViolationType violationType;
        private String description;

        public TransitionViolation(UUID thEnrollmentId, UUID rrhEnrollmentId, UUID linkageId,
                                 LocalDate thExitDate, LocalDate rrhMoveInDate,
                                 ViolationType violationType, String description) {
            this.thEnrollmentId = thEnrollmentId;
            this.rrhEnrollmentId = rrhEnrollmentId;
            this.linkageId = linkageId;
            this.thExitDate = thExitDate;
            this.rrhMoveInDate = rrhMoveInDate;
            this.violationType = violationType;
            this.description = description;
        }

        public enum ViolationType {
            MOVE_IN_DATE_CONSTRAINT,
            EXCESSIVE_TRANSITION_GAP,
            MISSING_PREDECESSOR,
            OVERLAPPING_ENROLLMENTS
        }

        // Getters
        public UUID getThEnrollmentId() { return thEnrollmentId; }
        public UUID getRrhEnrollmentId() { return rrhEnrollmentId; }
        public UUID getLinkageId() { return linkageId; }
        public LocalDate getThExitDate() { return thExitDate; }
        public LocalDate getRrhMoveInDate() { return rrhMoveInDate; }
        public ViolationType getViolationType() { return violationType; }
        public String getDescription() { return description; }
    }
}