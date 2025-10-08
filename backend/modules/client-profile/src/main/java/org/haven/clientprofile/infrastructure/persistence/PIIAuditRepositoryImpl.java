package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.pii.PIIAccessLog;
import org.haven.clientprofile.domain.pii.PIIAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PIIAuditRepositoryImpl implements PIIAuditRepository {
    
    @Autowired
    private JpaPIIAuditRepository jpaPIIAuditRepository;
    
    @Override
    public void save(PIIAccessLog logEntry) {
        JpaPIIAccessLogEntity entity = JpaPIIAccessLogEntity.fromDomain(logEntry);
        jpaPIIAuditRepository.save(entity);
    }
}