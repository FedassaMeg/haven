package org.haven.servicedelivery.infrastructure.config;

import org.haven.servicedelivery.application.handlers.ServiceDeliveryCommandHandler;
import org.haven.servicedelivery.application.queries.ServiceDeliveryQueryService;
import org.haven.servicedelivery.application.reporting.ServiceStatisticsCalculator;
import org.haven.servicedelivery.application.services.ServiceBillingService;
import org.haven.servicedelivery.application.services.ServiceReportingService;
import org.haven.servicedelivery.domain.ServiceEpisodeRepository;
import org.haven.servicedelivery.domain.ServiceTemplateFactory;
import org.haven.shared.events.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Service Delivery Infrastructure Configuration
 * Wires domain services and application handlers
 * Keeps domain layer framework-independent
 */
@Configuration
public class ServiceDeliveryConfiguration {

    @Bean
    public ServiceDeliveryCommandHandler serviceDeliveryCommandHandler(
            ServiceEpisodeRepository serviceEpisodeRepository,
            EventPublisher eventPublisher,
            ServiceBillingService billingService) {
        return new ServiceDeliveryCommandHandler(serviceEpisodeRepository, eventPublisher, billingService);
    }

    @Bean
    public ServiceDeliveryQueryService serviceDeliveryQueryService(
            ServiceEpisodeRepository serviceEpisodeRepository) {
        return new ServiceDeliveryQueryService(serviceEpisodeRepository);
    }

    @Bean
    public ServiceStatisticsCalculator serviceStatisticsCalculator() {
        return new ServiceStatisticsCalculator();
    }

    @Bean
    public ServiceBillingService serviceBillingService(
            ServiceBillingService.BillingRateRepository billingRateRepository) {
        return new ServiceBillingService(billingRateRepository);
    }

    @Bean
    public ServiceReportingService serviceReportingService() {
        return new ServiceReportingService();
    }

    @Bean
    public ServiceTemplateFactory serviceTemplateFactory(
            ServiceTemplateFactory.FundingRuleProvider fundingRuleProvider) {
        return new ServiceTemplateFactory(fundingRuleProvider);
    }
}
