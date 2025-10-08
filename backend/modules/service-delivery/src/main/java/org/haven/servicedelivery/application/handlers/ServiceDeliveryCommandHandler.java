package org.haven.servicedelivery.application.handlers;

import org.haven.clientprofile.domain.ClientId;
import org.haven.servicedelivery.application.commands.*;
import org.haven.servicedelivery.application.services.ServiceBillingService;
import org.haven.servicedelivery.domain.*;
import org.haven.shared.events.EventPublisher;
import org.haven.shared.events.ServiceEpisodeLinked;
import org.haven.shared.vo.services.FundingSource;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command Handler for Service Delivery
 * Handles all write operations for service episodes
 * Pure command orchestration without queries or statistics
 */
public class ServiceDeliveryCommandHandler {

    private final ServiceEpisodeRepository serviceEpisodeRepository;
    private final EventPublisher eventPublisher;
    private final ServiceBillingService billingService;

    public ServiceDeliveryCommandHandler(
            ServiceEpisodeRepository serviceEpisodeRepository,
            EventPublisher eventPublisher,
            ServiceBillingService billingService) {
        this.serviceEpisodeRepository = serviceEpisodeRepository;
        this.eventPublisher = eventPublisher;
        this.billingService = billingService;
    }

    /**
     * Handle create service episode command
     */
    public ServiceEpisodeId handle(CreateServiceEpisodeCmd cmd) {
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

        // Publish domain event for enrollment linkage
        eventPublisher.publish(new ServiceEpisodeLinked(
            episode.getId().value(),
            UUID.fromString(cmd.enrollmentId()),
            cmd.clientId(),
            cmd.serviceType(),
            cmd.serviceDate(),
            cmd.primaryProviderName()
        ));

        return episode.getId();
    }

    /**
     * Handle start service command
     */
    public void handle(StartServiceCmd cmd) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(cmd.episodeId()))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.startService(cmd.startTime(), cmd.location());
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Handle complete service command
     */
    public void handle(CompleteServiceCmd cmd) {
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
     * Add provider to service episode
     */
    public void addProvider(UUID episodeId, String providerId, String providerName, String role) {
        var episode = serviceEpisodeRepository.findById(ServiceEpisodeId.of(episodeId))
            .orElseThrow(() -> new IllegalArgumentException("Service episode not found"));

        episode.addProvider(providerId, providerName, role);
        serviceEpisodeRepository.save(episode);
    }

    /**
     * Add funding source to service episode
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
}
