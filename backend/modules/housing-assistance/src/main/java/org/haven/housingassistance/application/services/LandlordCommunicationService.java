package org.haven.housingassistance.application.services;

import org.haven.clientprofile.infrastructure.security.ConsentEnforcementAspect.RequiresConsent;
import org.haven.clientprofile.infrastructure.security.DataSystemBoundaryEnforcer.EnforceDataSystemBoundary;
import org.haven.clientprofile.infrastructure.security.PIIRedactionService;
import org.haven.clientprofile.infrastructure.security.VSPDataAccessService;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.DataSystem;
import org.haven.clientprofile.domain.consent.ConsentType;
import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.housingassistance.application.services.ContactSafetyService;
import org.haven.housingassistance.domain.LandlordCommunication;
import org.haven.housingassistance.infrastructure.persistence.LandlordCommunicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing landlord communications with consent and privacy enforcement
 */
@Service
@Transactional
public class LandlordCommunicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LandlordCommunicationService.class);
    
    private final LandlordCommunicationRepository communicationRepository;
    private final PIIRedactionService piiRedactionService;
    private final VSPDataAccessService vspDataAccessService;
    private final ContactSafetyService contactSafetyService;
    
    public LandlordCommunicationService(
            LandlordCommunicationRepository communicationRepository,
            PIIRedactionService piiRedactionService,
            VSPDataAccessService vspDataAccessService,
            ContactSafetyService contactSafetyService) {
        this.communicationRepository = communicationRepository;
        this.piiRedactionService = piiRedactionService;
        this.vspDataAccessService = vspDataAccessService;
        this.contactSafetyService = contactSafetyService;
    }
    
    /**
     * Send communication to landlord with consent and privacy checks
     */
    @EnforceDataSystemBoundary(
        allowedSystems = {DataSystem.COMPARABLE_DB},
        requiresPIIAccess = true
    )
    @RequiresConsent(
        operation = "landlord_contact",
        requiredConsentTypes = {ConsentType.INFORMATION_SHARING, ConsentType.REFERRAL_SHARING}
    )
    public LandlordCommunication sendCommunication(
            UUID clientId, 
            UUID landlordId,
            Channel channel,
            String subject,
            String body,
            Map<String, Object> requestedFields,
            RecipientContact recipient,
            UUID userId) {
        
        logger.info("Initiating landlord communication for client: {} to landlord: {}", clientId, landlordId);
        
        // Step 1: Check contact safety preferences
        validateContactSafety(clientId, channel);
        
        // Step 2: Apply PII redaction based on user context and minimum necessary principle
        Map<String, Object> sharedFields = applyMinimumNecessaryRedaction(
            clientId, requestedFields, userId
        );
        
        // Step 3: Create and persist communication record
        LandlordCommunication communication = new LandlordCommunication();
        communication.setId(UUID.randomUUID());
        communication.setLandlordId(landlordId);
        communication.setClientId(clientId);
        communication.setChannel(channel.name());
        communication.setSubject(subject);
        communication.setBody(body);
        communication.setSharedFields(sharedFields);
        communication.setRecipientContact(recipient.getContactInfo());
        communication.setConsentChecked(true);
        communication.setConsentType(ConsentType.INFORMATION_SHARING.name());
        communication.setSentStatus(SentStatus.DRAFT.name()); // Convert enum to string
        communication.setSentBy(userId);
        communication.setCreatedAt(Instant.now());
        communication.setUpdatedAt(Instant.now());
        
        // Step 4: Actually send the communication (implementation depends on channel)
        boolean sent = sendViaChannel(channel, recipient, subject, body, sharedFields);
        
        if (sent) {
            communication.setSentStatus(SentStatus.SENT.name());
            communication.setSentAt(Instant.now());
            
            // Step 5: Log VSP data access if applicable
            logDataAccess(clientId, landlordId, sharedFields, userId);
        } else {
            communication.setSentStatus(SentStatus.FAILED.name());
            logger.error("Failed to send communication to landlord: {}", landlordId);
        }
        
        // Step 6: Save communication record
        communication = communicationRepository.save(communication);
        
        // Step 7: Emit audit log entry
        auditLog(clientId, landlordId, channel, communication.getSentStatus(), userId);
        
        return communication;
    }
    
    /**
     * Validate contact safety preferences before sending
     */
    private void validateContactSafety(UUID clientId, Channel channel) {
        boolean isRestricted = contactSafetyService.isChannelRestricted(clientId, channel.name());
        if (isRestricted) {
            throw new ContactSafetyException(
                String.format("Channel %s is restricted for client %s due to safety preferences",
                    channel, clientId)
            );
        }
    }
    
    /**
     * Apply minimum necessary PII redaction
     */
    private Map<String, Object> applyMinimumNecessaryRedaction(
            UUID clientId, Map<String, Object> requestedFields, UUID userId) {
        
        try {
            // Create PII access context for landlord communication
            PIIAccessContext context = new PIIAccessContext(
                userId,
                List.of("CASE_MANAGER"), // Assume user has case manager role for this operation
                "LANDLORD_COMMUNICATION", // Business justification
                null, // caseId - could be provided if available
                null, // sessionId - could be provided if available  
                null  // ipAddress - could be provided if available
            );
            
            // Apply PII redaction using the correct API - pass the requestedFields as data object
            Map<String, Object> redactedData = piiRedactionService.createExportProjection(
                requestedFields,
                context,
                PIIRedactionService.ExportType.VSP_SHARING
            );
            
            // Additional filtering for landlord communication - minimum necessary only
            Set<String> allowedFields = Set.of(
                "firstName", "lastName", "enrollmentId", 
                "unitNumber", "leaseStartDate", "leaseEndDate",
                "monthlyRent", "assistanceAmount", "paymentSchedule"
            );
            
            return redactedData.entrySet().stream()
                .filter(entry -> allowedFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
        } catch (Exception e) {
            logger.warn("Failed to apply PII redaction, using minimal fallback for client: {}", clientId, e);
            
            // Fallback to minimal safe fields only
            Map<String, Object> minimalData = new HashMap<>();
            if (requestedFields.containsKey("firstName")) {
                minimalData.put("firstName", requestedFields.get("firstName"));
            }
            if (requestedFields.containsKey("lastName")) {
                minimalData.put("lastName", requestedFields.get("lastName"));
            }
            if (requestedFields.containsKey("unitNumber")) {
                minimalData.put("unitNumber", requestedFields.get("unitNumber"));
            }
            return minimalData;
        }
    }
    
    /**
     * Send communication via specified channel
     */
    private boolean sendViaChannel(Channel channel, RecipientContact recipient, 
                                  String subject, String body, Map<String, Object> sharedFields) {
        try {
            switch (channel) {
                case EMAIL:
                    return sendEmail(recipient.getEmail(), subject, body, sharedFields);
                case PHONE:
                    return logPhoneCall(recipient.getPhone(), subject, body);
                case FAX:
                    return sendFax(recipient.getFax(), subject, body);
                case PORTAL:
                    return postToPortal(recipient.getPortalId(), subject, body, sharedFields);
                case IN_PERSON:
                    return logInPersonMeeting(subject, body);
                default:
                    logger.warn("Unsupported channel: {}", channel);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error sending communication via {}: {}", channel, e.getMessage());
            return false;
        }
    }
    
    private boolean sendEmail(String email, String subject, String body, Map<String, Object> sharedFields) {
        // TODO: Implement actual email sending
        logger.info("Sending email to: {} with subject: {}", email, subject);
        return true;
    }
    
    private boolean logPhoneCall(String phone, String subject, String body) {
        // Log phone call details
        logger.info("Logging phone call to: {} regarding: {}", phone, subject);
        return true;
    }
    
    private boolean sendFax(String fax, String subject, String body) {
        // TODO: Implement fax sending
        logger.info("Sending fax to: {} with subject: {}", fax, subject);
        return true;
    }
    
    private boolean postToPortal(String portalId, String subject, String body, Map<String, Object> sharedFields) {
        // TODO: Implement portal posting
        logger.info("Posting to portal: {} with subject: {}", portalId, subject);
        return true;
    }
    
    private boolean logInPersonMeeting(String subject, String body) {
        // Log in-person meeting details
        logger.info("Logging in-person meeting regarding: {}", subject);
        return true;
    }
    
    /**
     * Log VSP data access if applicable
     */
    private void logDataAccess(UUID clientId, UUID landlordId,
                              Map<String, Object> sharedFields, UUID userId) {
        try {
            vspDataAccessService.logVSPDataAccess(
                userId,
                clientId,
                "LANDLORD_COMMUNICATION",
                true, // Access granted
                String.format("Shared client data with landlord: %s. Fields: %s", 
                    landlordId, sharedFields.keySet().toString())
            );
        } catch (Exception e) {
            logger.warn("Failed to log VSP data access for client: {}", clientId, e);
        }
    }
    
    /**
     * Emit audit log entry
     */
    private void auditLog(UUID clientId, UUID landlordId, Channel channel, 
                         String status, UUID userId) {
        logger.info("AUDIT: User {} sent {} communication to landlord {} for client {} - Status: {}",
            userId, channel, landlordId, clientId, status);
    }
    
    /**
     * Get communication history for a client
     */
    @Transactional(readOnly = true)
    public List<LandlordCommunication> getCommunicationsByClient(UUID clientId) {
        return communicationRepository.findByClientId(clientId);
    }
    
    /**
     * Get communication history for a landlord
     */
    @Transactional(readOnly = true)
    public List<LandlordCommunication> getCommunicationsByLandlord(UUID landlordId) {
        return communicationRepository.findByLandlordId(landlordId);
    }
    
    /**
     * Communication channel enum
     */
    public enum Channel {
        PHONE, EMAIL, TEXT, FAX, PORTAL, IN_PERSON, OTHER
    }
    
    /**
     * Sent status enum
     */
    public enum SentStatus {
        DRAFT, SENT, FAILED
    }
    
    /**
     * Recipient contact information
     */
    public static class RecipientContact {
        private String email;
        private String phone;
        private String fax;
        private String portalId;
        private String preferredChannel;
        
        public String getContactInfo() {
            if (email != null) return email;
            if (phone != null) return phone;
            if (fax != null) return fax;
            if (portalId != null) return portalId;
            return "Unknown";
        }
        
        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getFax() { return fax; }
        public void setFax(String fax) { this.fax = fax; }
        public String getPortalId() { return portalId; }
        public void setPortalId(String portalId) { this.portalId = portalId; }
        public String getPreferredChannel() { return preferredChannel; }
        public void setPreferredChannel(String preferredChannel) { this.preferredChannel = preferredChannel; }
    }
    
    /**
     * Custom exception for contact safety violations
     */
    public static class ContactSafetyException extends RuntimeException {
        public ContactSafetyException(String message) {
            super(message);
        }
    }
}