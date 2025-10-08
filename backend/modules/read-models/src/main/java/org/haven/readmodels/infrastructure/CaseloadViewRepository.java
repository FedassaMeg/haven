package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.CaseloadView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CaseloadViewRepository {
    
    private final JpaCaseloadViewRepository jpaRepository;
    
    public CaseloadViewRepository(JpaCaseloadViewRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    public Optional<CaseloadView> findByCaseId(UUID caseId) {
        return jpaRepository.findByCaseId(caseId)
            .map(JpaCaseloadViewEntity::toDomain);
    }
    
    public List<CaseloadView> findByClientId(UUID clientId) {
        return jpaRepository.findByClientId(clientId)
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public Page<CaseloadView> findByWorkerId(UUID workerId, Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findByWorkerId(workerId, pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
    
    public Page<CaseloadView> findByWorkerIdAndStage(UUID workerId, CaseloadView.CaseStage stage, Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findByWorkerIdAndStage(workerId, stage, pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
    
    public Page<CaseloadView> findByWorkerIdAndRiskLevel(UUID workerId, CaseloadView.RiskLevel riskLevel, Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findByWorkerIdAndRiskLevel(workerId, riskLevel, pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
    
    public Page<CaseloadView> findByWorkerIdAndRequiresAttentionTrue(UUID workerId, Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findByWorkerIdAndRequiresAttentionTrue(workerId, pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
    
    public Page<CaseloadView> findByWorkerIdAndStages(UUID workerId, List<CaseloadView.CaseStage> stages, Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findByWorkerIdAndStages(workerId, stages, pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
    
    public Page<CaseloadView> findByProgramAndStatus(UUID programId, CaseloadView.CaseStatus status, Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findByProgramAndStatus(programId, status, pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
    
    public List<CaseloadView> findOverdueCases(Integer days) {
        return jpaRepository.findOverdueCases(days)
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public List<CaseloadView> findHighRiskCases() {
        return jpaRepository.findHighRiskCases()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public Long countActiveByWorker(UUID workerId) {
        return jpaRepository.countActiveByWorker(workerId);
    }
    
    public Long countByWorkerAndStage(UUID workerId, CaseloadView.CaseStage stage) {
        return jpaRepository.countByWorkerAndStage(workerId, stage);
    }
    
    public List<CaseloadView> findSafeAtHomeCases() {
        return jpaRepository.findSafeAtHomeCases()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public List<CaseloadView> findComparableDbOnlyCases() {
        return jpaRepository.findComparableDbOnlyCases()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public CaseloadView save(CaseloadView view) {
        JpaCaseloadViewEntity entity = jpaRepository.findByCaseId(view.getCaseId())
            .orElse(new JpaCaseloadViewEntity(view));
        
        entity.updateFrom(view);
        JpaCaseloadViewEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    public List<CaseloadView> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    public Page<CaseloadView> findAll(Pageable pageable) {
        Page<JpaCaseloadViewEntity> entities = jpaRepository.findAll(pageable);
        List<CaseloadView> content = entities.getContent()
            .stream()
            .map(JpaCaseloadViewEntity::toDomain)
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, entities.getTotalElements());
    }
}