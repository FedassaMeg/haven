package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HMISExportGenerated(
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
) implements DomainEvent {
    
    public HMISExportGenerated {
        if (exportId == null) throw new IllegalArgumentException("Export ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (reportingPeriodStart == null) throw new IllegalArgumentException("Reporting period start cannot be null");
        if (reportingPeriodEnd == null) throw new IllegalArgumentException("Reporting period end cannot be null");
        if (exportType == null || exportType.trim().isEmpty()) throw new IllegalArgumentException("Export type cannot be null or empty");
        if (exportedBy == null || exportedBy.trim().isEmpty()) throw new IllegalArgumentException("Exported by cannot be null or empty");
        if (hmisVersion == null || hmisVersion.trim().isEmpty()) throw new IllegalArgumentException("HMIS version cannot be null or empty");
        if (occurredAt == null) occurredAt = Instant.now();
    }
    
    @Override
    public UUID aggregateId() {
        return exportId;
    }
    
    @Override
    public String eventType() {
        return "HMISExportGenerated";
    }
}