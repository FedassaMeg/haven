package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.CaseRecord;
import org.haven.casemgmt.domain.CaseId;
import org.haven.casemgmt.domain.CaseRepository;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.CodeableConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface JpaCaseRepository extends JpaRepository<JpaCaseEntity, UUID>, CaseRepository {
    
    @Override
    default void save(CaseRecord caseRecord) {
        JpaCaseEntity entity = JpaCaseEntity.fromDomain(caseRecord);
        save(entity);
    }
    
    @Override
    default Optional<CaseRecord> findById(CaseId id) {
        return findById(id.value())
            .map(JpaCaseEntity::toDomain);
    }
    
    @Override
    default List<CaseRecord> findByClientId(ClientId clientId) {
        return findByClientId(clientId.value()).stream()
            .map(JpaCaseEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaCaseEntity> findByClientId(UUID clientId);
    
    @Override
    default List<CaseRecord> findByStatus(CaseRecord.CaseStatus status) {
        return findJpaCaseEntitiesByStatus(status).stream()
            .map(JpaCaseEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaCaseEntity> findJpaCaseEntitiesByStatus(CaseRecord.CaseStatus status);
    
    @Override
    default List<CaseRecord> findByAssignee(String assigneeId) {
        return findByAssignedTo(assigneeId).stream()
            .map(JpaCaseEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaCaseEntity> findByAssignedTo(String assignedTo);
    
    @Override
    default List<CaseRecord> findByCaseType(CodeableConcept caseType) {
        return findByCaseTypeCode(caseType.coding().isEmpty() ? null : caseType.coding().get(0).code()).stream()
            .map(JpaCaseEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaCaseEntity> findByCaseTypeCode(String caseTypeCode);
    
    @Override
    default List<CaseRecord> findActiveCases() {
        return findByStatusNot(CaseRecord.CaseStatus.CLOSED).stream()
            .map(JpaCaseEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaCaseEntity> findByStatusNot(CaseRecord.CaseStatus status);
    
    @Override
    default List<CaseRecord> findCasesCreatedBetween(Instant start, Instant end) {
        return findByCreatedAtBetween(start, end).stream()
            .map(JpaCaseEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaCaseEntity> findByCreatedAtBetween(Instant start, Instant end);
    
    @Override
    default Optional<CaseRecord> findByExternalId(String externalId) {
        // For now, external ID is not implemented in the entity
        // You would need to add an external_id column to the cases table
        return Optional.empty();
    }
    
    @Override
    default void delete(CaseRecord caseRecord) {
        deleteById(caseRecord.getId().value());
    }
    
    @Override
    default CaseId nextId() {
        return CaseId.generate();
    }
}