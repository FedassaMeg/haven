package org.haven.casemgmt.domain.mandatedreport.events;

import org.haven.shared.events.DomainEvent;
import org.haven.casemgmt.domain.mandatedreport.ReportStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Event fired when mandated report status is updated
 */

public class MandatedReportStatusUpdated extends DomainEvent {
    private final UUID caseId;
    private final ReportStatus previousStatus;
    private final ReportStatus newStatus;
    private final String statusReason;
    private final UUID updatedByUserId;

    public MandatedReportStatusUpdated(UUID reportId, UUID caseId, ReportStatus previousStatus, ReportStatus newStatus, String statusReason, UUID updatedByUserId, Instant updatedAt) {
        super(reportId, updatedAt);
        this.caseId = caseId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.statusReason = statusReason;
        this.updatedByUserId = updatedByUserId;
    }

    public UUID caseId() {
        return caseId;
    }

    public ReportStatus previousStatus() {
        return previousStatus;
    }

    public ReportStatus newStatus() {
        return newStatus;
    }

    public String statusReason() {
        return statusReason;
    }

    public UUID updatedByUserId() {
        return updatedByUserId;
    }


    // JavaBean-style getters
    public UUID getCaseId() { return caseId; }
    public ReportStatus getPreviousStatus() { return previousStatus; }
    public ReportStatus getNewStatus() { return newStatus; }
    public String getStatusReason() { return statusReason; }
    public UUID getUpdatedByUserId() { return updatedByUserId; }
}