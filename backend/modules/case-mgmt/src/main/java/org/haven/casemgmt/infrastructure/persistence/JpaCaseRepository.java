package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.CaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaCaseEntity
 * Domain repository implementation is in CaseRepositoryImpl
 */
@Repository
public interface JpaCaseRepository extends JpaRepository<JpaCaseEntity, UUID> {

    List<JpaCaseEntity> findByClientId(UUID clientId);

    List<JpaCaseEntity> findJpaCaseEntitiesByStatus(CaseRecord.CaseStatus status);

    List<JpaCaseEntity> findByAssignedTo(String assignedTo);

    List<JpaCaseEntity> findByCaseTypeCode(String caseTypeCode);

    List<JpaCaseEntity> findByStatusNot(CaseRecord.CaseStatus status);

    List<JpaCaseEntity> findByCreatedAtBetween(Instant start, Instant end);
}
