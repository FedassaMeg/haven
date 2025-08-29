package org.haven.api.enrollments;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.programenrollment.application.services.ProgramEnrollmentAppService;
import org.haven.programenrollment.application.services.ProgramEnrollmentAppService.*;
import org.haven.servicedelivery.application.services.ServiceDeliveryAppService;
import org.haven.servicedelivery.domain.ServiceEpisode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
@Tag(name = "Enrollments", description = "Program enrollment management including Joint TH/RRH transitions")
public class EnrollmentController {
    
    private final ProgramEnrollmentAppService enrollmentAppService;
    private final ServiceDeliveryAppService serviceDeliveryAppService;
    
    public EnrollmentController(ProgramEnrollmentAppService enrollmentAppService,
                               ServiceDeliveryAppService serviceDeliveryAppService) {
        this.enrollmentAppService = enrollmentAppService;
        this.serviceDeliveryAppService = serviceDeliveryAppService;
    }
    
    @Operation(
        summary = "Transition TH enrollment to RRH",
        description = "Transitions a Transitional Housing enrollment to Rapid Re-Housing within a Joint TH/RRH project"
    )
    @PostMapping("/{thEnrollmentId}/transition-to-rrh")
    public ResponseEntity<TransitionToRrhResponse> transitionToRrh(
            @Parameter(description = "TH enrollment ID to transition from")
            @PathVariable UUID thEnrollmentId,
            @Valid @RequestBody TransitionToRrhRequest request) {
        
        try {
            TransitionToRrhCommand command = new TransitionToRrhCommand(
                thEnrollmentId,
                request.rrhProgramId(),
                request.rrhEnrollmentDate(),
                request.residentialMoveInDate()
            );
            
            TransitionToRrhResult result = enrollmentAppService.transitionToRrh(command);
            
            TransitionToRrhResponse response = new TransitionToRrhResponse(
                result.rrhEnrollmentId(),
                result.clientId(),
                result.rrhProgramId(),
                result.enrollmentDate(),
                result.residentialMoveInDate(),
                result.householdId(),
                "TH enrollment successfully transitioned to RRH"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get enrollment chain",
        description = "Retrieves the complete enrollment chain (TH → RRH) for a given enrollment"
    )
    @GetMapping("/{enrollmentId}/chain")
    public ResponseEntity<List<EnrollmentSummaryResponse>> getEnrollmentChain(
            @Parameter(description = "Enrollment ID (can be TH or RRH)")
            @PathVariable UUID enrollmentId) {
        
        try {
            List<EnrollmentSummary> chain = enrollmentAppService.getEnrollmentChain(enrollmentId);
            
            List<EnrollmentSummaryResponse> response = chain.stream()
                .map(summary -> new EnrollmentSummaryResponse(
                    summary.id(),
                    summary.clientId(),
                    summary.programId(),
                    summary.enrollmentDate(),
                    summary.predecessorEnrollmentId(),
                    summary.residentialMoveInDate(),
                    summary.householdId(),
                    summary.status()
                ))
                .toList();
                
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get combined service history",
        description = "Retrieves service history across linked enrollments (TH and RRH)"
    )
    @GetMapping("/{enrollmentId}/combined-services")
    public ResponseEntity<CombinedServicesResponse> getCombinedServices(
            @Parameter(description = "Enrollment ID (can be TH or RRH)")
            @PathVariable UUID enrollmentId) {
        
        try {
            ServiceDeliveryAppService.CombinedServiceHistory history = 
                serviceDeliveryAppService.getCombinedServiceHistory(enrollmentId);
            
            List<ServiceEpisodeSummary> serviceSummaries = history.allServices().stream()
                .map(service -> new ServiceEpisodeSummary(
                    service.getId().value(),
                    UUID.fromString(service.getEnrollmentId()),
                    service.getServiceType().name(),
                    service.getServiceDate(),
                    service.getPrimaryProviderName(),
                    service.getServiceDescription()
                ))
                .toList();
            
            CombinedServicesResponse response = new CombinedServicesResponse(
                enrollmentId,
                serviceSummaries,
                history.totalServiceCount(),
                String.format("Retrieved %d services across %d enrollments", 
                    history.totalServiceCount(), history.enrollmentChain().size())
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            CombinedServicesResponse errorResponse = new CombinedServicesResponse(
                enrollmentId,
                List.of(),
                0,
                "Error retrieving combined service history: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(
        summary = "Update residential move-in date",
        description = "Updates the move-in date for an RRH enrollment"
    )
    @PatchMapping("/{enrollmentId}/move-in-date")
    public ResponseEntity<Void> updateMoveInDate(
            @Parameter(description = "RRH enrollment ID")
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody UpdateMoveInDateRequest request) {
        
        try {
            enrollmentAppService.updateResidentialMoveInDate(enrollmentId, request.moveInDate());
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get client enrollments",
        description = "Retrieves all enrollments for a specific client"
    )
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<EnrollmentSummaryResponse>> getClientEnrollments(
            @Parameter(description = "Client ID")
            @PathVariable UUID clientId) {
        
        try {
            List<EnrollmentSummary> enrollments = enrollmentAppService.getClientEnrollments(clientId);
            
            List<EnrollmentSummaryResponse> response = enrollments.stream()
                .map(summary -> new EnrollmentSummaryResponse(
                    summary.id(),
                    summary.clientId(),
                    summary.programId(),
                    summary.enrollmentDate(),
                    summary.predecessorEnrollmentId(),
                    summary.residentialMoveInDate(),
                    summary.householdId(),
                    summary.status()
                ))
                .toList();
                
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Request/Response records
    public record TransitionToRrhRequest(
        UUID rrhProgramId,
        LocalDate rrhEnrollmentDate,
        LocalDate residentialMoveInDate
    ) {}
    
    public record TransitionToRrhResponse(
        UUID rrhEnrollmentId,
        UUID clientId,
        UUID rrhProgramId,
        LocalDate enrollmentDate,
        LocalDate residentialMoveInDate,
        String householdId,
        String message
    ) {}
    
    public record EnrollmentSummaryResponse(
        UUID id,
        UUID clientId,
        UUID programId,
        LocalDate enrollmentDate,
        UUID predecessorEnrollmentId,
        LocalDate residentialMoveInDate,
        String householdId,
        String status
    ) {}
    
    public record CombinedServicesResponse(
        UUID enrollmentId,
        List<ServiceEpisodeSummary> services,
        int totalServiceCount,
        String message
    ) {}
    
    public record ServiceEpisodeSummary(
        UUID id,
        UUID enrollmentId,
        String serviceType,
        LocalDate serviceDate,
        String providedBy,
        String description
    ) {}
    
    public record UpdateMoveInDateRequest(
        LocalDate moveInDate
    ) {}
}