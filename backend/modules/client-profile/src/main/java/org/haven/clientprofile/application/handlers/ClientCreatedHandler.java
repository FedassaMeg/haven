package org.haven.clientprofile.application.handlers;

import org.haven.clientprofile.domain.events.ClientCreated;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles client creation events for cross-cutting concerns
 */
@Component
public class ClientCreatedHandler implements EventHandler<ClientCreated> {

    @Override
    public void handle(ClientCreated event) {
        // Example business logic that happens when a client is created
        System.out.println("Client created: " + event.clientId() + 
                          " - " + event.name().getFullName());
        
        // Could trigger:
        // - Welcome email
        // - Initial case creation
        // - Onboarding workflow
        // - Audit logging
        // - Analytics events
    }

    @Override
    public Class<ClientCreated> getEventType() {
        return ClientCreated.class;
    }
}