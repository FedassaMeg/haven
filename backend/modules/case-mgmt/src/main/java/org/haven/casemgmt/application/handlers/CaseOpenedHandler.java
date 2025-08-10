package org.haven.casemgmt.application.handlers;

import org.haven.casemgmt.domain.events.CaseOpened;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles case opened events for cross-cutting concerns
 */
@Component  
public class CaseOpenedHandler implements EventHandler<CaseOpened> {

    @Override
    public void handle(CaseOpened event) {
        // Business logic when a case is opened
        System.out.println("Case opened: " + event.caseId() + 
                          " for client: " + event.clientId() +
                          " type: " + event.caseType().text());
        
        // Could trigger:
        // - Notification to case managers
        // - SLA timer start
        // - Resource allocation
        // - Priority queue management
        // - Reporting metrics
    }

    @Override
    public Class<CaseOpened> getEventType() {
        return CaseOpened.class;
    }
}