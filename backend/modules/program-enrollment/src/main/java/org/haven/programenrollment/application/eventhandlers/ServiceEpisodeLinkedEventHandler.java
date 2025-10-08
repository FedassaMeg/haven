package org.haven.programenrollment.application.eventhandlers;

import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.shared.events.EventHandler;
import org.haven.shared.events.ServiceEpisodeLinked;

/**
 * Event Handler for ServiceEpisodeLinked events
 * Updates enrollment when a service episode is linked
 * Replaces direct service-to-service coupling
 */
public class ServiceEpisodeLinkedEventHandler implements EventHandler<ServiceEpisodeLinked> {

    private final ProgramEnrollmentRepository enrollmentRepository;

    public ServiceEpisodeLinkedEventHandler(ProgramEnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public void handle(ServiceEpisodeLinked event) {
        var enrollment = enrollmentRepository.findById(ProgramEnrollmentId.of(event.enrollmentId()))
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + event.enrollmentId()));

        if (!enrollment.isActive()) {
            throw new IllegalStateException("Cannot link service to inactive enrollment");
        }

        // Link the service episode to the enrollment
        enrollment.linkServiceEpisode(
            org.haven.programenrollment.domain.ServiceEpisodeId.of(event.episodeId()),
            event.serviceType(),
            event.serviceDate(),
            event.providerName()
        );

        enrollmentRepository.save(enrollment);
    }

    @Override
    public Class<ServiceEpisodeLinked> getEventType() {
        return ServiceEpisodeLinked.class;
    }
}
