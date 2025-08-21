package org.haven.api.cases;

import org.haven.casemgmt.application.services.CaseAppService;
import org.haven.casemgmt.application.commands.*;
import org.haven.casemgmt.application.queries.*;
import org.haven.casemgmt.application.dto.CaseDto;
import org.haven.casemgmt.domain.CaseId;
import org.haven.clientprofile.domain.ClientId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for case management operations
 * Following FHIR patterns for resource management
 */
@RestController
@RequestMapping("/cases")
public class CaseController {
    
    private final CaseAppService caseAppService;
    
    public CaseController(CaseAppService caseAppService) {
        this.caseAppService = caseAppService;
    }
    
    @PostMapping
    public ResponseEntity<?> openCase(@Valid @RequestBody OpenCaseCmd cmd) {
        var caseId = caseAppService.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("id", caseId.value(), "resourceType", "Case"));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CaseDto> getCase(@PathVariable UUID id) {
        var query = new GetCaseQuery(new CaseId(id));
        return caseAppService.handle(query)
            .map(caseDto -> ResponseEntity.ok(caseDto))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<CaseDto>> getCases(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "false") boolean requiresAttention) {
        
        if (requiresAttention) {
            var cases = caseAppService.getCasesRequiringAttention();
            return ResponseEntity.ok(cases);
        }
        
        if (clientId != null) {
            var query = new GetCasesByClientQuery(new ClientId(clientId));
            var cases = caseAppService.handle(query);
            return ResponseEntity.ok(cases);
        }
        
        if (assigneeId != null) {
            var query = new GetCasesByAssigneeQuery(assigneeId);
            var cases = caseAppService.handle(query);
            return ResponseEntity.ok(cases);
        }
        
        if (activeOnly) {
            var query = new GetActiveCasesQuery();
            var cases = caseAppService.handle(query);
            return ResponseEntity.ok(cases);
        }
        
        // Default: return empty list or implement general case search
        return ResponseEntity.ok(List.of());
    }
    
    @PutMapping("/{id}/assignment")
    public ResponseEntity<Void> assignCase(
            @PathVariable UUID id,
            @Valid @RequestBody AssignCaseCmd cmd) {
        if (!id.equals(cmd.caseId().value())) {
            return ResponseEntity.badRequest().build();
        }
        caseAppService.handle(cmd);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/notes")
    public ResponseEntity<Void> addNote(
            @PathVariable UUID id,
            @Valid @RequestBody AddCaseNoteCmd cmd) {
        if (!id.equals(cmd.caseId().value())) {
            return ResponseEntity.badRequest().build();
        }
        caseAppService.handle(cmd);
        return ResponseEntity.created(
            URI.create("/cases/" + id + "/notes")).build();
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseStatusCmd cmd) {
        if (!id.equals(cmd.caseId().value())) {
            return ResponseEntity.badRequest().build();
        }
        caseAppService.handle(cmd);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeCase(
            @PathVariable UUID id,
            @Valid @RequestBody CloseCaseCmd cmd) {
        if (!id.equals(cmd.caseId().value())) {
            return ResponseEntity.badRequest().build();
        }
        caseAppService.handle(cmd);
        return ResponseEntity.noContent().build();
    }
}