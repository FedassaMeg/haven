package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_ledger_update_queue")
public class JpaConsentLedgerUpdateEntity {

    @Id
    private UUID id;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "packet_id")
    private UUID packetId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected JpaConsentLedgerUpdateEntity() {
        // JPA
    }

    public JpaConsentLedgerUpdateEntity(UUID id,
                                        UUID consentId,
                                        UUID packetId,
                                        String sourceSystem,
                                        String payloadHash,
                                        String status,
                                        Instant createdAt,
                                        Instant processedAt,
                                        String errorMessage) {
        this.id = id;
        this.consentId = consentId;
        this.packetId = packetId;
        this.sourceSystem = sourceSystem;
        this.payloadHash = payloadHash;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConsentId() {
        return consentId;
    }

    public UUID getPacketId() {
        return packetId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
    }
}
