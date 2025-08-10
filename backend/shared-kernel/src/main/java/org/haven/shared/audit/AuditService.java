package org.haven.shared.audit;

import org.haven.shared.events.DomainEvent;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for audit logging business events
 */
@Service
public class AuditService {
    
    public void logDomainEvent(DomainEvent event, String userId, String action) {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            event.aggregateId(),
            event.eventType(),
            action,
            userId,
            Instant.now(),
            event.toString()
        );
        
        // In real implementation, would persist to audit log
        System.out.println("AUDIT: " + entry);
    }
    
    public void logBusinessAction(UUID resourceId, String resourceType, 
                                 String action, String userId, String details) {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(),
            resourceId,
            resourceType,
            action,
            userId,
            Instant.now(),
            details
        );
        
        // In real implementation, would persist to audit log  
        System.out.println("AUDIT: " + entry);
    }
    
    private record AuditEntry(
        UUID id,
        UUID resourceId,
        String resourceType,
        String action,
        String userId,
        Instant timestamp,
        String details
    ) {}
}