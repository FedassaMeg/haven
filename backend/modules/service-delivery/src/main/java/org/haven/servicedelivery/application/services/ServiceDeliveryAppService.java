package org.haven.servicedelivery.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.application.services.ProgramEnrollmentAppService;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.servicedelivery.application.commands.*;
import org.haven.servicedelivery.domain.*;
import org.haven.shared.vo.services.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service Delivery Application Service
 * Replaces CaseNote-centric workflow with ServiceEpisode management
 * Handles service creation, tracking, billing, and outcome reporting
 */
@Service
@Transactional
public class ServiceDeliveryAppService {

    private final ServiceEpisodeRepository serviceEpisodeRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramEnrollmentAppService programEnrollmentAppService;
    private final ServiceBillingService billingService;
    private final ServiceReportingService reportingService;

    public ServiceDeliveryAppService(
            ServiceEpisodeRepository serviceEpisodeRepository,
            @Lazy ProgramEnrollmentRepository programEnrollmentRepository,
            @Lazy ProgramEnrollmentAppService programEnrollmentAppService,
            ServiceBillingService billingService,
            ServiceReportingService reportingService) {
        this.serviceEpisodeRepository = serviceEpisodeRepository;
        this.programEnrollmentRepository = programEnrollmentRepository;
        this.programEnrollmentAppService = programEnrollmentAppService;
        this.billingService = billingService;
        this.reportingService = reportingService;
    }

    /**
     * Create a new service episode
     */
    public ServiceEpisodeId createServiceEpisode(CreateServiceEpisodeCmd cmd) {
        // Validate enrollment exists and is active
        var enrollment = programEnrollmentRepository.findById(ProgramEnrollmentId.of(UUID.fromString(cmd.enrollmentId())))
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        if (!enrollment.isActive()) {
            throw new IllegalStateException("Cannot create service for inactive enrollment");
        }

        // Create the service episode
        var episode = ServiceEpisode.create(
            new ClientId(cmd.clientId()),
            cmd.enrollmentId(),
            cmd.programId(),
            cmd.programName(),
            cmd.serviceType(),
            cmd.deliveryMode(),
            cmd.serviceDate(),
            cmd.plannedDurationMinutes(),
            cmd.primaryProviderId(),
            cmd.primaryProviderName(),
            cmd.getFundingSource(),
            cmd.serviceDescription(),
            cmd.isConfidential(),
            cmd.createdBy()
        );

        // Save the episode
        serviceEpisodeRepository.save(episode);

        // Link to enrollment
        enrollment.linkServiceEpisode(
            org.haven.programenrollment.domain.ServiceEpisodeId.of(episode.getId().value()),
            cmd.serviceType(),
            cmd.serviceDate(),
            cmd.primaryProviderName()
        );
        programEnrollmentRepository.save(enrollment);

        return episode.getId();
    }

    /**
     * Start a service session
     */
    public void startService(StartServiceCmd cmd) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(cmd.episodeId()))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.startService(cmd.startTime(), cmd.location());
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Complete a service session
     */
    public void completeService(CompleteServiceCmd cmd) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(cmd.episodeId()))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.completeService(cmd.endTime(), cmd.outcome(), cmd.status(), cmd.notes());
        serviceEpisodeRepository.save(episode);

        // Generate billing if applicable
        if (episode.isBillable() && episode.isCompleted()) {
            billingService.generateBilling(episode);
        }
    }

    /**
     * Add additional provider to service
     */
    public void addProvider(UUID episodeId, String providerId, String providerName, String role) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(episodeId))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.addProvider(providerId, providerName, role);
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Add funding source to service
     */
    public void addFundingSource(UUID episodeId, FundingSource fundingSource, double allocationPercentage) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(episodeId))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.addFundingSource(fundingSource, allocationPercentage);
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Update service outcome
     */
    public void updateOutcome(UUID episodeId, String outcome, String followUpRequired, LocalDate followUpDate) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(episodeId))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.updateOutcome(outcome, followUpRequired, followUpDate);
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Mark service as court ordered
     */
    public void markAsCourtOrdered(UUID episodeId, String courtOrderNumber) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(episodeId))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.markAsCourtOrdered(courtOrderNumber);
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Get service episode by ID
     */
    @Transactional(readOnly = true)
    public java.util.Optional<ServiceEpisode> getServiceEpisode(ServiceEpisodeId episodeId) {
        return serviceEpisodeRepository.findById(episodeId);
    }

    /**
     * Search services with criteria
     */
    @Transactional(readOnly = true)
    public List<ServiceEpisode> searchServices(ServiceSearchCriteria criteria) {
        // For now, return all services and filter (this should be implemented in repository)
        var allServices = getServicesByDateRange(criteria.startDate(), criteria.endDate());
        return allServices.stream()
            .filter(s -> criteria.clientId() == null || s.getClientId().value().equals(criteria.clientId()))
            .filter(s -> criteria.enrollmentId() == null || s.getEnrollmentId().equals(criteria.enrollmentId()))
            .filter(s -> criteria.programId() == null || s.getProgramId().equals(criteria.programId()))
            .filter(s -> criteria.serviceType() == null || s.getServiceType().equals(criteria.serviceType()))
            .filter(s -> criteria.serviceCategory() == null || s.getServiceCategory().equals(criteria.serviceCategory()))
            .filter(s -> criteria.deliveryMode() == null || s.getDeliveryMode().equals(criteria.deliveryMode()))
            .filter(s -> criteria.providerId() == null || s.getPrimaryProviderId().equals(criteria.providerId()))
            .filter(s -> !criteria.confidentialOnly() || s.isConfidential())
            .filter(s -> !criteria.courtOrderedOnly() || s.isCourtOrdered())
            .filter(s -> !criteria.followUpRequired() || s.requiresFollowUp())
            .toList();
    }

    /**
     * Get services for client
     */
    @Transactional(readOnly = true)
    public List<ServiceEpisode> getServicesForClient(UUID clientId) {
        return serviceEpisodeRepository.findByClientId(new ClientId(clientId));
    }

    /**
     * Get services for enrollment
     */
    @Transactional(readOnly = true)
    public List<ServiceEpisode> getServicesForEnrollment(String enrollmentId) {
        return serviceEpisodeRepository.findByEnrollmentId(enrollmentId);
    }

    /**
     * Get services by date range
     */
    @Transactional(readOnly = true)
    public List<ServiceEpisode> getServicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return serviceEpisodeRepository.findByServiceDateBetween(startDate, endDate);
    }

    /**
     * Get services requiring follow-up
     */
    @Transactional(readOnly = true)
    public List<ServiceEpisode> getServicesRequiringFollowUp() {
        return serviceEpisodeRepository.findByFollowUpDateBeforeAndFollowUpRequiredIsNotNull(LocalDate.now());
    }

    /**
     * Get billable services for period
     */
    @Transactional(readOnly = true)
    public List<ServiceEpisode> getBillableServices(LocalDate startDate, LocalDate endDate, FundingSource fundingSource) {
        return serviceEpisodeRepository.findBillableServicesByDateRangeAndFunding(startDate, endDate, fundingSource);
    }

    /**
     * Generate service statistics
     */
    @Transactional(readOnly = true)
    public ServiceStatistics generateStatistics(LocalDate startDate, LocalDate endDate) {
        var services = getServicesByDateRange(startDate, endDate);
        
        int totalServices = services.size();
        int completedServices = (int) services.stream().filter(ServiceEpisode::isCompleted).count();
        int pendingServices = (int) services.stream().filter(s -> !s.isCompleted()).count();
        int cancelledServices = 0; // TODO: implement when status available
        double avgDuration = services.stream()
            .filter(s -> s.getActualDurationMinutes() != null)
            .mapToInt(ServiceEpisode::getActualDurationMinutes)
            .average()
            .orElse(0.0);
        java.math.BigDecimal averageDuration = java.math.BigDecimal.valueOf(avgDuration);
        int uniqueClients = (int) services.stream().map(ServiceEpisode::getClientId).distinct().count();
        int confidentialServices = (int) services.stream().filter(ServiceEpisode::isConfidential).count();
        int courtOrderedServices = (int) services.stream().filter(ServiceEpisode::isCourtOrdered).count();
        int servicesRequiringFollowUp = (int) services.stream().filter(ServiceEpisode::requiresFollowUp).count();
        
        return new ServiceStatistics(
            startDate, endDate, totalServices, completedServices, pendingServices, 
            cancelledServices, averageDuration, uniqueClients, confidentialServices, 
            courtOrderedServices, servicesRequiringFollowUp
        );
    }

    /**
     * Quick service creation for common scenarios
     */
    public ServiceEpisodeId createCrisisInterventionService(
            UUID clientId,
            String enrollmentId, 
            String programId,
            String providerId,
            String providerName,
            boolean isConfidential,
            String createdBy) {
        
        var cmd = new CreateServiceEpisodeCmd(
            clientId,
            enrollmentId,
            programId,
            "Crisis Response Program",
            ServiceType.CRISIS_INTERVENTION,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            60, // 1 hour planned
            providerId,
            providerName,
            "DOJ-VAWA", // VAWA funding typically for crisis services
            "DOJ Violence Against Women Act",
            null, // No specific grant number
            "Crisis intervention service",
            isConfidential,
            createdBy
        );
        
        return createServiceEpisode(cmd);
    }

    /**
     * Quick counseling session creation
     */
    public ServiceEpisodeId createCounselingSession(
            UUID clientId,
            String enrollmentId,
            String programId,
            ServiceType counselingType,
            String providerId,
            String providerName,
            String createdBy) {
        
        var cmd = new CreateServiceEpisodeCmd(
            clientId,
            enrollmentId,
            programId,
            "Counseling Program",
            counselingType,
            ServiceDeliveryMode.IN_PERSON,
            LocalDate.now(),
            50, // 50 minutes standard
            providerId,
            providerName,
            "CAL-OES", // Cal OES funding for counseling
            "California Office of Emergency Services",
            null,
            counselingType.getDescription() + " session",
            true, // Counseling is confidential
            createdBy
        );
        
        return createServiceEpisode(cmd);
    }

    /**
     * Quick case management contact
     */
    public ServiceEpisodeId createCaseManagementContact(
            UUID clientId,
            String enrollmentId,
            String programId,
            ServiceDeliveryMode deliveryMode,
            String providerId,
            String providerName,
            String description,
            String createdBy) {
        
        var cmd = new CreateServiceEpisodeCmd(
            clientId,
            enrollmentId,
            programId,
            "Case Management Program",
            ServiceType.CASE_MANAGEMENT,
            deliveryMode,
            LocalDate.now(),
            30, // 30 minutes planned
            providerId,
            providerName,
            "HUD-COC", // HUD funding for case management
            "HUD Continuum of Care",
            null,
            description,
            false, // Case management typically not confidential
            createdBy
        );
        
        return createServiceEpisode(cmd);
    }

    /**
     * Get combined service history across linked enrollments (Joint TH/RRH)
     * Retrieves services from both the TH and RRH enrollments in a transition chain
     */
    @Transactional(readOnly = true)
    public CombinedServiceHistory getCombinedServiceHistory(UUID enrollmentId) {
        // Find the enrollment chain
        var enrollment = programEnrollmentRepository.findById(ProgramEnrollmentId.of(enrollmentId))
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        
        List<UUID> chainEnrollmentIds = getEnrollmentChain(enrollmentId);
        
        // Get services for all enrollments in the chain
        List<ServiceEpisode> allServices = chainEnrollmentIds.stream()
            .flatMap(id -> serviceEpisodeRepository.findByEnrollmentId(id.toString()).stream())
            .sorted((s1, s2) -> s1.getServiceDate().compareTo(s2.getServiceDate()))
            .toList();
        
        // Calculate statistics
        int totalServiceCount = allServices.size();
        LocalDate firstServiceDate = allServices.isEmpty() ? null : allServices.get(0).getServiceDate();
        LocalDate lastServiceDate = allServices.isEmpty() ? null : allServices.get(allServices.size() - 1).getServiceDate();
        
        // Group by enrollment
        var servicesByEnrollment = allServices.stream()
            .collect(java.util.stream.Collectors.groupingBy(ServiceEpisode::getEnrollmentId));
        
        // Create enrollment service summaries
        List<EnrollmentServiceSummary> enrollmentSummaries = chainEnrollmentIds.stream()
            .map(id -> {
                List<ServiceEpisode> enrollmentServices = servicesByEnrollment.getOrDefault(id.toString(), List.of());
                return new EnrollmentServiceSummary(
                    id,
                    enrollmentServices.size(),
                    enrollmentServices.isEmpty() ? null : enrollmentServices.get(0).getServiceDate(),
                    enrollmentServices.isEmpty() ? null : enrollmentServices.get(enrollmentServices.size() - 1).getServiceDate(),
                    enrollmentServices
                );
            })
            .toList();
        
        return new CombinedServiceHistory(
            enrollmentId,
            chainEnrollmentIds,
            totalServiceCount,
            firstServiceDate,
            lastServiceDate,
            enrollmentSummaries,
            allServices
        );
    }
    
    /**
     * Get the enrollment chain for a given enrollment ID
     * Returns all related enrollments (TH -> RRH transitions)
     */
    private List<UUID> getEnrollmentChain(UUID enrollmentId) {
        try {
            // Use the program enrollment app service to get the complete enrollment chain
            return programEnrollmentAppService.getEnrollmentChain(enrollmentId)
                .stream()
                .map(ProgramEnrollmentAppService.EnrollmentSummary::id)
                .toList();
        } catch (Exception e) {
            // Fallback to single enrollment
            return List.of(enrollmentId);
        }
    }
    
    // Record classes for combined service history
    public record CombinedServiceHistory(
        UUID rootEnrollmentId,
        List<UUID> enrollmentChain,
        int totalServiceCount,
        LocalDate firstServiceDate,
        LocalDate lastServiceDate,
        List<EnrollmentServiceSummary> enrollmentSummaries,
        List<ServiceEpisode> allServices
    ) {}
    
    public record EnrollmentServiceSummary(
        UUID enrollmentId,
        int serviceCount,
        LocalDate firstServiceDate,
        LocalDate lastServiceDate,
        List<ServiceEpisode> services
    ) {}
}
