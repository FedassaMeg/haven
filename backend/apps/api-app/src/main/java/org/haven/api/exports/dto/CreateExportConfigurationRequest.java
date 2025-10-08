package org.haven.api.exports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating export configuration
 * Supports CoC_APR, ESG_CAPER, SPM, PIT, HIC report types
 */
public class CreateExportConfigurationRequest {

    @NotBlank(message = "Report type is required")
    private String reportType; // CoC_APR, ESG_CAPER, SPM, PIT, HIC

    @NotNull(message = "Operating year start is required")
    private LocalDate operatingYearStart;

    @NotNull(message = "Operating year end is required")
    private LocalDate operatingYearEnd;

    @NotNull(message = "Project IDs list is required")
    private List<UUID> projectIds;

    private boolean includeAggregateOnly = false;

    private String exportNotes;

    // Constructors
    public CreateExportConfigurationRequest() {}

    public CreateExportConfigurationRequest(String reportType, LocalDate operatingYearStart,
                                           LocalDate operatingYearEnd, List<UUID> projectIds,
                                           boolean includeAggregateOnly) {
        this.reportType = reportType;
        this.operatingYearStart = operatingYearStart;
        this.operatingYearEnd = operatingYearEnd;
        this.projectIds = projectIds;
        this.includeAggregateOnly = includeAggregateOnly;
    }

    // Getters and setters
    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getOperatingYearStart() {
        return operatingYearStart;
    }

    public void setOperatingYearStart(LocalDate operatingYearStart) {
        this.operatingYearStart = operatingYearStart;
    }

    public LocalDate getOperatingYearEnd() {
        return operatingYearEnd;
    }

    public void setOperatingYearEnd(LocalDate operatingYearEnd) {
        this.operatingYearEnd = operatingYearEnd;
    }

    public List<UUID> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(List<UUID> projectIds) {
        this.projectIds = projectIds;
    }

    public boolean isIncludeAggregateOnly() {
        return includeAggregateOnly;
    }

    public void setIncludeAggregateOnly(boolean includeAggregateOnly) {
        this.includeAggregateOnly = includeAggregateOnly;
    }

    public String getExportNotes() {
        return exportNotes;
    }

    public void setExportNotes(String exportNotes) {
        this.exportNotes = exportNotes;
    }
}
