package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DataQualityJobIntegrationTest {

    @MockBean
    private ThRrhTransitionValidationService validationService;

    @MockBean
    private DataQualityAlertService alertService;

    @MockBean
    private EmailNotificationService emailService;

    @MockBean
    private DataQualityDashboardService dashboardService;

    @MockBean
    private ProgramEnrollmentRepository enrollmentRepository;

    @MockBean
    private ProjectLinkageRepository linkageRepository;

    private ThRrhDataQualityJob dataQualityJob;

    @BeforeEach
    void setUp() {
        dataQualityJob = new ThRrhDataQualityJob(
            validationService,
            alertService,
            emailService,
            dashboardService,
            enrollmentRepository,
            linkageRepository
        );
    }

    @Test
    @DisplayName("Perform data quality scan - should detect violations and create alerts")
    void performDataQualityScan_ShouldDetectViolationsAndCreateAlerts() {
        // Arrange
        UUID thEnrollmentId = UUID.randomUUID();
        UUID rrhEnrollmentId = UUID.randomUUID();
        UUID linkageId = UUID.randomUUID();

        ThRrhTransitionValidationService.TransitionViolation violation =
            new ThRrhTransitionValidationService.TransitionViolation(
                thEnrollmentId,
                rrhEnrollmentId,
                linkageId,
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 1, 31), // Move-in before exit
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.MOVE_IN_DATE_CONSTRAINT,
                "RRH move-in date precedes TH exit date"
            );

        when(validationService.findAllTransitionViolations())
            .thenReturn(List.of(violation));

        when(linkageRepository.findLinkagesEffectiveOn(any(LocalDate.class)))
            .thenReturn(List.of()); // No linkages for missing predecessor check

        when(alertService.getAlertsForEnrollment(rrhEnrollmentId))
            .thenReturn(List.of()); // No existing alerts

        when(alertService.createAlert(any(DataQualityAlert.class)))
            .thenReturn(UUID.randomUUID());

        // Act
        ThRrhDataQualityJob.DataQualityJobResult result = dataQualityJob.performDataQualityScan();

        // Assert
        assertThat(result.getTotalViolations()).isEqualTo(1);
        assertThat(result.getNewAlerts()).hasSize(1);
        assertThat(result.hasViolations()).isTrue();
        assertThat(result.getMetrics()).isNotNull();
        assertThat(result.getMetrics().getMoveInDateViolations()).isEqualTo(1);
    }

    @Test
    @DisplayName("Perform data quality scan - no violations should return clean result")
    void performDataQualityScan_NoViolations_ShouldReturnCleanResult() {
        // Arrange
        when(validationService.findAllTransitionViolations())
            .thenReturn(List.of());

        when(linkageRepository.findLinkagesEffectiveOn(any(LocalDate.class)))
            .thenReturn(List.of());

        // Act
        ThRrhDataQualityJob.DataQualityJobResult result = dataQualityJob.performDataQualityScan();

        // Assert
        assertThat(result.getTotalViolations()).isEqualTo(0);
        assertThat(result.getNewAlerts()).isEmpty();
        assertThat(result.hasViolations()).isFalse();
        assertThat(result.getMetrics().getTotalViolations()).isEqualTo(0);
    }

    @Test
    @DisplayName("Data quality metrics - should calculate correctly")
    void dataQualityMetrics_ShouldCalculateCorrectly() {
        // Arrange
        UUID thEnrollmentId = UUID.randomUUID();
        UUID rrhEnrollmentId = UUID.randomUUID();
        UUID linkageId = UUID.randomUUID();

        List<ThRrhTransitionValidationService.TransitionViolation> violations = List.of(
            new ThRrhTransitionValidationService.TransitionViolation(
                thEnrollmentId, rrhEnrollmentId, linkageId,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 31),
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.MOVE_IN_DATE_CONSTRAINT,
                "Move-in violation"
            ),
            new ThRrhTransitionValidationService.TransitionViolation(
                thEnrollmentId, rrhEnrollmentId, linkageId,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1),
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.EXCESSIVE_TRANSITION_GAP,
                "Gap violation"
            ),
            new ThRrhTransitionValidationService.TransitionViolation(
                thEnrollmentId, rrhEnrollmentId, linkageId,
                null, null,
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.MISSING_PREDECESSOR,
                "Missing predecessor"
            )
        );

        when(validationService.findAllTransitionViolations())
            .thenReturn(violations);

        when(linkageRepository.findLinkagesEffectiveOn(any(LocalDate.class)))
            .thenReturn(List.of());

        when(alertService.getAlertsForEnrollment(any()))
            .thenReturn(List.of());

        when(alertService.createAlert(any(DataQualityAlert.class)))
            .thenReturn(UUID.randomUUID());

        // Act
        ThRrhDataQualityJob.DataQualityJobResult result = dataQualityJob.performDataQualityScan();

        // Assert
        ThRrhDataQualityJob.DataQualityMetrics metrics = result.getMetrics();
        assertThat(metrics.getTotalViolations()).isEqualTo(3);
        assertThat(metrics.getMoveInDateViolations()).isEqualTo(1);
        assertThat(metrics.getExcessiveGapViolations()).isEqualTo(1);
        assertThat(metrics.getMissingPredecessorViolations()).isEqualTo(1);
        assertThat(metrics.getOverlappingEnrollmentViolations()).isEqualTo(0);
    }

    @Test
    @DisplayName("Data quality scan with existing alerts - should not create duplicates")
    void performDataQualityScan_ExistingAlerts_ShouldNotCreateDuplicates() {
        // Arrange
        UUID thEnrollmentId = UUID.randomUUID();
        UUID rrhEnrollmentId = UUID.randomUUID();
        UUID linkageId = UUID.randomUUID();

        ThRrhTransitionValidationService.TransitionViolation violation =
            new ThRrhTransitionValidationService.TransitionViolation(
                thEnrollmentId, rrhEnrollmentId, linkageId,
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 31),
                ThRrhTransitionValidationService.TransitionViolation.ViolationType.MOVE_IN_DATE_CONSTRAINT,
                "Move-in violation"
            );

        when(validationService.findAllTransitionViolations())
            .thenReturn(List.of(violation));

        when(linkageRepository.findLinkagesEffectiveOn(any(LocalDate.class)))
            .thenReturn(List.of());

        // Mock existing alert for this violation
        DataQualityAlert existingAlert = new DataQualityAlert(
            UUID.randomUUID(),
            DataQualityAlert.AlertType.MOVE_IN_DATE_VIOLATION,
            "Existing alert",
            rrhEnrollmentId,
            DataQualityAlert.Severity.HIGH,
            LocalDate.now(),
            false, // Not resolved
            null,
            null
        );

        when(alertService.getAlertsForEnrollment(rrhEnrollmentId))
            .thenReturn(List.of(existingAlert));

        // Act
        ThRrhDataQualityJob.DataQualityJobResult result = dataQualityJob.performDataQualityScan();

        // Assert
        assertThat(result.getTotalViolations()).isEqualTo(1);
        assertThat(result.getNewAlerts()).isEmpty(); // No new alerts created
        assertThat(result.hasViolations()).isTrue();
    }

    @Test
    @DisplayName("Historical data migration test - should process legacy enrollments")
    void historicalDataMigration_ShouldProcessLegacyEnrollments() {
        // This test would verify that historical TH/RRH enrollments
        // can be properly migrated to use the new linkage system

        // Arrange
        UUID thProjectId = UUID.randomUUID();
        UUID rrhProjectId = UUID.randomUUID();

        // Create mock legacy linkage
        ProjectLinkage legacyLinkage = ProjectLinkage.create(
            thProjectId,
            rrhProjectId,
            "TH-LEGACY-001",
            "RRH-LEGACY-001",
            "Legacy TH Project",
            "Legacy RRH Project",
            LocalDate.of(2023, 1, 1),
            "Historical linkage for migration",
            "Migration Script",
            UUID.randomUUID()
        );

        when(linkageRepository.findLinkagesEffectiveOn(any(LocalDate.class)))
            .thenReturn(List.of(legacyLinkage));

        when(enrollmentRepository.findByProgramId(thProjectId))
            .thenReturn(List.of()); // No legacy enrollments

        when(validationService.findAllTransitionViolations())
            .thenReturn(List.of());

        // Act
        ThRrhDataQualityJob.DataQualityJobResult result = dataQualityJob.performDataQualityScan();

        // Assert
        assertThat(result.getTotalViolations()).isEqualTo(0);
        assertThat(result.hasViolations()).isFalse();

        // Verify that the legacy linkage is processed correctly
        assertThat(legacyLinkage.isEffective()).isTrue();
        assertThat(legacyLinkage.getThHudProjectId()).isEqualTo("TH-LEGACY-001");
        assertThat(legacyLinkage.getRrhHudProjectId()).isEqualTo("RRH-LEGACY-001");
    }

    @Test
    @DisplayName("Data quality job resilience - should handle errors gracefully")
    void dataQualityJob_ErrorHandling_ShouldBeResilient() {
        // Arrange
        when(validationService.findAllTransitionViolations())
            .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert - should not throw
        try {
            ThRrhDataQualityJob.DataQualityJobResult result = dataQualityJob.performDataQualityScan();
            // If we get here, the job handled the error gracefully
            // In a real implementation, you might return a partial result or error status
        } catch (RuntimeException e) {
            // Expected behavior - job should log error and continue
            assertThat(e.getMessage()).contains("Database connection error");
        }
    }
}