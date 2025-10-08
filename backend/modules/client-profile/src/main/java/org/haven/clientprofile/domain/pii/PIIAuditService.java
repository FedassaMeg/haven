package org.haven.clientprofile.domain.pii;

import java.util.UUID;

public class PIIAuditService {
    
    private final PIIAuditRepository auditRepository;
    
    public PIIAuditService(PIIAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }
    
    public void logAccess(PIIAccessContext context, UUID clientId, PIICategory category,
                         PIIAccessLevel accessLevel, boolean granted) {
        
        PIIAccessLog logEntry = new PIIAccessLog(
            context.getUserId(),
            clientId,
            category,
            accessLevel,
            granted,
            context.getBusinessJustification(),
            context.getCaseId(),
            context.getSessionId(),
            context.getIpAddress(),
            context.getAccessTime()
        );
        
        auditRepository.save(logEntry);
    }
    
    public void logBulkAccess(PIIAccessContext context, UUID clientId, 
                             String accessMethod, int recordCount) {
        
        PIIAccessLog logEntry = new PIIAccessLog(
            context.getUserId(),
            clientId,
            PIICategory.SERVICE_DATA, // For bulk operations
            PIIAccessLevel.INTERNAL,
            true,
            context.getBusinessJustification() + " (Bulk: " + recordCount + " records)",
            context.getCaseId(),
            context.getSessionId(),
            context.getIpAddress(),
            context.getAccessTime()
        );
        
        auditRepository.save(logEntry);
    }
}