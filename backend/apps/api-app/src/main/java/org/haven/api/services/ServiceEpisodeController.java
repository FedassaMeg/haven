package org.haven.api.services;

import org.haven.api.services.dto.*;
import org.haven.api.services.dto.QuickServiceRequests.*;
import org.haven.servicedelivery.application.commands.*;
import org.haven.servicedelivery.application.services.*;
import org.haven.servicedelivery.domain.*;
import org.haven.shared.vo.services.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ServiceEpisode REST API Controller
 * Provides endpoints for service delivery management
 */
@RestController
@RequestMapping("/api/v1/service-episodes")
@PreAuthorize("hasRole('CASE_MANAGER') or hasRole('COUNSELOR') or hasRole('ADVOCATE')")
public class ServiceEpisodeController {

    private final ServiceDeliveryAppService serviceDeliveryService;
    private final ServiceReportingService reportingService;
    private final ServiceBillingService billingService;

    public ServiceEpisodeController(
            ServiceDeliveryAppService serviceDeliveryService,
            ServiceReportingService reportingService,
            ServiceBillingService billingService) {
        this.serviceDeliveryService = serviceDeliveryService;
        this.reportingService = reportingService;
        this.billingService = billingService;
    }

    /**
     * Create a new service episode
     */
    @PostMapping
    public ResponseEntity<CreateServiceEpisodeResponse> createServiceEpisode(
            @Valid @RequestBody CreateServiceEpisodeRequest request) {
        
        var cmd = new CreateServiceEpisodeCmd(
            request.clientId(),
            request.enrollmentId(),
            request.programId(),
            request.programName(),
            request.serviceType(),
            request.deliveryMode(),
            request.serviceDate(),
            request.plannedDurationMinutes(),
            request.primaryProviderId(),
            request.primaryProviderName(),
            request.funderId(),
            request.funderName(),
            request.grantNumber(),
            request.serviceDescription(),
            request.isConfidential(),
            getCurrentUserId()
        );

        var episodeId = serviceDeliveryService.createServiceEpisode(cmd);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreateServiceEpisodeResponse(episodeId.value()));
    }

    /**
     * Start a service session
     */
    @PostMapping("/{episodeId}/start")
    public ResponseEntity<Void> startService(
            @PathVariable UUID episodeId,
            @Valid @RequestBody StartServiceRequest request) {
        
        var cmd = new StartServiceCmd(episodeId, request.startTime(), request.location());
        serviceDeliveryService.startService(cmd);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Complete a service session
     */
    @PostMapping("/{episodeId}/complete")
    public ResponseEntity<Void> completeService(
            @PathVariable UUID episodeId,
            @Valid @RequestBody CompleteServiceRequest request) {
        
        var cmd = new CompleteServiceCmd(
            episodeId, 
            request.endTime(), 
            request.outcome(), 
            request.status(), 
            request.notes()
        );
        serviceDeliveryService.completeService(cmd);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get service episode by ID
     */
    @GetMapping("/{episodeId}")
    @PreAuthorize("@serviceEpisodeSecurityService.canViewServiceEpisode(#episodeId, authentication.name)")
    public ResponseEntity<ServiceEpisodeResponse> getServiceEpisode(@PathVariable UUID episodeId) {
        var episode = serviceDeliveryService.getServiceEpisode(ServiceEpisodeId.of(episodeId))
            .orElseThrow(() -> new ServiceEpisodeNotFoundException(episodeId));
        
        return ResponseEntity.ok(ServiceEpisodeResponse.fromDomain(episode));
    }

    /**
     * Get services for client
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("@clientSecurityService.canViewClient(#clientId, authentication.name)")
    public ResponseEntity<List<ServiceEpisodeResponse>> getServicesForClient(@PathVariable UUID clientId) {
        var services = serviceDeliveryService.getServicesForClient(clientId);
        var responses = services.stream()
            .map(ServiceEpisodeResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get services for enrollment
     */
    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("@enrollmentSecurityService.canViewEnrollment(#enrollmentId, authentication.name)")
    public ResponseEntity<List<ServiceEpisodeResponse>> getServicesForEnrollment(@PathVariable String enrollmentId) {
        var services = serviceDeliveryService.getServicesForEnrollment(enrollmentId);
        var responses = services.stream()
            .map(ServiceEpisodeResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Search services with filters
     */
    @GetMapping("/search")
    public ResponseEntity<List<ServiceEpisodeResponse>> searchServices(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String enrollmentId,
            @RequestParam(required = false) String programId,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) ServiceCategory serviceCategory,
            @RequestParam(required = false) ServiceDeliveryMode deliveryMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String providerId,
            @RequestParam(defaultValue = "false") boolean confidentialOnly,
            @RequestParam(defaultValue = "false") boolean courtOrderedOnly,
            @RequestParam(defaultValue = "false") boolean followUpRequired) {
        
        var services = serviceDeliveryService.searchServices(new ServiceSearchCriteria(
            clientId,
            enrollmentId,
            programId,
            serviceType,
            serviceCategory,
            deliveryMode,
            startDate,
            endDate,
            providerId,
            confidentialOnly,
            courtOrderedOnly,
            followUpRequired
        ));
        
        var responses = services.stream()
            .map(ServiceEpisodeResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get services requiring follow-up
     */
    @GetMapping("/follow-up")
    public ResponseEntity<List<ServiceEpisodeResponse>> getServicesRequiringFollowUp() {
        var services = serviceDeliveryService.getServicesRequiringFollowUp();
        var responses = services.stream()
            .map(ServiceEpisodeResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Update service outcome
     */
    @PutMapping("/{episodeId}/outcome")
    public ResponseEntity<Void> updateOutcome(
            @PathVariable UUID episodeId,
            @Valid @RequestBody UpdateOutcomeRequest request) {
        
        serviceDeliveryService.updateOutcome(
            episodeId, 
            request.outcome(), 
            request.followUpRequired(), 
            request.followUpDate()
        );
        
        return ResponseEntity.ok().build();
    }

    /**
     * Add provider to service
     */
    @PostMapping("/{episodeId}/providers")
    public ResponseEntity<Void> addProvider(
            @PathVariable UUID episodeId,
            @Valid @RequestBody AddProviderRequest request) {
        
        serviceDeliveryService.addProvider(
            episodeId, 
            request.providerId(), 
            request.providerName(), 
            request.role()
        );
        
        return ResponseEntity.ok().build();
    }

    /**
     * Add funding source to service
     */
    @PostMapping("/{episodeId}/funding")
    public ResponseEntity<Void> addFundingSource(
            @PathVariable UUID episodeId,
            @Valid @RequestBody AddFundingSourceRequest request) {
        
        var fundingSource = createFundingSource(request);
        serviceDeliveryService.addFundingSource(episodeId, fundingSource, request.allocationPercentage());
        
        return ResponseEntity.ok().build();
    }

    /**
     * Mark service as court ordered
     */
    @PostMapping("/{episodeId}/court-order")
    public ResponseEntity<Void> markAsCourtOrdered(
            @PathVariable UUID episodeId,
            @Valid @RequestBody CourtOrderRequest request) {
        
        serviceDeliveryService.markAsCourtOrdered(episodeId, request.courtOrderNumber());
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get service statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<ServiceStatisticsResponse> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        var statistics = serviceDeliveryService.generateStatistics(startDate, endDate);
        return ResponseEntity.ok(ServiceStatisticsResponse.fromDomain(statistics));
    }

    /**
     * Quick service creation methods
     */
    @PostMapping("/quick/crisis-intervention")
    public ResponseEntity<CreateServiceEpisodeResponse> createCrisisIntervention(
            @Valid @RequestBody QuickCrisisServiceRequest request) {
        
        var episodeId = serviceDeliveryService.createCrisisInterventionService(
            request.clientId(),
            request.enrollmentId(),
            request.programId(),
            request.providerId(),
            request.providerName(),
            request.isConfidential(),
            getCurrentUserId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreateServiceEpisodeResponse(episodeId.value()));
    }

    @PostMapping("/quick/counseling")
    public ResponseEntity<CreateServiceEpisodeResponse> createCounselingSession(
            @Valid @RequestBody QuickCounselingServiceRequest request) {
        
        var episodeId = serviceDeliveryService.createCounselingSession(
            request.clientId(),
            request.enrollmentId(),
            request.programId(),
            request.serviceType(),
            request.providerId(),
            request.providerName(),
            getCurrentUserId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreateServiceEpisodeResponse(episodeId.value()));
    }

    @PostMapping("/quick/case-management")
    public ResponseEntity<CreateServiceEpisodeResponse> createCaseManagementContact(
            @Valid @RequestBody QuickCaseManagementServiceRequest request) {
        
        var episodeId = serviceDeliveryService.createCaseManagementContact(
            request.clientId(),
            request.enrollmentId(),
            request.programId(),
            request.deliveryMode(),
            request.providerId(),
            request.providerName(),
            request.description(),
            getCurrentUserId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreateServiceEpisodeResponse(episodeId.value()));
    }

    /**
     * Service types lookup
     */
    @GetMapping("/service-types")
    public ResponseEntity<List<ServiceTypeResponse>> getServiceTypes() {
        var serviceTypes = List.of(ServiceType.values()).stream()
            .map(ServiceTypeResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(serviceTypes);
    }

    /**
     * Delivery modes lookup
     */
    @GetMapping("/delivery-modes")
    public ResponseEntity<List<ServiceDeliveryModeResponse>> getDeliveryModes() {
        var deliveryModes = List.of(ServiceDeliveryMode.values()).stream()
            .map(ServiceDeliveryModeResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(deliveryModes);
    }

    /**
     * Funding sources lookup
     */
    @GetMapping("/funding-sources")
    public ResponseEntity<List<FundingSourceResponse>> getFundingSources() {
        var fundingSources = List.of(
            FundingSourceResponse.fromDomain(FundingSource.hudCoc("", "")),
            FundingSourceResponse.fromDomain(FundingSource.vawa("", "")),
            FundingSourceResponse.fromDomain(FundingSource.calOes("", "")),
            FundingSourceResponse.fromDomain(FundingSource.fema("", "")),
            FundingSourceResponse.fromDomain(FundingSource.hopwa("", "")),
            FundingSourceResponse.fromDomain(FundingSource.noFunding())
        );
        
        return ResponseEntity.ok(fundingSources);
    }

    private String getCurrentUserId() {
        // Get from Spring Security context
        return "current-user-id"; // Placeholder
    }

    private FundingSource createFundingSource(AddFundingSourceRequest request) {
        return switch (request.funderId()) {
            case "HUD-COC" -> FundingSource.hudCoc(request.grantNumber(), request.programName());
            case "DOJ-VAWA" -> FundingSource.vawa(request.grantNumber(), request.programName());
            case "CAL-OES" -> FundingSource.calOes(request.grantNumber(), request.programName());
            case "FEMA-ESG" -> FundingSource.fema(request.grantNumber(), request.programName());
            case "HUD-HOPWA" -> FundingSource.hopwa(request.grantNumber(), request.programName());
            case "NONE" -> FundingSource.noFunding();
            default -> FundingSource.privateFoundation(
                request.funderId(), 
                request.funderName(), 
                request.grantNumber(), 
                request.programName()
            );
        };
    }
}