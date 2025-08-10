package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.CodeableConcept;
import org.junit.jupiter.api.Test;
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
        caseRecord.assignTo("user123", role);

        // Assert
        assertNotNull(caseRecord.getAssignment());
        assertEquals("user123", caseRecord.getAssignment().assigneeId());
        assertEquals(role, caseRecord.getAssignment().role());
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
            caseRecord.assignTo("user123", role);
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
}