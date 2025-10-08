package org.haven.casemgmt.domain;

import org.haven.casemgmt.domain.events.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CaseRecordTest {

    @Test
    void shouldOpenCase() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");

        // Act
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Initial assessment case");

        // Assert
        assertNotNull(caseRecord.getId());
        assertEquals(clientId, caseRecord.getClientId());
        assertEquals(caseType, caseRecord.getCaseType());
        assertEquals(priority, caseRecord.getPriority());
        assertEquals("Initial assessment case", caseRecord.getDescription());
        assertEquals(CaseRecord.CaseStatus.OPEN, caseRecord.getStatus());
        assertTrue(caseRecord.isActive());
        assertEquals(1, caseRecord.getPendingEvents().size());
    }

    @Test
    void shouldAssignCase() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");
        caseRecord.clearPendingEvents();

        var role = createRole("case-manager", "Case Manager");

        // Act
        caseRecord.assignTo("user123", "John Doe", role, 
                           CaseAssignment.AssignmentType.PRIMARY, "Initial assignment", "admin");

        // Assert
        assertTrue(caseRecord.hasActivePrimaryAssignment());
        var assignment = caseRecord.getCurrentPrimaryAssignment().orElseThrow();
        assertEquals("user123", assignment.getAssigneeId());
        assertEquals("John Doe", assignment.getAssigneeName());
        assertEquals(role, assignment.getRole());
        assertEquals(1, caseRecord.getPendingEvents().size());
    }

    @Test
    void shouldAddNote() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");
        caseRecord.clearPendingEvents();

        // Act
        caseRecord.addNote("Initial contact made with client", "user123");

        // Assert
        assertEquals(1, caseRecord.getNotes().size());
        assertEquals("Initial contact made with client", caseRecord.getNotes().get(0).content());
        assertEquals("user123", caseRecord.getNotes().get(0).authorId());
        assertEquals(1, caseRecord.getPendingEvents().size());
    }

    @Test
    void shouldUpdateStatus() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");
        caseRecord.clearPendingEvents();

        // Act
        caseRecord.updateStatus(CaseRecord.CaseStatus.IN_PROGRESS);

        // Assert
        assertEquals(CaseRecord.CaseStatus.IN_PROGRESS, caseRecord.getStatus());
        assertEquals(1, caseRecord.getPendingEvents().size());
    }

    @Test
    void shouldCloseCase() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");
        caseRecord.clearPendingEvents();

        // Act
        caseRecord.close("Assessment completed successfully");

        // Assert
        assertEquals(CaseRecord.CaseStatus.CLOSED, caseRecord.getStatus());
        assertFalse(caseRecord.isActive());
        assertNotNull(caseRecord.getPeriod().end());
        assertEquals(1, caseRecord.getPendingEvents().size());
    }

    @Test
    void shouldLinkProgramEnrollment() {
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");
        caseRecord.clearPendingEvents();

        var enrollmentId = ProgramEnrollmentId.generate();

        caseRecord.linkProgramEnrollment(enrollmentId, "caseworker", "Coordinated support plan");

        assertEquals(1, caseRecord.getLinkedEnrollments().size());
        assertTrue(caseRecord.getLinkedEnrollments().contains(enrollmentId));
        assertEquals(1, caseRecord.getPendingEvents().size());
        assertInstanceOf(ProgramEnrollmentLinked.class, caseRecord.getPendingEvents().get(0));
    }

    @Test
    void shouldPreventDuplicateProgramEnrollmentLink() {
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");

        var enrollmentId = ProgramEnrollmentId.generate();
        caseRecord.linkProgramEnrollment(enrollmentId, "caseworker", "Initial linkage");

        assertThrows(IllegalStateException.class, () ->
            caseRecord.linkProgramEnrollment(enrollmentId, "caseworker", "Duplicate linkage")
        );
    }

    @Test
    void shouldNotAssignClosedCase() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var caseRecord = CaseRecord.open(clientId, caseType, priority, "Test case");
        caseRecord.close("Completed");
        var role = createRole("case-manager", "Case Manager");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            caseRecord.assignTo("user123", "John Doe", role, 
                               CaseAssignment.AssignmentType.PRIMARY, "Initial assignment", "admin");
        });
    }

    private CodeableConcept createCaseType(String code, String display) {
        var coding = new CodeableConcept.Coding(
            "http://haven.org/fhir/CodeSystem/case-type", null, code, display, null
        );
        return new CodeableConcept(List.of(coding), display);
    }

    private CodeableConcept createPriority(String code, String display) {
        var coding = new CodeableConcept.Coding(
            "http://haven.org/fhir/CodeSystem/case-priority", null, code, display, null
        );
        return new CodeableConcept(List.of(coding), display);
    }

    private CodeableConcept createRole(String code, String display) {
        var coding = new CodeableConcept.Coding(
            "http://haven.org/fhir/CodeSystem/participant-role", null, code, display, null
        );
        return new CodeableConcept(List.of(coding), display);
    }

    @Test
    void shouldReconstructFromEvents() {
        // Arrange
        UUID caseId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        var role = createRole("case-manager", "Case Manager");
        Instant now = Instant.now();

        List<DomainEvent> events = new ArrayList<>();
        events.add(new CaseOpened(caseId, clientId, caseType, priority, "Test case", now));
        events.add(new CaseAssigned(caseId, UUID.randomUUID(), "user123", "John Doe",
                                   role, CaseAssignment.AssignmentType.PRIMARY,
                                   "Initial assignment", "admin", true, now));
        events.add(new CaseNoteAdded(caseId, UUID.randomUUID(), "Test note", "user123", now));

        // Act
        var reconstructed = CaseRecord.reconstruct(caseId, events);

        // Assert
        assertNotNull(reconstructed);
        assertEquals(new CaseId(caseId), reconstructed.getId());
        assertEquals(new ClientId(clientId), reconstructed.getClientId());
        assertEquals(caseType, reconstructed.getCaseType());
        assertEquals(priority, reconstructed.getPriority());
        assertEquals("Test case", reconstructed.getDescription());
        assertEquals(CaseRecord.CaseStatus.OPEN, reconstructed.getStatus());
        assertEquals(3, reconstructed.getVersion());
        assertTrue(reconstructed.getPendingEvents().isEmpty()); // No pending events after reconstruction
        assertEquals(1, reconstructed.getNotes().size());
        assertTrue(reconstructed.hasActivePrimaryAssignment());
    }

    @Test
    void shouldReconstructWithoutCreatingNewEvents() {
        // Arrange
        var clientId = new ClientId(UUID.randomUUID());
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");

        // Create a case and get its events
        var original = CaseRecord.open(clientId, caseType, priority, "Test case");
        var events = new ArrayList<>(original.getPendingEvents());
        UUID caseId = original.getId().value();

        // Act - reconstruct from same events
        var reconstructed = CaseRecord.reconstruct(caseId, events);

        // Assert
        assertEquals(original.getId(), reconstructed.getId());
        assertEquals(original.getClientId(), reconstructed.getClientId());
        assertEquals(original.getCaseType(), reconstructed.getCaseType());
        assertEquals(original.getStatus(), reconstructed.getStatus());
        assertTrue(reconstructed.getPendingEvents().isEmpty()); // Critical: no new events queued
    }

    @Test
    void shouldReconstructCompleteLifecycle() {
        // Arrange
        UUID caseId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        var caseType = createCaseType("assessment", "Initial Assessment");
        var priority = createPriority("medium", "Medium Priority");
        Instant now = Instant.now();

        List<DomainEvent> events = new ArrayList<>();
        events.add(new CaseOpened(caseId, clientId, caseType, priority, "Test case", now));
        events.add(new CaseStatusChanged(caseId, CaseRecord.CaseStatus.OPEN,
                                        CaseRecord.CaseStatus.IN_PROGRESS, now));
        events.add(new CaseClosed(caseId, "Completed successfully", now));

        // Act
        var reconstructed = CaseRecord.reconstruct(caseId, events);

        // Assert
        assertEquals(CaseRecord.CaseStatus.CLOSED, reconstructed.getStatus());
        assertFalse(reconstructed.isActive());
        assertNotNull(reconstructed.getPeriod());
        assertNotNull(reconstructed.getPeriod().end());
        assertEquals(3, reconstructed.getVersion());
    }
}
