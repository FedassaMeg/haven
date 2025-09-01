package org.haven.housingassistance.infrastructure.services;

import org.haven.housingassistance.application.services.ContactSafetyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Default implementation of ContactSafetyService that stubs out safety checks
 * TODO: Integrate with safety-assessment module's ContactSafetyPreferences
 */
@Service
public class DefaultContactSafetyService implements ContactSafetyService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultContactSafetyService.class);
    
    @Override
    public boolean isChannelRestricted(UUID clientId, String channel) {
        // TODO: Implement actual safety preference checking
        // For now, we'll stub this out and always allow communication
        // In a real implementation, this would:
        // 1. Query ContactSafetyPreferences from safety-assessment module
        // 2. Check if the channel matches restricted communication types
        // 3. Consider restriction levels and safety concerns
        
        logger.debug("Checking channel {} restrictions for client {} - currently stubbed to allow all", 
            channel, clientId);
        
        // Temporarily allow all communications until safety-assessment integration
        return false;
    }
    
    @Override
    public ContactSafetyRestrictions getRestrictions(UUID clientId) {
        // TODO: Implement actual restrictions retrieval
        // For now, return no restrictions
        
        logger.debug("Getting safety restrictions for client {} - currently stubbed to no restrictions", 
            clientId);
            
        return new ContactSafetyRestrictions(
            false, // noPhone
            false, // noEmail  
            false, // noText
            false, // noVoicemail
            false, // noHomeVisits
            "NONE", // restrictionLevel
            "No restrictions configured (stubbed implementation)"
        );
    }
}