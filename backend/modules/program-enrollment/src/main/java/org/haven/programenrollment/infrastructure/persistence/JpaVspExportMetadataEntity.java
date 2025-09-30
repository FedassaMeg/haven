package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.programenrollment.domain.vsp.VspExportMetadata;
import org.haven.shared.vo.hmis.VawaRecipientCategory;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "vsp_export_metadata",
    indexes = {
        @Index(name = "idx_vsp_export_recipient", columnList = "recipient"),
        @Index(name = "idx_vsp_export_packet_hash", columnList = "packet_hash"),
        @Index(name = "idx_vsp_export_ce_hash", columnList = "ce_hash_key"),
        @Index(name = "idx_vsp_export_status", columnList = "status"),
        @Index(name = "idx_vsp_export_expiry", columnList = "expiry_date"),
        @Index(name = "idx_vsp_export_timestamp", columnList = "export_timestamp")
    }
)
public class JpaVspExportMetadataEntity {

    @Id
    @Column(name = "export_id")
    private UUID exportId;

    @Column(name = "recipient", nullable = false, length = 500)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_category", nullable = false, length = 50)
    private VawaRecipientCategory recipientCategory;

    @Column(name = "consent_basis", nullable = false, length = 500)
    private String consentBasis;

    @Column(name = "packet_hash", nullable = false, length = 128)
    private String packetHash;

    @Column(name = "ce_hash_key", nullable = false, length = 128)
    private String ceHashKey;

    @Column(name = "export_timestamp", nullable = false)
    private Instant exportTimestamp;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "share_scopes", columnDefinition = "text[]")
    private String[] shareScopes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anonymization_rules", nullable = false)
    private Map<String, Object> anonymizationRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "initiated_by", nullable = false, length = 200)
    private String initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VspExportMetadata.ExportStatus status;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 200)
    private String revokedBy;

    @Column(name = "revocation_reason", length = 1000)
    private String revocationReason;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaVspExportMetadataEntity() {
        // JPA
    }

    public JpaVspExportMetadataEntity(
            UUID exportId,
            String recipient,
            VawaRecipientCategory recipientCategory,
            String consentBasis,
            String packetHash,
            String ceHashKey,
            Instant exportTimestamp,
            LocalDateTime expiryDate,
            String[] shareScopes,
            Map<String, Object> anonymizationRules,
            Map<String, Object> metadata,
            String initiatedBy,
            VspExportMetadata.ExportStatus status) {
        this.exportId = exportId;
        this.recipient = recipient;
        this.recipientCategory = recipientCategory;
        this.consentBasis = consentBasis;
        this.packetHash = packetHash;
        this.ceHashKey = ceHashKey;
        this.exportTimestamp = exportTimestamp;
        this.expiryDate = expiryDate;
        this.shareScopes = shareScopes;
        this.anonymizationRules = anonymizationRules;
        this.metadata = metadata;
        this.initiatedBy = initiatedBy;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getExportId() { return exportId; }
    public void setExportId(UUID exportId) { this.exportId = exportId; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public VawaRecipientCategory getRecipientCategory() { return recipientCategory; }
    public void setRecipientCategory(VawaRecipientCategory recipientCategory) {
        this.recipientCategory = recipientCategory;
    }

    public String getConsentBasis() { return consentBasis; }
    public void setConsentBasis(String consentBasis) { this.consentBasis = consentBasis; }

    public String getPacketHash() { return packetHash; }
    public void setPacketHash(String packetHash) { this.packetHash = packetHash; }

    public String getCeHashKey() { return ceHashKey; }
    public void setCeHashKey(String ceHashKey) { this.ceHashKey = ceHashKey; }

    public Instant getExportTimestamp() { return exportTimestamp; }
    public void setExportTimestamp(Instant exportTimestamp) {
        this.exportTimestamp = exportTimestamp;
    }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String[] getShareScopes() { return shareScopes; }
    public void setShareScopes(String[] shareScopes) { this.shareScopes = shareScopes; }

    public Map<String, Object> getAnonymizationRules() { return anonymizationRules; }
    public void setAnonymizationRules(Map<String, Object> anonymizationRules) {
        this.anonymizationRules = anonymizationRules;
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public VspExportMetadata.ExportStatus getStatus() { return status; }
    public void setStatus(VspExportMetadata.ExportStatus status) { this.status = status; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}