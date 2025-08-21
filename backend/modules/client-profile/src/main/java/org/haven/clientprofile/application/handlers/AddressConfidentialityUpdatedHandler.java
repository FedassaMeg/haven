package org.haven.clientprofile.application.handlers;

import org.haven.clientprofile.domain.events.AddressConfidentialityUpdated;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles address confidentiality updates for Safe at Home program compliance
 */
@Component
public class AddressConfidentialityUpdatedHandler implements EventHandler<AddressConfidentialityUpdated> {

    @Override
    public void handle(AddressConfidentialityUpdated event) {
        System.out.println("Address confidentiality updated for client: " + event.clientId() + 
                          " - Updated by: " + event.updatedBy() +
                          " - Reason: " + event.updateReason());
        
        // Critical security operations:
        // - Update all external system records with safe address
        // - Notify Safe at Home program coordinator
        // - Update mailing systems and labels
        // - Flag court documents for address protection
        // - Update emergency contact systems
        // - Log security audit trail
        // - Verify substitute address validity
        // - Update HMIS/Comparable DB restrictions
        
        boolean wasConfidential = event.previousAddressConfidentiality() != null && 
                                 event.previousAddressConfidentiality().isConfidentialLocation();
        boolean isNowConfidential = event.newAddressConfidentiality() != null && 
                                   event.newAddressConfidentiality().isConfidentialLocation();
        
        if (!wasConfidential && isNowConfidential) {
            System.out.println("Client address protection ENABLED - implementing security measures");
            // Trigger enhanced protection protocols
        } else if (wasConfidential && !isNowConfidential) {
            System.out.println("Client address protection DISABLED - verify safety protocols");
            // Require additional verification and safety check
        }
    }

    @Override
    public Class<AddressConfidentialityUpdated> getEventType() {
        return AddressConfidentialityUpdated.class;
    }
}