package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.Repository;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CaseRepository extends Repository<CaseRecord, CaseId> {
    
    List<CaseRecord> findByClientId(ClientId clientId);
    
    List<CaseRecord> findByStatus(CaseRecord.CaseStatus status);
    
    List<CaseRecord> findByAssignee(String assigneeId);
    
    List<CaseRecord> findByCaseType(CodeableConcept caseType);
    
    List<CaseRecord> findActiveCases();
    
    List<CaseRecord> findCasesCreatedBetween(Instant start, Instant end);
    
    Optional<CaseRecord> findByExternalId(String externalId);
}