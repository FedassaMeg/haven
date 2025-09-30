package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ce_export_receipts")
public class JpaCeExportReceiptEntity {

    @Id
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "packet_id")
    private UUID packetId;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "export_hash", nullable = false, length = 128)
    private String exportHash;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "share_scopes", columnDefinition = "text[]", nullable = false)
    private String[] shareScopes;

    @Column(name = "encryption_scheme", nullable = false, length = 50)
    private String encryptionScheme;

    @Column(name = "encryption_key_id", nullable = false, length = 120)
    private String encryptionKeyId;

    @Column(name = "delivery_endpoint", length = 255)
    private String deliveryEndpoint;

    @Column(name = "package_location", length = 255)
    private String packageLocation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    protected JpaCeExportReceiptEntity() {
        // JPA
    }

    public JpaCeExportReceiptEntity(UUID id,
                                    UUID enrollmentId,
                                    UUID consentId,
                                    UUID packetId,
                                    String recipient,
                                    String exportHash,
                                    String[] shareScopes,
                                    String encryptionScheme,
                                    String encryptionKeyId,
                                    String deliveryEndpoint,
                                    String packageLocation,
                                    Instant createdAt,
                                    String createdBy,
                                    Map<String, Object> metadata) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.consentId = consentId;
        this.packetId = packetId;
        this.recipient = recipient;
        this.exportHash = exportHash;
        this.shareScopes = shareScopes;
        this.encryptionScheme = encryptionScheme;
        this.encryptionKeyId = encryptionKeyId;
        this.deliveryEndpoint = deliveryEndpoint;
        this.packageLocation = packageLocation;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getConsentId() {
        return consentId;
    }

    public UUID getPacketId() {
        return packetId;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getExportHash() {
        return exportHash;
    }

    public String[] getShareScopes() {
        return shareScopes;
    }

    public String getEncryptionScheme() {
        return encryptionScheme;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public String getDeliveryEndpoint() {
        return deliveryEndpoint;
    }

    public String getPackageLocation() {
        return packageLocation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getReceiptId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    // Factory method for creating a simple export receipt
    public static JpaCeExportReceiptEntity createSimpleReceipt(
            UUID receiptId,
            String cocId,
            String exportType,
            String fileName,
            int recordCount,
            int fileSize,
            String encryptionKeyId,
            String initiatedBy) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cocId", cocId);
        metadata.put("exportType", exportType);
        metadata.put("fileName", fileName);
        metadata.put("recordCount", recordCount);
        metadata.put("fileSize", fileSize);

        return new JpaCeExportReceiptEntity(
            receiptId,
            null, // enrollmentId - not available in simple export
            null, // consentId - not available in simple export
            null, // packetId - not available in simple export
            cocId, // using cocId as recipient for now
            "", // exportHash - to be computed
            new String[]{}, // shareScopes - empty for simple export
            "AES-256-GCM", // default encryption scheme
            encryptionKeyId,
            null, // deliveryEndpoint
            fileName, // packageLocation
            Instant.now(),
            initiatedBy,
            metadata
        );
    }
}
