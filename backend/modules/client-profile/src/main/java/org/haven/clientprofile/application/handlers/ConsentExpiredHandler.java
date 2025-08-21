package org.haven.clientprofile.application.handlers;

import org.haven.clientprofile.domain.events.ConsentExpired;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles consent expiration events for VAWA compliance and data protection
 */
@Component
public class ConsentExpiredHandler implements EventHandler<ConsentExpired> {

    @Override
    public void handle(ConsentExpired event) {
        System.out.println("Consent expired for client: " + event.clientId() + 
                          " - Type: " + event.consentType() +
                          " - Auto-expired: " + event.autoExpired());
        
        // Critical business logic when consent expires:
        // - Flag client record for review
        // - Suspend data sharing operations
        // - Notify case manager for consent renewal
        // - Update access permissions
        // - Log compliance audit trail
        // - Generate renewal reminder notices
        // - Trigger VAWA compliance workflow
        
        if (event.autoExpired()) {
            // Handle automatic expiration differently
            System.out.println("Auto-expired consent requires immediate attention for VAWA compliance");
        }
    }

    @Override
    public Class<ConsentExpired> getEventType() {
        return ConsentExpired.class;
    }
}