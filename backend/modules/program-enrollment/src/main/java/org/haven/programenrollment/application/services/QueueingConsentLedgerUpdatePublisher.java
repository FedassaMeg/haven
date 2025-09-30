package org.haven.programenrollment.application.services;

import org.haven.programenrollment.infrastructure.persistence.JpaConsentLedgerUpdateEntity;
import org.haven.programenrollment.infrastructure.persistence.JpaConsentLedgerUpdateRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class QueueingConsentLedgerUpdatePublisher implements ConsentLedgerUpdatePublisher {

    private final JpaConsentLedgerUpdateRepository repository;

    public QueueingConsentLedgerUpdatePublisher(JpaConsentLedgerUpdateRepository repository) {
        this.repository = repository;
    }

    @Override
    public void publishPendingUpdate(UUID consentId,
                                     UUID packetId,
                                     String sourceSystem,
                                     String payloadHash) {
        UUID id = UUID.randomUUID();
        JpaConsentLedgerUpdateEntity entity = new JpaConsentLedgerUpdateEntity(
            id,
            consentId,
            packetId,
            sourceSystem,
            payloadHash,
            "PENDING",
            Instant.now(),
            null,
            null
        );
        repository.save(entity);
    }

    @Override
    public void publishUpdate(Map<String, Object> ledgerData) {
        // Extract relevant fields from ledgerData
        UUID consentId = ledgerData.containsKey("consentId") ?
            UUID.fromString(ledgerData.get("consentId").toString()) : null;
        UUID packetId = ledgerData.containsKey("packetId") ?
            UUID.fromString(ledgerData.get("packetId").toString()) : null;
        String sourceSystem = ledgerData.getOrDefault("sourceSystem", "CE_EXPORT").toString();
        String payloadHash = ledgerData.getOrDefault("payloadHash", "").toString();

        if (consentId != null) {
            publishPendingUpdate(consentId, packetId, sourceSystem, payloadHash);
        }
    }
}
