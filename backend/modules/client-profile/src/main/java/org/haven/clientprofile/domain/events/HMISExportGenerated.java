package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class HMISExportGenerated extends DomainEvent {
    private final UUID exportId;
    private final UUID clientId;
    private final LocalDate reportingPeriodStart;
    private final LocalDate reportingPeriodEnd;
    private final String exportType;
    private final List<UUID> includedEnrollments;
    private final String exportedBy;
    private final UUID exportedByUserId;
    private final String hmisVersion;
    private final String exportFilePath;
    private final String exportChecksum;
    private final int recordCount;
    private final boolean containsPII;
    private final String privacyNotice;

    public HMISExportGenerated(
        UUID exportId,
        UUID clientId,
        LocalDate reportingPeriodStart,
        LocalDate reportingPeriodEnd,
        String exportType,
        List<UUID> includedEnrollments,
        String exportedBy,
        UUID exportedByUserId,
        String hmisVersion,
        String exportFilePath,
        String exportChecksum,
        int recordCount,
        boolean containsPII,
        String privacyNotice,
        Instant occurredAt
    ) {
        super(exportId, occurredAt != null ? occurredAt : Instant.now());
        if (exportId == null) throw new IllegalArgumentException("Export ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (reportingPeriodStart == null) throw new IllegalArgumentException("Reporting period start cannot be null");
        if (reportingPeriodEnd == null) throw new IllegalArgumentException("Reporting period end cannot be null");
        if (exportType == null || exportType.trim().isEmpty()) throw new IllegalArgumentException("Export type cannot be null or empty");
        if (exportedBy == null || exportedBy.trim().isEmpty()) throw new IllegalArgumentException("Exported by cannot be null or empty");
        if (hmisVersion == null || hmisVersion.trim().isEmpty()) throw new IllegalArgumentException("HMIS version cannot be null or empty");

        this.exportId = exportId;
        this.clientId = clientId;
        this.reportingPeriodStart = reportingPeriodStart;
        this.reportingPeriodEnd = reportingPeriodEnd;
        this.exportType = exportType;
        this.includedEnrollments = includedEnrollments;
        this.exportedBy = exportedBy;
        this.exportedByUserId = exportedByUserId;
        this.hmisVersion = hmisVersion;
        this.exportFilePath = exportFilePath;
        this.exportChecksum = exportChecksum;
        this.recordCount = recordCount;
        this.containsPII = containsPII;
        this.privacyNotice = privacyNotice;
    }

    public UUID exportId() {
        return exportId;
    }

    public UUID clientId() {
        return clientId;
    }

    public LocalDate reportingPeriodStart() {
        return reportingPeriodStart;
    }

    public LocalDate reportingPeriodEnd() {
        return reportingPeriodEnd;
    }

    public String exportType() {
        return exportType;
    }

    public List<UUID> includedEnrollments() {
        return includedEnrollments;
    }

    public String exportedBy() {
        return exportedBy;
    }

    public UUID exportedByUserId() {
        return exportedByUserId;
    }

    public String hmisVersion() {
        return hmisVersion;
    }

    public String exportFilePath() {
        return exportFilePath;
    }

    public String exportChecksum() {
        return exportChecksum;
    }

    public int recordCount() {
        return recordCount;
    }

    public boolean containsPII() {
        return containsPII;
    }

    public String privacyNotice() {
        return privacyNotice;
    }

    // JavaBean-style getters
    public UUID getExportId() { return exportId; }
    public UUID getClientId() { return clientId; }
    public LocalDate getReportingPeriodStart() { return reportingPeriodStart; }
    public LocalDate getReportingPeriodEnd() { return reportingPeriodEnd; }
    public String getExportType() { return exportType; }
    public List<UUID> getIncludedEnrollments() { return includedEnrollments; }
    public String getExportedBy() { return exportedBy; }
    public UUID getExportedByUserId() { return exportedByUserId; }
    public String getHmisVersion() { return hmisVersion; }
    public String getExportFilePath() { return exportFilePath; }
    public String getExportChecksum() { return exportChecksum; }
    public int getRecordCount() { return recordCount; }
    public boolean isContainsPII() { return containsPII; }
    public String getPrivacyNotice() { return privacyNotice; }
}