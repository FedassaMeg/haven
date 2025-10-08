package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when agency response or investigation outcome is recorded
 */

public class AgencyResponseRecorded extends DomainEvent {
    private final UUID caseId;
    private final String response;
    private final String investigationOutcome;
    private final UUID recordedByUserId;

    public AgencyResponseRecorded(UUID reportId, UUID caseId, String response, String investigationOutcome, UUID recordedByUserId, Instant recordedAt) {
        super(reportId, recordedAt);
        this.caseId = caseId;
        this.response = response;
        this.investigationOutcome = investigationOutcome;
        this.recordedByUserId = recordedByUserId;
    }

    public UUID caseId() {
        return caseId;
    }

    public String response() {
        return response;
    }

    public String investigationOutcome() {
        return investigationOutcome;
    }

    public UUID recordedByUserId() {
        return recordedByUserId;
    }


    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public String getResponse() { return response; }
    public String getInvestigationOutcome() { return investigationOutcome; }
    public UUID getRecordedByUserId() { return recordedByUserId; }
}