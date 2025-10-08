package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.CaseRecord;
import org.haven.casemgmt.domain.CaseId;
import org.haven.casemgmt.domain.CaseRepository;
import org.haven.clientprofile.domain.ClientId;
import org.haven.eventstore.EventStore;
import org.haven.shared.vo.CodeableConcept;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class CaseRepositoryImpl implements CaseRepository {

    private final JpaCaseRepository jpaRepository;
    private final EventStore eventStore;

    public CaseRepositoryImpl(JpaCaseRepository jpaRepository, EventStore eventStore) {
        this.jpaRepository = jpaRepository;
        this.eventStore = eventStore;
    }

    @Override
    public void save(CaseRecord caseRecord) {
        JpaCaseEntity entity = JpaCaseEntity.fromDomain(caseRecord);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<CaseRecord> findById(CaseId id) {
        return jpaRepository.findById(id.value())
            .map(entity -> entity.toDomain(eventStore));
    }

    @Override
    public List<CaseRecord> findByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).stream()
            .map(entity -> entity.toDomain(eventStore))
            .toList();
    }

    @Override
    public List<CaseRecord> findByStatus(CaseRecord.CaseStatus status) {
        return jpaRepository.findJpaCaseEntitiesByStatus(status).stream()
            .map(entity -> entity.toDomain(eventStore))
            .toList();
    }

    @Override
    public List<CaseRecord> findByAssignee(String assigneeId) {
        return jpaRepository.findByAssignedTo(assigneeId).stream()
            .map(entity -> entity.toDomain(eventStore))
            .toList();
    }

    @Override
    public List<CaseRecord> findByCaseType(CodeableConcept caseType) {
        String code = caseType.coding().isEmpty() ? null : caseType.coding().get(0).code();
        return jpaRepository.findByCaseTypeCode(code).stream()
            .map(entity -> entity.toDomain(eventStore))
            .toList();
    }

    @Override
    public List<CaseRecord> findActiveCases() {
        return jpaRepository.findByStatusNot(CaseRecord.CaseStatus.CLOSED).stream()
            .map(entity -> entity.toDomain(eventStore))
            .toList();
    }

    @Override
    public List<CaseRecord> findCasesCreatedBetween(Instant start, Instant end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream()
            .map(entity -> entity.toDomain(eventStore))
            .toList();
    }

    @Override
    public Optional<CaseRecord> findByExternalId(String externalId) {
        // External ID not yet implemented
        return Optional.empty();
    }

    @Override
    public void delete(CaseRecord caseRecord) {
        jpaRepository.deleteById(caseRecord.getId().value());
    }

    @Override
    public CaseId nextId() {
        return CaseId.generate();
    }
}
