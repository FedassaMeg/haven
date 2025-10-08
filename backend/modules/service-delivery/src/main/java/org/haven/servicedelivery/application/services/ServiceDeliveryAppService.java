package org.haven.servicedelivery.application.services;

import org.haven.programenrollment.application.services.ProgramEnrollmentAppService;
import org.haven.servicedelivery.application.commands.*;
import org.haven.servicedelivery.application.handlers.ServiceDeliveryCommandHandler;
import org.haven.servicedelivery.application.queries.*;
import org.haven.servicedelivery.application.reporting.ServiceStatisticsCalculator;
import org.haven.servicedelivery.domain.*;
import org.haven.shared.vo.services.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Delivery Application Service (Facade)
 * Thin facade delegating to specialized handlers and services
 * Maintains backward compatibility while enforcing CQRS separation
 */
@Service
@Transactional
public class ServiceDeliveryAppService {

    private final ServiceDeliveryCommandHandler commandHandler;
    private final ServiceDeliveryQueryService queryService;
    private final ServiceStatisticsCalculator statisticsCalculator;
    private final ServiceTemplateFactory templateFactory;
    private final ProgramEnrollmentAppService enrollmentAppService;

    public ServiceDeliveryAppService(
            ServiceDeliveryCommandHandler commandHandler,
            ServiceDeliveryQueryService queryService,
            ServiceStatisticsCalculator statisticsCalculator,
            ServiceTemplateFactory templateFactory,
            @Lazy ProgramEnrollmentAppService enrollmentAppService) {
        this.commandHandler = commandHandler;
        this.queryService = queryService;
        this.statisticsCalculator = statisticsCalculator;
        this.templateFactory = templateFactory;
        this.enrollmentAppService = enrollmentAppService;
    }

    // ========== Command Operations ==========

    public ServiceEpisodeId createServiceEpisode(CreateServiceEpisodeCmd cmd) {
        return commandHandler.handle(cmd);
    }

    public void startService(StartServiceCmd cmd) {
        commandHandler.handle(cmd);
    }

    public void completeService(CompleteServiceCmd cmd) {
        commandHandler.handle(cmd);
    }

    public void addProvider(UUID episodeId, String providerId, String providerName, String role) {
        commandHandler.addProvider(episodeId, providerId, providerName, role);
    }

    public void addFundingSource(UUID episodeId, FundingSource fundingSource, double allocationPercentage) {
        commandHandler.addFundingSource(episodeId, fundingSource, allocationPercentage);
    }

    public void updateOutcome(UUID episodeId, String outcome, String followUpRequired, LocalDate followUpDate) {
        commandHandler.updateOutcome(episodeId, outcome, followUpRequired, followUpDate);
    }

    public void markAsCourtOrdered(UUID episodeId, String courtOrderNumber) {
        commandHandler.markAsCourtOrdered(episodeId, courtOrderNumber);
    }

    // ========== Query Operations ==========

    @Transactional(readOnly = true)
    public Optional<ServiceEpisodeDTO> getServiceEpisode(ServiceEpisodeId episodeId) {
        return queryService.getServiceEpisode(episodeId);
    }

    @Transactional(readOnly = true)
    public List<ServiceEpisodeDTO> searchServices(ServiceSearchCriteria criteria) {
        return queryService.searchServices(criteria);
    }

    @Transactional(readOnly = true)
    public List<ServiceEpisodeDTO> getServicesForClient(UUID clientId) {
        return queryService.getServicesForClient(clientId);
    }

    @Transactional(readOnly = true)
    public List<ServiceEpisodeDTO> getServicesForEnrollment(String enrollmentId) {
        return queryService.getServicesForEnrollment(enrollmentId);
    }

    @Transactional(readOnly = true)
    public List<ServiceEpisodeDTO> getServicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return queryService.getServicesByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ServiceEpisodeDTO> getServicesRequiringFollowUp() {
        return queryService.getServicesRequiringFollowUp();
    }

    @Transactional(readOnly = true)
    public List<ServiceEpisodeDTO> getBillableServices(LocalDate startDate, LocalDate endDate, FundingSource fundingSource) {
        return queryService.getBillableServices(startDate, endDate, fundingSource);
    }

    @Transactional(readOnly = true)
    public CombinedServiceHistoryDTO getCombinedServiceHistory(UUID enrollmentId) {
        List<UUID> enrollmentChain = getEnrollmentChain(enrollmentId);
        return queryService.getCombinedServiceHistory(enrollmentChain);
    }

    // ========== Reporting Operations ==========

    @Transactional(readOnly = true)
    public ServiceStatistics generateStatistics(LocalDate startDate, LocalDate endDate) {
        var services = queryService.getServicesByDateRange(startDate, endDate);
        return statisticsCalculator.calculateStatistics(startDate, endDate, services);
    }

    // ========== Template Factory Operations ==========

    public ServiceEpisodeId createCrisisInterventionService(
            UUID clientId,
            String enrollmentId,
            String programId,
            String providerId,
            String providerName,
            boolean isConfidential,
            String createdBy) {
        var cmd = templateFactory.createCrisisInterventionService(
            clientId, enrollmentId, programId, providerId, providerName, isConfidential, createdBy);
        return commandHandler.handle(cmd);
    }

    public ServiceEpisodeId createCounselingSession(
            UUID clientId,
            String enrollmentId,
            String programId,
            ServiceType counselingType,
            String providerId,
            String providerName,
            String createdBy) {
        var cmd = templateFactory.createCounselingSession(
            clientId, enrollmentId, programId, counselingType, providerId, providerName, createdBy);
        return commandHandler.handle(cmd);
    }

    public ServiceEpisodeId createCaseManagementContact(
            UUID clientId,
            String enrollmentId,
            String programId,
            ServiceDeliveryMode deliveryMode,
            String providerId,
            String providerName,
            String description,
            String createdBy) {
        var cmd = templateFactory.createCaseManagementContact(
            clientId, enrollmentId, programId, deliveryMode, providerId, providerName, description, createdBy);
        return commandHandler.handle(cmd);
    }

    // ========== Private Helpers ==========

    private List<UUID> getEnrollmentChain(UUID enrollmentId) {
        try {
            return enrollmentAppService.getEnrollmentChain(enrollmentId)
                .stream()
                .map(ProgramEnrollmentAppService.EnrollmentSummary::id)
                .toList();
        } catch (Exception e) {
            return List.of(enrollmentId);
        }
    }
}
