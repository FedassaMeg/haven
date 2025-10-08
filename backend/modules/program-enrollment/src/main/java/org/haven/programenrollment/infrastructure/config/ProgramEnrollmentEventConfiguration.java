package org.haven.programenrollment.infrastructure.config;

import org.haven.programenrollment.application.eventhandlers.ServiceEpisodeLinkedEventHandler;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.shared.events.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Program Enrollment Event Configuration
 * Registers event handlers for cross-module integration
 */
@Configuration
public class ProgramEnrollmentEventConfiguration {

    private final EventPublisher eventPublisher;

    public ProgramEnrollmentEventConfiguration(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Bean
    public ServiceEpisodeLinkedEventHandler serviceEpisodeLinkedEventHandler(
            ProgramEnrollmentRepository enrollmentRepository) {
        ServiceEpisodeLinkedEventHandler handler = new ServiceEpisodeLinkedEventHandler(enrollmentRepository);
        eventPublisher.subscribe(handler);
        return handler;
    }
}
