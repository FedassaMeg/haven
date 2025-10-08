package org.haven.api.exports.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ExportJobStatusResponse {
    private UUID exportJobId;
    private String state;
    private String exportType;
    private LocalDate reportingPeriodStart;
    private LocalDate reportingPeriodEnd;
    private Instant queuedAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private String downloadUrl;
    private String sha256Hash;
    private Long totalRecords;
    private Long vawaSupressedRecords;

    public ExportJobStatusResponse() {}

    public ExportJobStatusResponse(UUID exportJobId, String state, String exportType,
                                   LocalDate reportingPeriodStart, LocalDate reportingPeriodEnd,
                                   Instant queuedAt, Instant startedAt, Instant completedAt,
                                   String errorMessage, String downloadUrl, String sha256Hash,
                                   Long totalRecords, Long vawaSupressedRecords) {
        this.exportJobId = exportJobId;
        this.state = state;
        this.exportType = exportType;
        this.reportingPeriodStart = reportingPeriodStart;
        this.reportingPeriodEnd = reportingPeriodEnd;
        this.queuedAt = queuedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.downloadUrl = downloadUrl;
        this.sha256Hash = sha256Hash;
        this.totalRecords = totalRecords;
        this.vawaSupressedRecords = vawaSupressedRecords;
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getVawaSupressedRecords() {
        return vawaSupressedRecords;
    }

    public void setVawaSupressedRecords(Long vawaSupressedRecords) {
        this.vawaSupressedRecords = vawaSupressedRecords;
    }
}
