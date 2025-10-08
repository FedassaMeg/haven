package org.haven.reporting.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Event: Export job created and queued for processing
 */
public class ExportJobQueued extends DomainEvent {
    private final String exportType;
    private final LocalDate reportingPeriodStart;
    private final LocalDate reportingPeriodEnd;
    private final List<UUID> includedProjectIds;
    private final String requestedByUserId;
    private final String requestedByUserName;
    private final String cocCode;
    private final String exportReason;

    public ExportJobQueued(
            UUID exportJobId,
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> includedProjectIds,
            String requestedByUserId,
            String requestedByUserName,
            String cocCode,
            String exportReason,
            Instant occurredOn) {
        super(exportJobId, occurredOn);
        this.exportType = exportType;
        this.reportingPeriodStart = reportingPeriodStart;
        this.reportingPeriodEnd = reportingPeriodEnd;
        this.includedProjectIds = includedProjectIds;
        this.requestedByUserId = requestedByUserId;
        this.requestedByUserName = requestedByUserName;
        this.cocCode = cocCode;
        this.exportReason = exportReason;
    }

    public String getExportType() {
        return exportType;
    }

    public LocalDate getReportingPeriodStart() {
        return reportingPeriodStart;
    }

    public LocalDate getReportingPeriodEnd() {
        return reportingPeriodEnd;
    }

    public List<UUID> getIncludedProjectIds() {
        return includedProjectIds;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getRequestedByUserName() {
        return requestedByUserName;
    }

    public String getCocCode() {
        return cocCode;
    }

    public String getExportReason() {
        return exportReason;
    }
}
