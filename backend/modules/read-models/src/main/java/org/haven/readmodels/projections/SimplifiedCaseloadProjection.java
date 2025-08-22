package org.haven.readmodels.projections;

import org.axonframework.eventhandling.EventHandler;
import org.haven.casemgmt.domain.events.*;
import org.haven.readmodels.domain.CaseloadView;
import org.haven.readmodels.infrastructure.CaseloadViewRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;

@Component
@Transactional
public class SimplifiedCaseloadProjection {
    
    private final CaseloadViewRepository caseloadRepository;
    
    public SimplifiedCaseloadProjection(CaseloadViewRepository caseloadRepository) {
        this.caseloadRepository = caseloadRepository;
    }
    
    @EventHandler
    public void on(CaseOpened event) {
        CaseloadView view = new CaseloadView();
        view.setCaseId(event.caseId());
        view.setCaseNumber("CASE-" + event.caseId().toString().substring(0, 8).toUpperCase());
        view.setClientId(event.clientId());
        view.setStage(CaseloadView.CaseStage.INTAKE);
        view.setStatus(CaseloadView.CaseStatus.OPEN);
        view.setEnrollmentDate(LocalDate.now());
        view.setServiceCount(0);
        view.setRequiresAttention(false);
        view.setLastUpdated(event.occurredAt());
        view.setActiveAlerts(new ArrayList<>());
        view.setRiskLevel(CaseloadView.RiskLevel.LOW); // Default risk
        
        caseloadRepository.save(view);
    }
    
    @EventHandler
    public void on(CaseAssigned event) {
        caseloadRepository.findByCaseId(event.caseId())
            .ifPresent(view -> {
                try {
                    view.setWorkerId(java.util.UUID.fromString(event.assigneeId()));
                } catch (IllegalArgumentException e) {
                    // If assigneeId is not a valid UUID, skip setting it
                }
                view.setLastUpdated(event.occurredAt());
                caseloadRepository.save(view);
            });
    }
    
    @EventHandler
    public void on(CaseClosed event) {
        caseloadRepository.findByCaseId(event.caseId())
            .ifPresent(view -> {
                view.setStage(CaseloadView.CaseStage.CLOSED);
                view.setStatus(CaseloadView.CaseStatus.CLOSED);
                view.setRequiresAttention(false);
                view.setLastUpdated(event.occurredAt());
                caseloadRepository.save(view);
            });
    }
}