package org.haven.reporting.domain;

import jakarta.persistence.*;
import org.haven.shared.security.AccessContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced audit metadata with package integrity tracking.
 * Includes digital signature and encryption metadata for tamper detection.
 */
@Entity
@Table(name = "export_audit_metadata_v2", indexes = {
    @Index(name = "idx_v2_export_job_id", columnList = "export_job_id"),
    @Index(name = "idx_v2_export_requested_by", columnList = "requested_by_user_id"),
    @Index(name = "idx_v2_export_generated_at", columnList = "generated_at")
})
public class ExportAuditMetadataV2 {

    @Id
    private UUID exportAuditId;

    @Column(nullable = false, unique = true)
    private UUID exportJobId;

    // Access Context
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
    private String exportType;

    @Column(nullable = false)
    private LocalDate reportingPeriodStart;

    @Column(nullable = false)
    private LocalDate reportingPeriodEnd;

    @ElementCollection
    @CollectionTable(name = "export_v2_included_projects", joinColumns = @JoinColumn(name = "export_audit_id"))
    @Column(name = "project_id")
    private List<UUID> includedProjectIds = new ArrayList<>();

    @Column(length = 50)
    private String cocCode;

    // Integrity verification
    @Column(nullable = false, length = 64)
    private String manifestHash; // SHA-256 of manifest

    @Column(length = 512)
    private String digitalSignature; // HMAC-SHA256 of manifest

    @Column(length = 1000)
    private String blobStorageUrl;

    // Package metadata
    @Column
    private Boolean encrypted;

    @Column(length = 100)
    private String encryptionAlgorithm;

    // Statistics
    @Column(nullable = false)
    private Long totalRecordsGenerated;

    @Column(nullable = false)
    private Long vawaSupressedRecords;

    @Column(nullable = false)
    private Long vawaRedactedFields;

    @ElementCollection
    @CollectionTable(name = "export_v2_generated_files", joinColumns = @JoinColumn(name = "export_audit_id"))
    @Column(name = "file_name")
    private List<String> generatedFiles = new ArrayList<>();

    // Timestamps
    @Column(nullable = false)
    private Instant generatedAt;

    @Column
    private Instant expiresAt;

    protected ExportAuditMetadataV2() {
        // JPA constructor
    }

    public static ExportAuditMetadataV2 create(
            UUID exportJobId,
            AccessContext accessContext,
            String exportType,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            List<UUID> includedProjectIds,
            String cocCode,
            String manifestHash,
            String digitalSignature,
            String blobStorageUrl,
            Boolean encrypted,
            String encryptionAlgorithm,
            Long totalRecordsGenerated,
            Long vawaSupressedRecords,
            Long vawaRedactedFields,
            List<String> generatedFiles,
            Instant generatedAt,
            Instant expiresAt) {

        ExportAuditMetadataV2 metadata = new ExportAuditMetadataV2();

        metadata.exportAuditId = UUID.randomUUID();
        metadata.exportJobId = exportJobId;

        metadata.requestedByUserId = accessContext.getUserId();
        metadata.requestedByUserName = accessContext.getUserName();
        metadata.ipAddress = accessContext.getIpAddress();
        metadata.sessionId = accessContext.getSessionId();
        metadata.userAgent = accessContext.getUserAgent();
        metadata.accessReason = accessContext.getAccessReason();

        metadata.exportType = exportType;
        metadata.reportingPeriodStart = reportingPeriodStart;
        metadata.reportingPeriodEnd = reportingPeriodEnd;
        metadata.includedProjectIds = new ArrayList<>(includedProjectIds);
        metadata.cocCode = cocCode;

        metadata.manifestHash = manifestHash;
        metadata.digitalSignature = digitalSignature;
        metadata.blobStorageUrl = blobStorageUrl;
        metadata.encrypted = encrypted;
        metadata.encryptionAlgorithm = encryptionAlgorithm;

        metadata.totalRecordsGenerated = totalRecordsGenerated;
        metadata.vawaSupressedRecords = vawaSupressedRecords;
        metadata.vawaRedactedFields = vawaRedactedFields;
        metadata.generatedFiles = new ArrayList<>(generatedFiles);

        metadata.generatedAt = generatedAt;
        metadata.expiresAt = expiresAt;

        return metadata;
    }

    public boolean verifyIntegrity(String expectedSignature) {
        return digitalSignature != null && digitalSignature.equals(expectedSignature);
    }

    // Getters
    public UUID getExportAuditId() { return exportAuditId; }
    public UUID getExportJobId() { return exportJobId; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public String getRequestedByUserName() { return requestedByUserName; }
    public String getManifestHash() { return manifestHash; }
    public String getDigitalSignature() { return digitalSignature; }
    public Boolean getEncrypted() { return encrypted; }
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
}
