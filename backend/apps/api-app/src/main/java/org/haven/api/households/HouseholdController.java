package org.haven.api.households;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.clientprofile.application.services.HouseholdCompositionAppService;
import org.haven.clientprofile.application.commands.*;
import org.haven.clientprofile.application.queries.*;
import org.haven.clientprofile.domain.*;
import org.haven.api.households.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/households")
@Tag(name = "Household Management", description = "APIs for managing household compositions and membership")
public class HouseholdController {
    
    private final HouseholdCompositionAppService householdAppService;
    
    public HouseholdController(HouseholdCompositionAppService householdAppService) {
        this.householdAppService = householdAppService;
    }
    
    @PostMapping
    @Operation(summary = "Create a new household composition")
    @ApiResponse(responseCode = "201", description = "Household composition created successfully")
    public ResponseEntity<HouseholdCompositionResponse> createHousehold(
            @Valid @RequestBody CreateHouseholdCompositionRequest request) {
        
        CreateHouseholdCompositionCmd cmd = new CreateHouseholdCompositionCmd(
            new ClientId(request.headOfHouseholdId()),
            request.effectiveDate(),
            request.householdType(),
            request.recordedBy(),
            request.notes()
        );
        
        HouseholdCompositionId compositionId = householdAppService.handle(cmd);
        HouseholdComposition composition = householdAppService.findById(compositionId);
        
        HouseholdCompositionResponse response = HouseholdCompositionResponse.fromDomain(composition);
        
        return ResponseEntity.created(
            URI.create("/api/households/" + compositionId.getValue())
        ).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get household composition by ID")
    public ResponseEntity<HouseholdCompositionResponse> getHousehold(@PathVariable UUID id) {
        HouseholdComposition composition = householdAppService.findById(HouseholdCompositionId.from(id));
        if (composition == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(HouseholdCompositionResponse.fromDomain(composition));
    }
    
    @GetMapping("/{id}/history")
    @Operation(summary = "Get complete membership history for a household")
    public ResponseEntity<HouseholdHistoryResponse> getHouseholdHistory(@PathVariable UUID id) {
        HouseholdComposition composition = householdAppService.findById(HouseholdCompositionId.from(id));
        if (composition == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(HouseholdHistoryResponse.fromDomain(composition));
    }
    
    @GetMapping("/{id}/members")
    @Operation(summary = "Get active household members as of a specific date")
    public ResponseEntity<List<HouseholdMemberResponse>> getActiveMembers(
            @PathVariable UUID id,
            @Parameter(description = "Date to check membership (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        
        HouseholdComposition composition = householdAppService.findById(HouseholdCompositionId.from(id));
        if (composition == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<HouseholdMembershipRecord> activeMembers = composition.getActiveMemberships(asOfDate);
        List<HouseholdMemberResponse> response = activeMembers.stream()
            .map(HouseholdMemberResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to the household")
    @ApiResponse(responseCode = "201", description = "Member added successfully")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddHouseholdMemberRequest request) {
        
        AddHouseholdMemberToCompositionCmd cmd = new AddHouseholdMemberToCompositionCmd(
            HouseholdCompositionId.from(id),
            new ClientId(request.memberId()),
            request.relationship(),
            request.effectiveFrom(),
            request.effectiveTo(),
            request.recordedBy(),
            request.reason()
        );
        
        householdAppService.handle(cmd);
        
        return ResponseEntity.created(
            URI.create("/api/households/" + id + "/members")
        ).build();
    }
    
    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from the household")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody RemoveHouseholdMemberRequest request) {
        
        RemoveHouseholdMemberCmd cmd = new RemoveHouseholdMemberCmd(
            HouseholdCompositionId.from(id),
            new ClientId(memberId),
            request.effectiveDate(),
            request.recordedBy(),
            request.reason()
        );
        
        householdAppService.handle(cmd);
        
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}/members/{memberId}/relationship")
    @Operation(summary = "Update a member's relationship (creates new membership record)")
    public ResponseEntity<Void> updateMemberRelationship(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRelationshipRequest request) {
        
        UpdateMemberRelationshipCmd cmd = new UpdateMemberRelationshipCmd(
            HouseholdCompositionId.from(id),
            new ClientId(memberId),
            request.newRelationship(),
            request.effectiveDate(),
            request.recordedBy(),
            request.reason()
        );
        
        householdAppService.handle(cmd);
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/custody-changes")
    @Operation(summary = "Record a custody change with court documentation")
    public ResponseEntity<Void> recordCustodyChange(
            @PathVariable UUID id,
            @Valid @RequestBody RecordCustodyChangeRequest request) {
        
        RecordCustodyChangeCmd cmd = new RecordCustodyChangeCmd(
            HouseholdCompositionId.from(id),
            new ClientId(request.childId()),
            request.newCustodyRelationship(),
            request.effectiveDate(),
            request.courtOrder(),
            request.recordedBy()
        );
        
        householdAppService.handle(cmd);
        
        return ResponseEntity.created(
            URI.create("/api/households/" + id + "/custody-changes")
        ).build();
    }
    
    // Query endpoints
    
    @GetMapping("/by-head/{headOfHouseholdId}")
    @Operation(summary = "Find households where client is head of household")
    public ResponseEntity<List<HouseholdCompositionResponse>> getHouseholdsByHead(
            @PathVariable UUID headOfHouseholdId) {
        
        List<HouseholdComposition> compositions = householdAppService.findByHeadOfHousehold(
            new ClientId(headOfHouseholdId)
        );
        
        List<HouseholdCompositionResponse> response = compositions.stream()
            .map(HouseholdCompositionResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-member/{memberId}")
    @Operation(summary = "Find households where client is a member")
    public ResponseEntity<List<HouseholdCompositionResponse>> getHouseholdsByMember(
            @PathVariable UUID memberId) {
        
        List<HouseholdComposition> compositions = householdAppService.findByMember(
            new ClientId(memberId)
        );
        
        List<HouseholdCompositionResponse> response = compositions.stream()
            .map(HouseholdCompositionResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active-for-client/{clientId}")
    @Operation(summary = "Find active household for a client as of a specific date")
    public ResponseEntity<HouseholdCompositionResponse> getActiveHouseholdForClient(
            @PathVariable UUID clientId,
            @Parameter(description = "Date to check membership (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        
        HouseholdComposition composition = householdAppService.findActiveHouseholdForClient(
            new ClientId(clientId), asOfDate
        );
        
        if (composition == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(HouseholdCompositionResponse.fromDomain(composition));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search households by various criteria")
    public ResponseEntity<List<HouseholdCompositionResponse>> searchHouseholds(
            @Parameter(description = "Start date for composition date range")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for composition date range")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Household type filter")
            @RequestParam(required = false) HouseholdComposition.HouseholdType householdType,
            @Parameter(description = "Include households with custody changes only")
            @RequestParam(required = false, defaultValue = "false") boolean custodyChangesOnly) {
        
        List<HouseholdComposition> compositions;
        
        if (custodyChangesOnly) {
            compositions = householdAppService.findWithCustodyChanges();
        } else if (startDate != null && endDate != null) {
            compositions = householdAppService.findByCompositionDateRange(startDate, endDate);
        } else if (householdType != null) {
            compositions = householdAppService.findByHouseholdType(householdType);
        } else {
            // Return empty list if no valid search criteria provided
            compositions = List.of();
        }
        
        List<HouseholdCompositionResponse> response = compositions.stream()
            .map(HouseholdCompositionResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/with-changes")
    @Operation(summary = "Find households with membership changes in a date range")
    public ResponseEntity<List<HouseholdCompositionResponse>> getHouseholdsWithChanges(
            @Parameter(description = "Start date for change range", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for change range", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<HouseholdComposition> compositions = householdAppService.findWithChangesInDateRange(
            startDate, endDate
        );
        
        List<HouseholdCompositionResponse> response = compositions.stream()
            .map(HouseholdCompositionResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
}