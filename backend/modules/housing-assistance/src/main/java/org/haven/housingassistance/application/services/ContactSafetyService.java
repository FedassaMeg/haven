package org.haven.housingassistance.application.services;

import java.util.UUID;

/**
 * Interface for checking client contact safety preferences
 * This interfaces with the safety-assessment module's ContactSafetyPreferences
 */
public interface ContactSafetyService {
    
    /**
     * Check if a communication channel is restricted for a client
     * 
     * @param clientId The client ID
     * @param channel The communication channel (PHONE, EMAIL, TEXT, etc.)
     * @return true if the channel is restricted, false otherwise
     */
    boolean isChannelRestricted(UUID clientId, String channel);
    
    /**
     * Get the safety restrictions for a client
     * 
     * @param clientId The client ID
     * @return A summary of contact restrictions
     */
    ContactSafetyRestrictions getRestrictions(UUID clientId);
    
    /**
     * DTO for contact safety restrictions
     */
    record ContactSafetyRestrictions(
        boolean noPhone,
        boolean noEmail,
        boolean noText,
        boolean noVoicemail,
        boolean noHomeVisits,
        String restrictionLevel,
        String notes
    ) {}
}