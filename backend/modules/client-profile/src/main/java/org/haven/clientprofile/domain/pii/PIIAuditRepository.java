package org.haven.clientprofile.domain.pii;

public interface PIIAuditRepository {
    
    void save(PIIAccessLog logEntry);
}