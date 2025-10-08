package org.haven.reporting.domain.events;

import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event: Export job completed successfully
 */
public class ExportJobCompleted extends DomainEvent {
    private final String blobStorageUrl;
    private final String sha256Hash;
    private final Long totalRecords;
    private final Long vawaSupressedRecords;
    private final List<String> generatedCsvFiles;
    private final Instant completedAt;

    public ExportJobCompleted(
            UUID exportJobId,
            String blobStorageUrl,
            String sha256Hash,
            Long totalRecords,
            Long vawaSupressedRecords,
            List<String> generatedCsvFiles,
            Instant completedAt) {
        super(exportJobId, completedAt);
        this.blobStorageUrl = blobStorageUrl;
        this.sha256Hash = sha256Hash;
        this.totalRecords = totalRecords;
        this.vawaSupressedRecords = vawaSupressedRecords;
        this.generatedCsvFiles = generatedCsvFiles;
        this.completedAt = completedAt;
    }

    public String getBlobStorageUrl() {
        return blobStorageUrl;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public Long getVawaSupressedRecords() {
        return vawaSupressedRecords;
    }

    public List<String> getGeneratedCsvFiles() {
        return generatedCsvFiles;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
