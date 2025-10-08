package org.haven.reporting.domain;

import jakarta.persistence.*;
import org.haven.shared.security.AccessContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Audit metadata for HUD export jobs
 * Captures compliance tracking data per 24 CFR 578 requirements
 *
 * Records:
 * - Who requested the export (AccessContext with IP, session, user agent)
 * - What data was included (reporting period, project IDs, CoC code)
 * - When it was generated
 * - Integrity verification (SHA-256 hash of result set)
 * - VAWA compliance metrics (records suppressed/redacted)
 */
@Entity
@Table(name = "export_audit_metadata", indexes = {
    @Index(name = "idx_export_job_id", columnList = "export_job_id"),
    @Index(name = "idx_export_requested_by", columnList = "requested_by_user_id"),
    @Index(name = "idx_export_generated_at", columnList = "generated_at"),
    @Index(name = "idx_export_period", columnList = "reporting_period_start, reporting_period_end")
})
public class ExportAuditMetadata {

    @Id
    private UUID exportAuditId;

    @Column(nullable = false, unique = true)
    private UUID exportJobId;

    // Access Context - who requested the export
    @Column(nullable = false)
    private UUID requestedByUserId;

    @Column(nullable = false, length = 255)
    private String requestedByUserName;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 255)
    private String sessionId;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 1000)
    private String accessReason;

    // Export parameters
    @Column(nullable = false, length = 50)
    private String exportType; // HMIS_CSV, CoC_APR, ESG_CAPER, etc.

    @Column(nullable = false)
    private LocalDate reportingPeriodStart;

    @Column(nullable = false)
    private LocalDate reportingPeriodEnd;

    @ElementCollection
    @CollectionTable(name = "export_included_projects", joinColumns = @JoinColumn(name = "export_audit_id"))
    @Column(name = "project_id")
    private List<UUID> includedProjectIds = new ArrayList<>();

    @Column(length = 50)
    private String cocCode;

    // Integrity verification
    @Column(nullable = false, length = 64)
    private String sha256Hash;

    @Column(length = 1000)
    private String blobStorageUrl;

    // Statistics
    @Column(nullable = false)
    private Long totalRecordsGenerated;

    @Column(nullable = false)
    private Long vawaSupressedRecords;

    @Column(nullable = false)
    private Long vawaRedactedFields;

    @ElementCollection
    @CollectionTable(name = "export_generated_files", joinColumns = @JoinColumn(name = "export_audit_id"))
    @Column(name = "file_name")
    private List<String> generatedCsvFiles = new ArrayList<>();

    // Timestamps
    @Column(nullable = false)
    private Instant generatedAt;

    @Column
    private Instant expiresAt; // Retention policy expiration

    protected ExportAuditMetadata() {
        // JPA constructor
    }

    public ExportAuditMetadata(
            UUID exportJobId,
            AccessContext accessContext,
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> includedProjectIds,
            String cocCode,
            String sha256Hash,
            String blobStorageUrl,
            Long totalRecordsGenerated,
            Long vawaSupressedRecords,
            Long vawaRedactedFields,
            List<String> generatedCsvFiles,
            Instant generatedAt,
            Instant expiresAt) {

        this.exportAuditId = UUID.randomUUID();
        this.exportJobId = exportJobId;

        // Extract from AccessContext
        this.requestedByUserId = accessContext.getUserId();
        this.requestedByUserName = accessContext.getUserName();
        this.ipAddress = accessContext.getIpAddress();
        this.sessionId = accessContext.getSessionId();
        this.userAgent = accessContext.getUserAgent();
        this.accessReason = accessContext.getAccessReason();

        this.exportType = exportType;
        this.reportingPeriodStart = reportingPeriodStart;
        this.reportingPeriodEnd = reportingPeriodEnd;
        this.includedProjectIds = new ArrayList<>(includedProjectIds);
        this.cocCode = cocCode;

        this.sha256Hash = sha256Hash;
        this.blobStorageUrl = blobStorageUrl;
        this.totalRecordsGenerated = totalRecordsGenerated;
        this.vawaSupressedRecords = vawaSupressedRecords;
        this.vawaRedactedFields = vawaRedactedFields;
        this.generatedCsvFiles = new ArrayList<>(generatedCsvFiles);

        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }

    // Getters

    public UUID getExportAuditId() {
        return exportAuditId;
    }

    public UUID getExportJobId() {
        return exportJobId;
    }

    public UUID getRequestedByUserId() {
        return requestedByUserId;
    }

    public String getRequestedByUserName() {
        return requestedByUserName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getAccessReason() {
        return accessReason;
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
        return new ArrayList<>(includedProjectIds);
    }

    public String getCocCode() {
        return cocCode;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public String getBlobStorageUrl() {
        return blobStorageUrl;
    }

    public Long getTotalRecordsGenerated() {
        return totalRecordsGenerated;
    }

    public Long getVawaSupressedRecords() {
        return vawaSupressedRecords;
    }

    public Long getVawaRedactedFields() {
        return vawaRedactedFields;
    }

    public List<String> getGeneratedCsvFiles() {
        return new ArrayList<>(generatedCsvFiles);
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Create export audit metadata with package information
     * Factory method for exports with digital signatures and encryption
     */
    public static ExportAuditMetadata withPackageInfo(
            UUID exportJobId,
            AccessContext accessContext,
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> includedProjectIds,
            String cocCode,
            String sha256Hash,
            String blobStorageUrl,
            Long totalRecordsGenerated,
            Long vawaSupressedRecords,
            Long vawaRedactedFields,
            List<String> generatedCsvFiles,
            Instant generatedAt,
            Instant expiresAt,
            String digitalSignature,
            boolean encrypted,
            String encryptionAlgorithm) {

        return new ExportAuditMetadata(
                exportJobId,
                accessContext,
                exportType,
                reportingPeriodStart,
                reportingPeriodEnd,
                includedProjectIds,
                cocCode,
                sha256Hash,
                blobStorageUrl,
                totalRecordsGenerated,
                vawaSupressedRecords,
                vawaRedactedFields,
                generatedCsvFiles,
                generatedAt,
                expiresAt
        );
    }
}
