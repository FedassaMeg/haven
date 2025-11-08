package org.haven.api.intake;

import org.haven.intake.application.commands.*;
import org.haven.intake.application.services.IntakeAppService;
import org.haven.intake.application.dto.PreIntakeContactDto;
import org.haven.intake.domain.PreIntakeContactId;
import org.haven.api.intake.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import java.util.UUID;

/**
 * Intake Workflow API Controller Implementation
 *
 * Implements the VAWA-compliant progressive intake workflow.
 * This replaces the TODO-based IntakeController with working implementations.
 *
 * IMPLEMENTED ENDPOINTS:
 * 1. POST /api/v1/intake/pre-contact - Create temporary client contact
 * 2. GET /api/v1/intake/pre-contact/{id} - Retrieve pre-intake contact
 * 3. PUT /api/v1/intake/pre-contact/{id} - Update pre-intake contact
 * 6. POST /api/v1/intake/clients/promote - Promote temp client to full client
 *
 * NOT YET IMPLEMENTED:
 * 4. POST /api/v1/intake/programs/match - Program matching engine
 * 5. GET /api/v1/intake/programs/available - Available programs query
 * 7. POST /api/v1/intake/enrollments - Create enrollment
 * 8. POST /api/v1/intake/tasks/recurring - Create recurring tasks
 * 9. GET /api/v1/intake/tasks - Query tasks
 * 10. POST /api/v1/intake/documents - Upload document
 * 11. GET /api/v1/intake/documents/{id} - Retrieve document
 */
@RestController
@RequestMapping("/api/v1/intake")
@Validated
public class IntakeControllerImpl {

    private final IntakeAppService intakeAppService;

    public IntakeControllerImpl(IntakeAppService intakeAppService) {
        this.intakeAppService = intakeAppService;
    }

    // =============================================================================
    // ENDPOINT 1: CREATE PRE-INTAKE CONTACT
    // =============================================================================

    @PostMapping("/pre-contact")
    public ResponseEntity<CreatePreIntakeContactResponse> createPreIntakeContact(
            @Valid @RequestBody CreatePreIntakeContactRequest request) {

        var cmd = new CreatePreIntakeContactCmd(
            request.clientAlias(),
            request.contactDate(),
            request.referralSource(),
            request.intakeWorkerName()
        );

        var tempClientId = intakeAppService.handle(cmd);

        PreIntakeContactDto contact = intakeAppService.getPreIntakeContact(tempClientId)
            .orElseThrow(() -> new IllegalStateException("Failed to retrieve created pre-intake contact"));

        var response = new CreatePreIntakeContactResponse(
            contact.tempClientId(),
            contact.clientAlias(),
            contact.createdAt(),
            contact.expiresAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =============================================================================
    // ENDPOINT 2: GET PRE-INTAKE CONTACT
    // =============================================================================

    @GetMapping("/pre-contact/{tempClientId}")
    public ResponseEntity<PreIntakeContactDto> getPreIntakeContact(
            @PathVariable UUID tempClientId) {

        PreIntakeContactDto contact = intakeAppService.getPreIntakeContact(PreIntakeContactId.of(tempClientId))
            .orElseThrow(() -> new IllegalArgumentException("Pre-intake contact not found: " + tempClientId));

        if (contact.expired()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        return ResponseEntity.ok(contact);
    }

    // =============================================================================
    // ENDPOINT 3: UPDATE PRE-INTAKE CONTACT
    // =============================================================================

    @PutMapping("/pre-contact/{tempClientId}")
    public ResponseEntity<PreIntakeContactDto> updatePreIntakeContact(
            @PathVariable UUID tempClientId,
            @Valid @RequestBody UpdatePreIntakeContactRequest request) {

        var tempClientIdObj = PreIntakeContactId.of(tempClientId);

        // Handle basic info update
        if (request.clientAlias() != null || request.contactDate() != null || request.referralSource() != null) {
            var cmd = new UpdatePreIntakeContactCmd(
                tempClientIdObj,
                request.clientAlias(),
                request.contactDate(),
                request.referralSource()
            );
            intakeAppService.handle(cmd);
        }

        // Handle workflow step update
        if (request.step() != null && request.stepData() != null) {
            var workflowCmd = new UpdateWorkflowDataCmd(
                tempClientIdObj,
                request.step(),
                request.stepData()
            );
            intakeAppService.handle(workflowCmd);
        }

        PreIntakeContactDto contact = intakeAppService.getPreIntakeContact(tempClientIdObj)
            .orElseThrow(() -> new IllegalArgumentException("Pre-intake contact not found: " + tempClientId));

        return ResponseEntity.ok(contact);
    }

    // =============================================================================
    // ENDPOINT 6: PROMOTE CLIENT (TEMP → FULL)
    // =============================================================================

    @PostMapping("/clients/promote")
    public ResponseEntity<PromoteClientResponse> promoteClient(
            @Valid @RequestBody PromoteClientRequest request) {

        var addresses = request.addresses() != null ?
            request.addresses().stream().map(PromoteClientRequest.AddressDto::toValueObject).toList() :
            null;

        var telecoms = request.telecoms() != null ?
            request.telecoms().stream().map(PromoteClientRequest.ContactPointDto::toValueObject).toList() :
            null;

        var cmd = new PromoteClientCmd(
            PreIntakeContactId.of(request.tempClientId()),
            request.givenName(),
            request.familyName(),
            request.gender(),
            request.birthDate(),
            addresses,
            telecoms,
            request.vawaProtected(),
            request.socialSecurityNumber()
        );

        var clientId = intakeAppService.handle(cmd);

        var response = new PromoteClientResponse(
            clientId.value(),
            request.tempClientId(),
            "Client promoted successfully. Pre-intake contact marked as promoted."
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =============================================================================
    // NOT YET IMPLEMENTED ENDPOINTS
    // =============================================================================

    @PostMapping("/programs/match")
    public ResponseEntity<?> matchPrograms(@RequestBody Object request) {
        throw new UnsupportedOperationException("Program matching not yet implemented");
    }

    @GetMapping("/programs/available")
    public ResponseEntity<?> getAvailablePrograms() {
        throw new UnsupportedOperationException("Available programs query not yet implemented");
    }

    @PostMapping("/enrollments")
    public ResponseEntity<?> createEnrollment(@RequestBody Object request) {
        throw new UnsupportedOperationException("Enrollment creation not yet implemented");
    }

    @PostMapping("/tasks/recurring")
    public ResponseEntity<?> createRecurringTask(@RequestBody Object request) {
        throw new UnsupportedOperationException("Recurring task creation not yet implemented");
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getTasks() {
        throw new UnsupportedOperationException("Task query not yet implemented");
    }

    @PostMapping("/documents")
    public ResponseEntity<?> uploadDocument(@RequestBody Object request) {
        throw new UnsupportedOperationException("Document upload not yet implemented");
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable UUID documentId) {
        throw new UnsupportedOperationException("Document retrieval not yet implemented");
    }
}
