package org.haven.api.exports.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ExportJobSummaryResponse {
    private UUID exportJobId;
    private String state;
    private String exportType;
    private LocalDate reportingPeriodStart;
    private LocalDate reportingPeriodEnd;
    private Instant queuedAt;
    private Instant completedAt;
    private Integer projectCount;

    public ExportJobSummaryResponse() {}

    public ExportJobSummaryResponse(UUID exportJobId, String state, String exportType,
                                   LocalDate reportingPeriodStart, LocalDate reportingPeriodEnd,
                                   Instant queuedAt, Instant completedAt, Integer projectCount) {
        this.exportJobId = exportJobId;
        this.state = state;
        this.exportType = exportType;
        this.reportingPeriodStart = reportingPeriodStart;
        this.reportingPeriodEnd = reportingPeriodEnd;
        this.queuedAt = queuedAt;
        this.completedAt = completedAt;
        this.projectCount = projectCount;
    }

    // Getters and setters
    public UUID getExportJobId() {
        return exportJobId;
    }

    public void setExportJobId(UUID exportJobId) {
        this.exportJobId = exportJobId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public LocalDate getReportingPeriodStart() {
        return reportingPeriodStart;
    }

    public void setReportingPeriodStart(LocalDate reportingPeriodStart) {
        this.reportingPeriodStart = reportingPeriodStart;
    }

    public LocalDate getReportingPeriodEnd() {
        return reportingPeriodEnd;
    }

    public void setReportingPeriodEnd(LocalDate reportingPeriodEnd) {
        this.reportingPeriodEnd = reportingPeriodEnd;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Integer projectCount) {
        this.projectCount = projectCount;
    }
}
