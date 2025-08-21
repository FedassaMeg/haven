package org.haven.programenrollment.application.handlers;

import org.haven.programenrollment.domain.events.ServiceProvided;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles service provision events for reporting and case management
 */
@Component
public class ServiceProvidedHandler implements EventHandler<ServiceProvided> {

    @Override
    public void handle(ServiceProvided event) {
        System.out.println("Service provided to client: " + event.clientId() + 
                          " - Service type: " + event.serviceType().text() +
                          " - Category: " + (event.serviceCategory() != null ? event.serviceCategory().text() : "N/A") +
                          " - Provider: " + event.providedBy() +
                          " - Date: " + event.serviceDate() +
                          " - Duration: " + (event.durationMinutes() != null ? event.durationMinutes() + " minutes" : "N/A"));
        
        // Service tracking and reporting workflows:
        // - Update client service history
        // - Track program compliance and outcomes
        // - Generate service utilization reports
        // - Monitor provider performance metrics
        // - Update case notes with service details
        // - Check for service plan milestones
        // - Validate service authorization if required
        
        if (event.isConfidential()) {
            System.out.println("CONFIDENTIAL SERVICE: Special handling required");
            // - Restrict access to authorized personnel only
            // - Apply DV counselor-victim privilege if applicable
            // - Ensure reporting compliance with confidentiality rules
        }
        
        // Update enrollment progress tracking
        System.out.println("Enrollment ID: " + event.enrollmentId() + " - Service location: " + 
                          (event.location() != null ? event.location() : "Not specified"));
        
        // Service quality and follow-up
        if (event.description() != null && !event.description().trim().isEmpty()) {
            System.out.println("Service description: " + event.description());
        }
        
        // Provider accountability and documentation
        if (event.providerId() != null) {
            System.out.println("Provider ID: " + event.providerId() + " for accountability tracking");
        }
    }

    @Override
    public Class<ServiceProvided> getEventType() {
        return ServiceProvided.class;
    }
}