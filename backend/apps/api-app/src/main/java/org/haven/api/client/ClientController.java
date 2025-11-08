package org.haven.api.client;

import io.swagger.v3.oas.annotations.Operation;
import org.haven.clientprofile.application.services.ClientAppService;
import org.haven.clientprofile.application.commands.*;
import org.haven.clientprofile.application.queries.*;
import org.haven.clientprofile.application.dto.ClientDto;
import org.haven.clientprofile.domain.ClientId;
import org.haven.api.client.dto.CreateClientRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for client management operations
 * Following FHIR patterns for resource management
 */
@RestController
@RequestMapping("/clients")
public class ClientController {
    
    private final ClientAppService clientAppService;
    
    public ClientController(ClientAppService clientAppService) {
        this.clientAppService = clientAppService;
    }

    @PostMapping
    public ResponseEntity<?> createClient(@Valid @RequestBody CreateClientRequest request) {
        // Map FHIR request to internal command
        var humanName = request.name().toValueObject();

        // Collect all addresses
        var addresses = (request.addresses() != null)
            ? request.addresses().stream()
                .filter(a -> a != null)
                .map(CreateClientRequest.AddressDto::toValueObject)
                .toList()
            : null;

        // Collect all telecoms
        var telecoms = (request.telecoms() != null)
            ? request.telecoms().stream()
                .filter(t -> t != null)
                .map(CreateClientRequest.ContactPointDto::toValueObject)
                .toList()
            : null;

        // Single command with all data - single save operation
        var cmd = new CreateClientCmd(
            humanName.getFirstName(),
            humanName.getLastName(),
            request.gender(),
            request.birthDate(),
            addresses,
            telecoms
        );

        var clientId = clientAppService.handle(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("id", clientId.value(), "resourceType", "Client"));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getClient(@PathVariable UUID id) {
        var query = new GetClientQuery(new ClientId(id));
        return clientAppService.handle(query)
            .map(client -> ResponseEntity.ok(client))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<ClientDto>> searchClients(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        var query = new SearchClientsQuery(name, activeOnly);
        var clients = clientAppService.handle(query);
        return ResponseEntity.ok(clients);
    }
    
    @PutMapping("/{id}/demographics")
    public ResponseEntity<Void> updateDemographics(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDemographicsCmd cmd) {
        // Ensure path ID matches command ID
        if (!id.equals(cmd.clientId().value())) {
            return ResponseEntity.badRequest().build();
        }
        clientAppService.handle(cmd);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/addresses")
    public ResponseEntity<Void> addAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddClientAddressCmd cmd) {
        if (!id.equals(cmd.clientId().value())) {
            return ResponseEntity.badRequest().build();
        }
        clientAppService.handle(cmd);
        return ResponseEntity.created(
            URI.create("/api/clients/" + id + "/addresses")).build();
    }
    
    @PostMapping("/{id}/telecoms")
    public ResponseEntity<Void> addTelecom(
            @PathVariable UUID id,
            @Valid @RequestBody AddClientTelecomCmd cmd) {
        if (!id.equals(cmd.clientId().value())) {
            return ResponseEntity.badRequest().build();
        }
        clientAppService.handle(cmd);
        return ResponseEntity.created(
            URI.create("/api/clients/" + id + "/telecoms")).build();
    }
    
    @PostMapping("/{id}/household-members")
    public ResponseEntity<Void> addHouseholdMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddHouseholdMemberCmd cmd) {
        if (!id.equals(cmd.clientId().value())) {
            return ResponseEntity.badRequest().build();
        }
        clientAppService.handle(cmd);
        return ResponseEntity.created(
            URI.create("/api/clients/" + id + "/household-members")).build();
    }
    
    @Operation(
        summary = "Get HUD element coverage for client endpoints",
        description = "Returns which HUD data elements are covered by client API endpoints"
    )
    @GetMapping("/hud-coverage")
    public ResponseEntity<ClientHudCoverageResponse> getHudCoverage() {
        List<HudElementCoverage> coverages = List.of(
            new HudElementCoverage("3.01", "Personal ID", "/clients/{id}", "GET", "personalId", true),
            new HudElementCoverage("3.02", "First Name", "/clients/{id}", "GET", "name.given[0]", true),
            new HudElementCoverage("3.03", "Last Name", "/clients/{id}", "GET", "name.family", true),
            new HudElementCoverage("3.04", "Race", "/clients/{id}", "GET", "race", true),
            new HudElementCoverage("3.05", "Ethnicity", "/clients/{id}", "GET", "ethnicity", false),
            new HudElementCoverage("3.06", "Gender", "/clients/{id}", "GET", "gender", true),
            new HudElementCoverage("3.07", "Veteran Status", "/clients/{id}", "GET", "veteranStatus", true)
        );
        
        ClientHudCoverageResponse response = new ClientHudCoverageResponse(
            "Client API",
            coverages,
            coverages.stream().mapToLong(c -> c.implemented() ? 1 : 0).sum(),
            coverages.size()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // HUD Coverage DTOs
    public record ClientHudCoverageResponse(
        String apiName,
        List<HudElementCoverage> elements,
        long implementedCount,
        long totalCount
    ) {}
    
    public record HudElementCoverage(
        String hudId,
        String elementName,
        String route,
        String method,
        String fieldName,
        boolean implemented
    ) {}
}
