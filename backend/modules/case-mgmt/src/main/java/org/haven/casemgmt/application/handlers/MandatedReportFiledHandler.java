package org.haven.casemgmt.application.handlers;

import org.haven.casemgmt.domain.events.MandatedReportFiled;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles mandated reporting events for compliance and follow-up tracking
 */
@Component
public class MandatedReportFiledHandler implements EventHandler<MandatedReportFiled> {

    @Override
    public void handle(MandatedReportFiled event) {
        System.out.println("Mandated report filed for case: " + event.caseId() + 
                          " - Client: " + event.clientId() +
                          " - Report type: " + event.reportType().text() +
                          " - Agency: " + event.reportingAgency() +
                          " - Report #: " + event.reportNumber());
        
        // Critical compliance and follow-up workflows:
        // - Document report in case file with timestamp
        // - Set follow-up reminders based on report type
        // - Notify client of report filing (when appropriate)
        // - Update safety assessment based on incident
        // - Coordinate with investigating agency
        // - Ensure client safety during investigation
        // - Document any protection measures taken
        
        if (event.isEmergencyReport()) {
            System.out.println("EMERGENCY REPORT: Immediate safety protocols activated");
            // - Alert supervisors immediately
            // - Activate crisis response team
            // - Coordinate with law enforcement
            // - Ensure client safety and support
        }
        
        if (event.followUpRequired() != null && !event.followUpRequired().trim().isEmpty()) {
            System.out.println("Follow-up required: " + event.followUpRequired());
            // - Schedule follow-up tasks
            // - Set calendar reminders
            // - Assign responsibility for follow-up
        }
        
        // Log for audit and compliance tracking
        System.out.println("Report filed by: " + event.reportedBy() + " on " + event.filedDate());
        System.out.println("Incident description: " + event.incidentDescription());
    }

    @Override
    public Class<MandatedReportFiled> getEventType() {
        return MandatedReportFiled.class;
    }
}