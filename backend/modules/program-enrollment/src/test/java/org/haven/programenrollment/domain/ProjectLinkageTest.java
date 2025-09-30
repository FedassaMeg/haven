package org.haven.programenrollment.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectLinkageTest {

    private UUID thProjectId;
    private UUID rrhProjectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        thProjectId = UUID.randomUUID();
        rrhProjectId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create project linkage - should create with valid data")
    void create_ValidData_ShouldCreateLinkage() {
        // Act
        ProjectLinkage linkage = ProjectLinkage.create(
            thProjectId,
            rrhProjectId,
            "TH-2024-001",
            "RRH-2024-001",
            "Test TH Project",
            "Test RRH Project",
            LocalDate.of(2024, 1, 1),
            "Test linkage reason",
            "Test User",
            userId
        );

        // Assert
        assertThat(linkage.getId()).isNotNull();
        assertThat(linkage.getThProjectId()).isEqualTo(thProjectId);
        assertThat(linkage.getRrhProjectId()).isEqualTo(rrhProjectId);
        assertThat(linkage.getThHudProjectId()).isEqualTo("TH-2024-001");
        assertThat(linkage.getRrhHudProjectId()).isEqualTo("RRH-2024-001");
        assertThat(linkage.getStatus()).isEqualTo(ProjectLinkage.LinkageStatus.ACTIVE);
        assertThat(linkage.isEffective()).isTrue();
    }

    @Test
    @DisplayName("Modify linkage - should update reason and notes")
    void modifyLinkage_ValidData_ShouldUpdate() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();

        // Act
        linkage.modifyLinkage(
            "Updated reason",
            "Updated notes",
            "Modifier",
            userId
        );

        // Assert
        assertThat(linkage.getLinkageReason()).isEqualTo("Updated reason");
        assertThat(linkage.getLinkageNotes()).isEqualTo("Updated notes");
        assertThat(linkage.getLastModifiedBy()).isEqualTo("Modifier");
    }

    @Test
    @DisplayName("Revoke linkage - should change status to revoked")
    void revokeLinkage_ValidData_ShouldRevoke() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();

        // Act
        linkage.revokeLinkage(
            LocalDate.of(2024, 6, 1),
            "No longer needed",
            "Revoker",
            userId
        );

        // Assert
        assertThat(linkage.getStatus()).isEqualTo(ProjectLinkage.LinkageStatus.REVOKED);
        assertThat(linkage.getLinkageEndDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(linkage.isEffective()).isFalse();
    }

    @Test
    @DisplayName("Modify revoked linkage - should throw exception")
    void modifyLinkage_RevokedLinkage_ShouldThrowException() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();
        linkage.revokeLinkage(
            LocalDate.of(2024, 6, 1),
            "No longer needed",
            "Revoker",
            userId
        );

        // Act & Assert
        assertThatThrownBy(() -> linkage.modifyLinkage(
            "Updated reason",
            "Updated notes",
            "Modifier",
            userId
        )).isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot modify revoked linkage");
    }

    @Test
    @DisplayName("Revoke already revoked linkage - should throw exception")
    void revokeLinkage_AlreadyRevoked_ShouldThrowException() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();
        linkage.revokeLinkage(
            LocalDate.of(2024, 6, 1),
            "No longer needed",
            "Revoker",
            userId
        );

        // Act & Assert
        assertThatThrownBy(() -> linkage.revokeLinkage(
            LocalDate.of(2024, 7, 1),
            "Another reason",
            "Another Revoker",
            userId
        )).isInstanceOf(IllegalStateException.class)
          .hasMessage("Linkage is already revoked");
    }

    @Test
    @DisplayName("Validate transition constraints - valid dates should pass")
    void validateTransitionConstraints_ValidDates_ShouldPass() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();
        LocalDate thExitDate = LocalDate.of(2024, 2, 1);
        LocalDate rrhMoveInDate = LocalDate.of(2024, 2, 1); // Same day - valid

        // Act & Assert - should not throw
        linkage.validateTransitionConstraints(thExitDate, rrhMoveInDate);
    }

    @Test
    @DisplayName("Validate transition constraints - move-in before exit should fail")
    void validateTransitionConstraints_MoveInBeforeExit_ShouldFail() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();
        LocalDate thExitDate = LocalDate.of(2024, 2, 1);
        LocalDate rrhMoveInDate = LocalDate.of(2024, 1, 31); // Before exit

        // Act & Assert
        assertThatThrownBy(() -> linkage.validateTransitionConstraints(thExitDate, rrhMoveInDate))
            .isInstanceOf(ProjectLinkageViolationException.class)
            .hasMessage("RRH move-in date (2024-01-31) cannot precede TH exit date (2024-02-01)");
    }

    @Test
    @DisplayName("Validate transition constraints - excessive gap should fail")
    void validateTransitionConstraints_ExcessiveGap_ShouldFail() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();
        LocalDate thExitDate = LocalDate.of(2024, 1, 1);
        LocalDate rrhMoveInDate = LocalDate.of(2024, 3, 1); // 60 days later

        // Act & Assert
        assertThatThrownBy(() -> linkage.validateTransitionConstraints(thExitDate, rrhMoveInDate))
            .isInstanceOf(ProjectLinkageViolationException.class)
            .hasMessageContaining("Transition gap of 60 days exceeds threshold");
    }

    @Test
    @DisplayName("Was effective on date - should check effective period")
    void wasEffectiveOn_DifferentDates_ShouldCheckCorrectly() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage(); // Effective from 2024-01-01

        // Act & Assert
        assertThat(linkage.wasEffectiveOn(LocalDate.of(2023, 12, 31))).isFalse(); // Before
        assertThat(linkage.wasEffectiveOn(LocalDate.of(2024, 1, 1))).isTrue();   // Start date
        assertThat(linkage.wasEffectiveOn(LocalDate.of(2024, 6, 1))).isTrue();   // During

        // Revoke the linkage
        linkage.revokeLinkage(
            LocalDate.of(2024, 6, 1),
            "Test revocation",
            "Revoker",
            userId
        );

        assertThat(linkage.wasEffectiveOn(LocalDate.of(2024, 7, 1))).isFalse();  // After revocation
    }

    @Test
    @DisplayName("Get linkage duration - should calculate correctly")
    void getLinkageDurationDays_ShouldCalculateCorrectly() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage(); // Effective from 2024-01-01

        // Revoke after 30 days
        linkage.revokeLinkage(
            LocalDate.of(2024, 1, 31),
            "Test revocation",
            "Revoker",
            userId
        );

        // Act
        long duration = linkage.getLinkageDurationDays();

        // Assert
        assertThat(duration).isEqualTo(30);
    }

    @Test
    @DisplayName("Validate with null dates - should throw exception")
    void validateTransitionConstraints_NullDates_ShouldThrowException() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();

        // Act & Assert
        assertThatThrownBy(() -> linkage.validateTransitionConstraints(null, LocalDate.of(2024, 2, 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TH exit date is required for transition validation");

        assertThatThrownBy(() -> linkage.validateTransitionConstraints(LocalDate.of(2024, 2, 1), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("RRH move-in date is required for transition validation");
    }

    @Test
    @DisplayName("Validate with revoked linkage - should throw exception")
    void validateTransitionConstraints_RevokedLinkage_ShouldThrowException() {
        // Arrange
        ProjectLinkage linkage = createTestLinkage();
        linkage.revokeLinkage(
            LocalDate.of(2024, 6, 1),
            "No longer needed",
            "Revoker",
            userId
        );

        // Act & Assert
        assertThatThrownBy(() -> linkage.validateTransitionConstraints(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 1)
        )).isInstanceOf(IllegalStateException.class)
          .hasMessage("Cannot validate transition - linkage is not effective");
    }

    private ProjectLinkage createTestLinkage() {
        return ProjectLinkage.create(
            thProjectId,
            rrhProjectId,
            "TH-2024-001",
            "RRH-2024-001",
            "Test TH Project",
            "Test RRH Project",
            LocalDate.of(2024, 1, 1),
            "Test linkage reason",
            "Test User",
            userId
        );
    }
}