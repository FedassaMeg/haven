package org.haven.readmodels.projections;

import org.axonframework.eventhandling.EventHandler;
import org.haven.casemgmt.domain.events.*;
import org.haven.readmodels.domain.TriageAlert;
import org.haven.readmodels.infrastructure.TriageAlertRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Transactional
public class SimplifiedTriageProjection {
    
    private final TriageAlertRepository alertRepository;
    
    public SimplifiedTriageProjection(TriageAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }
    
    @EventHandler
    public void on(MandatedReportFiled event) {
        // Create high priority alert for mandated report follow-up
        TriageAlert alert = new TriageAlert();
        alert.setClientId(event.clientId());
        alert.setAlertType(TriageAlert.AlertType.SAFETY_CHECK_NEEDED);
        alert.setSeverity(TriageAlert.AlertSeverity.CRITICAL);
        alert.setDescription(String.format(
            "Mandated report filed (%s). Immediate safety planning required. Report #: %s",
            event.reportType(), event.reportNumber()
        ));
        alert.setDueDate(LocalDate.now());
        alert.setStatus(TriageAlert.AlertStatus.ACTIVE);
        alert.setCaseNumber(event.caseId().toString());
        
        alertRepository.save(alert);
    }
    
    @EventHandler
    public void on(CaseOpened event) {
        // Create alert for new cases requiring intake
        TriageAlert alert = new TriageAlert();
        alert.setClientId(event.clientId());
        alert.setAlertType(TriageAlert.AlertType.DOCUMENTATION_MISSING);
        alert.setSeverity(TriageAlert.AlertSeverity.MEDIUM);
        alert.setDescription("New case opened - intake assessment required");
        alert.setDueDate(LocalDate.now().plusDays(3));
        alert.setStatus(TriageAlert.AlertStatus.ACTIVE);
        alert.setCaseNumber(event.caseId().toString());
        
        alertRepository.save(alert);
    }
}