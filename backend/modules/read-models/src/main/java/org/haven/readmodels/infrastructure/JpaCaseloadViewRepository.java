package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.CaseloadView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCaseloadViewRepository extends JpaRepository<JpaCaseloadViewEntity, UUID> {
    
    Optional<JpaCaseloadViewEntity> findByCaseId(UUID caseId);
    
    List<JpaCaseloadViewEntity> findByClientId(UUID clientId);
    
    Page<JpaCaseloadViewEntity> findByWorkerId(UUID workerId, Pageable pageable);
    
    Page<JpaCaseloadViewEntity> findByWorkerIdAndStage(UUID workerId, CaseloadView.CaseStage stage, Pageable pageable);
    
    Page<JpaCaseloadViewEntity> findByWorkerIdAndRiskLevel(UUID workerId, CaseloadView.RiskLevel riskLevel, Pageable pageable);
    
    Page<JpaCaseloadViewEntity> findByWorkerIdAndRequiresAttentionTrue(UUID workerId, Pageable pageable);
    
    @Query("SELECT c FROM JpaCaseloadViewEntity c WHERE c.workerId = :workerId AND c.stage IN :stages")
    Page<JpaCaseloadViewEntity> findByWorkerIdAndStages(@Param("workerId") UUID workerId, @Param("stages") List<CaseloadView.CaseStage> stages, Pageable pageable);
    
    @Query("SELECT c FROM JpaCaseloadViewEntity c WHERE c.programId = :programId AND c.status = :status")
    Page<JpaCaseloadViewEntity> findByProgramAndStatus(@Param("programId") UUID programId, @Param("status") CaseloadView.CaseStatus status, Pageable pageable);
    
    @Query("SELECT c FROM JpaCaseloadViewEntity c WHERE c.daysSinceLastContact > :days AND c.status = 'OPEN'")
    List<JpaCaseloadViewEntity> findOverdueCases(@Param("days") Integer days);
    
    @Query("SELECT c FROM JpaCaseloadViewEntity c WHERE c.riskLevel IN ('CRITICAL', 'HIGH') AND c.status = 'OPEN' ORDER BY c.riskLevel ASC")
    List<JpaCaseloadViewEntity> findHighRiskCases();
    
    @Query("SELECT COUNT(c) FROM JpaCaseloadViewEntity c WHERE c.workerId = :workerId AND c.status = 'OPEN'")
    Long countActiveByWorker(@Param("workerId") UUID workerId);
    
    @Query("SELECT COUNT(c) FROM JpaCaseloadViewEntity c WHERE c.workerId = :workerId AND c.stage = :stage")
    Long countByWorkerAndStage(@Param("workerId") UUID workerId, @Param("stage") CaseloadView.CaseStage stage);
    
    @Query("SELECT c FROM JpaCaseloadViewEntity c WHERE c.isSafeAtHome = true AND c.status = 'OPEN'")
    List<JpaCaseloadViewEntity> findSafeAtHomeCases();
    
    @Query("SELECT c FROM JpaCaseloadViewEntity c WHERE c.dataSystem = 'COMPARABLE_DB' AND c.status = 'OPEN'")
    List<JpaCaseloadViewEntity> findComparableDbOnlyCases();
}