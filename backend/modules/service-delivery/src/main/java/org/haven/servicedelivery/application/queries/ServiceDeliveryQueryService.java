package org.haven.servicedelivery.application.queries;

import org.haven.clientprofile.domain.ClientId;
import org.haven.servicedelivery.application.services.ServiceSearchCriteria;
import org.haven.servicedelivery.domain.ServiceEpisode;
import org.haven.servicedelivery.domain.ServiceEpisodeId;
import org.haven.servicedelivery.domain.ServiceEpisodeRepository;
import org.haven.shared.vo.services.FundingSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query Service for Service Delivery
 * Handles all read operations and returns DTOs
 * Separated from command handling for CQRS
 */
public class ServiceDeliveryQueryService {

    private final ServiceEpisodeRepository serviceEpisodeRepository;

    public ServiceDeliveryQueryService(ServiceEpisodeRepository serviceEpisodeRepository) {
        this.serviceEpisodeRepository = serviceEpisodeRepository;
    }

    /**
     * Get service episode by ID
     */
    public Optional<ServiceEpisodeDTO> getServiceEpisode(ServiceEpisodeId episodeId) {
        return serviceEpisodeRepository.findById(episodeId)
            .map(this::toDTO);
    }

    /**
     * Search services with criteria
     */
    public List<ServiceEpisodeDTO> searchServices(ServiceSearchCriteria criteria) {
        var allServices = getServicesByDateRange(criteria.startDate(), criteria.endDate());
        return allServices.stream()
            .filter(s -> criteria.clientId() == null || s.clientId().equals(criteria.clientId()))
            .filter(s -> criteria.enrollmentId() == null || s.enrollmentId().equals(criteria.enrollmentId()))
            .filter(s -> criteria.programId() == null || s.programId().equals(criteria.programId()))
            .filter(s -> criteria.serviceType() == null || s.serviceType().equals(criteria.serviceType()))
            .filter(s -> criteria.serviceCategory() == null || s.serviceCategory().equals(criteria.serviceCategory()))
            .filter(s -> criteria.deliveryMode() == null || s.deliveryMode().equals(criteria.deliveryMode()))
            .filter(s -> criteria.providerId() == null || s.primaryProviderId().equals(criteria.providerId()))
            .filter(s -> !criteria.confidentialOnly() || s.isConfidential())
            .filter(s -> !criteria.courtOrderedOnly() || s.isCourtOrdered())
            .filter(s -> !criteria.followUpRequired() || s.requiresFollowUp())
            .toList();
    }

    /**
     * Get services for client
     */
    public List<ServiceEpisodeDTO> getServicesForClient(UUID clientId) {
        return serviceEpisodeRepository.findByClientId(new ClientId(clientId))
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Get services for enrollment
     */
    public List<ServiceEpisodeDTO> getServicesForEnrollment(String enrollmentId) {
        return serviceEpisodeRepository.findByEnrollmentId(enrollmentId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Get services by date range
     */
    public List<ServiceEpisodeDTO> getServicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return serviceEpisodeRepository.findByServiceDateBetween(startDate, endDate)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Get services requiring follow-up
     */
    public List<ServiceEpisodeDTO> getServicesRequiringFollowUp() {
        return serviceEpisodeRepository.findByFollowUpDateBeforeAndFollowUpRequiredIsNotNull(LocalDate.now())
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Get billable services for period
     */
    public List<ServiceEpisodeDTO> getBillableServices(LocalDate startDate, LocalDate endDate, FundingSource fundingSource) {
        return serviceEpisodeRepository.findBillableServicesByDateRangeAndFunding(startDate, endDate, fundingSource)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Get combined service history across enrollment chain
     */
    public CombinedServiceHistoryDTO getCombinedServiceHistory(List<UUID> enrollmentChain) {
        // Get services for all enrollments in the chain
        List<ServiceEpisodeDTO> allServices = enrollmentChain.stream()
            .flatMap(id -> serviceEpisodeRepository.findByEnrollmentId(id.toString()).stream())
            .map(this::toDTO)
            .sorted((s1, s2) -> s1.serviceDate().compareTo(s2.serviceDate()))
            .toList();

        // Calculate statistics
        int totalServiceCount = allServices.size();
        LocalDate firstServiceDate = allServices.isEmpty() ? null : allServices.get(0).serviceDate();
        LocalDate lastServiceDate = allServices.isEmpty() ? null : allServices.get(allServices.size() - 1).serviceDate();

        // Group by enrollment
        var servicesByEnrollment = allServices.stream()
            .collect(java.util.stream.Collectors.groupingBy(ServiceEpisodeDTO::enrollmentId));

        // Create enrollment service summaries
        List<EnrollmentServiceSummaryDTO> enrollmentSummaries = enrollmentChain.stream()
            .map(id -> {
                List<ServiceEpisodeDTO> enrollmentServices = servicesByEnrollment.getOrDefault(id.toString(), List.of());
                return new EnrollmentServiceSummaryDTO(
                    id,
                    enrollmentServices.size(),
                    enrollmentServices.isEmpty() ? null : enrollmentServices.get(0).serviceDate(),
                    enrollmentServices.isEmpty() ? null : enrollmentServices.get(enrollmentServices.size() - 1).serviceDate(),
                    enrollmentServices
                );
            })
            .toList();

        return new CombinedServiceHistoryDTO(
            enrollmentChain.get(0), // Root enrollment ID
            enrollmentChain,
            totalServiceCount,
            firstServiceDate,
            lastServiceDate,
            enrollmentSummaries,
            allServices
        );
    }

    /**
     * Convert domain entity to DTO
     */
    private ServiceEpisodeDTO toDTO(ServiceEpisode episode) {
        return new ServiceEpisodeDTO(
            episode.getId().value(),
            episode.getClientId().value(),
            episode.getEnrollmentId(),
            episode.getProgramId(),
            episode.getProgramName(),
            episode.getServiceType(),
            episode.getServiceCategory(),
            episode.getDeliveryMode(),
            episode.getServiceDate(),
            episode.getPlannedDurationMinutes(),
            episode.getActualDurationMinutes(),
            episode.getPrimaryProviderId(),
            episode.getPrimaryProviderName(),
            episode.isConfidential(),
            episode.isCourtOrdered(),
            episode.requiresFollowUp(),
            episode.isCompleted(),
            episode.isBillable()
        );
    }
}
