package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ce_import_jobs")
public class JpaCeImportJobEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "import_format", nullable = false, length = 50)
    private String importFormat;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "initiated_by", length = 200)
    private String initiatedBy;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "successful_records")
    private Integer successfulRecords;

    @Column(name = "failed_records")
    private Integer failedRecords;

    @Column(name = "warning_count")
    private Integer warningCount;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected JpaCeImportJobEntity() {
        // JPA
    }

    public JpaCeImportJobEntity(UUID id,
                                String sourceSystem,
                                String importFormat,
                                String status,
                                String initiatedBy,
                                String fileName,
                                Integer totalRecords,
                                Integer successfulRecords,
                                Integer failedRecords,
                                Integer warningCount,
                                String errorLog,
                                Instant createdAt,
                                Instant completedAt) {
        this.id = id;
        this.sourceSystem = sourceSystem;
        this.importFormat = importFormat;
        this.status = status;
        this.initiatedBy = initiatedBy;
        this.fileName = fileName;
        this.totalRecords = totalRecords;
        this.successfulRecords = successfulRecords;
        this.failedRecords = failedRecords;
        this.warningCount = warningCount;
        this.errorLog = errorLog;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getImportFormat() {
        return importFormat;
    }

    public String getStatus() {
        return status;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public Integer getSuccessfulRecords() {
        return successfulRecords;
    }

    public Integer getFailedRecords() {
        return failedRecords;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void markProcessing() {
        this.status = "PROCESSING";
        this.createdAt = Instant.now();
        this.totalRecords = 0;
        this.successfulRecords = 0;
        this.failedRecords = 0;
        this.warningCount = 0;
    }

    public void markCompleted(Integer successfulRecords,
                              Integer failedRecords,
                              Integer warningCount,
                              String errorLog) {
        this.status = "COMPLETED";
        this.successfulRecords = successfulRecords;
        this.failedRecords = failedRecords;
        this.warningCount = warningCount;
        this.errorLog = errorLog;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorLog) {
        this.status = "FAILED";
        this.errorLog = errorLog;
        this.completedAt = Instant.now();
    }

    public void incrementTotals(boolean success, boolean warning) {
        this.totalRecords = safeIncrement(totalRecords);
        if (success) {
            this.successfulRecords = safeIncrement(successfulRecords);
        } else {
            this.failedRecords = safeIncrement(failedRecords);
        }
        if (warning) {
            this.warningCount = safeIncrement(warningCount);
        }
    }

    public void appendErrorLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (this.errorLog == null || this.errorLog.isBlank()) {
            this.errorLog = message;
        } else {
            this.errorLog = this.errorLog + System.lineSeparator() + message;
        }
    }

    private int safeIncrement(Integer value) {
        return value == null ? 1 : value + 1;
    }
}
