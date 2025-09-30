package org.haven.clientprofile.infrastructure.eventhandlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.haven.clientprofile.domain.consent.events.*;
import org.haven.clientprofile.infrastructure.persistence.ConsentAuditTrailEntity;
import org.haven.clientprofile.infrastructure.persistence.ConsentAuditTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Event handler to maintain the immutable consent audit trail
 * Records all consent-related events for compliance and auditing purposes
 */
@Component
@Transactional
public class ConsentAuditTrailHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsentAuditTrailHandler.class);
    
    private final ConsentAuditTrailRepository auditRepository;
    private final ObjectMapper objectMapper;
    
    public ConsentAuditTrailHandler(ConsentAuditTrailRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }
    
    @EventListener
    public void on(ConsentGranted event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            ConsentAuditTrailEntity auditEntry = new ConsentAuditTrailEntity(
                event.consentId(),
                event.clientId(),
                event.eventType(),
                event.consentType(),
                event.grantedByUserId(),
                event.occurredAt(),
                eventData,
                "Consent granted: " + event.purpose(),
                event.recipientOrganization(),
                getCurrentIpAddress(),
                getCurrentUserAgent()
            );
            
            auditRepository.save(auditEntry);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ConsentGranted event for audit trail", e);
        }
    }
    
    @EventListener
    public void on(ConsentRevoked event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            ConsentAuditTrailEntity auditEntry = new ConsentAuditTrailEntity(
                event.consentId(),
                event.clientId(),
                event.eventType(),
                event.consentType(),
                event.revokedByUserId(),
                event.occurredAt(),
                eventData,
                event.reason(),
                null, // No recipient org for revocation
                getCurrentIpAddress(),
                getCurrentUserAgent()
            );
            
            auditRepository.save(auditEntry);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ConsentRevoked event for audit trail", e);
        }
    }
    
    @EventListener
    public void on(ConsentUpdated event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            ConsentAuditTrailEntity auditEntry = new ConsentAuditTrailEntity(
                event.consentId(),
                event.clientId(),
                event.eventType(),
                null, // Type not available in update event
                event.updatedByUserId(),
                event.occurredAt(),
                eventData,
                "Consent limitations or contact updated",
                null,
                getCurrentIpAddress(),
                getCurrentUserAgent()
            );
            
            auditRepository.save(auditEntry);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ConsentUpdated event for audit trail", e);
        }
    }
    
    @EventListener
    public void on(ConsentExtended event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            ConsentAuditTrailEntity auditEntry = new ConsentAuditTrailEntity(
                event.consentId(),
                event.clientId(),
                event.eventType(),
                null, // Type not available in extend event
                event.extendedByUserId(),
                event.occurredAt(),
                eventData,
                "Consent expiration extended",
                null,
                getCurrentIpAddress(),
                getCurrentUserAgent()
            );
            
            auditRepository.save(auditEntry);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ConsentExtended event for audit trail", e);
        }
    }
    
    @EventListener
    public void on(ConsentExpired event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            ConsentAuditTrailEntity auditEntry = new ConsentAuditTrailEntity(
                event.consentId(),
                event.clientId(),
                event.eventType(),
                event.consentType(),
                null, // System action, no user
                event.occurredAt(),
                eventData,
                "Consent automatically expired",
                null,
                null, // System action
                "System"
            );
            
            auditRepository.save(auditEntry);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ConsentExpired event for audit trail", e);
        }
    }
    
    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.debug("Could not determine IP address", e);
        }
        return null;
    }
    
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            logger.debug("Could not determine user agent", e);
        }
        return null;
    }
}