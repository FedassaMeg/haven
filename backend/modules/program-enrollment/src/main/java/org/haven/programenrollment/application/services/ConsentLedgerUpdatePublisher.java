package org.haven.programenrollment.application.services;

import java.util.Map;
import java.util.UUID;

public interface ConsentLedgerUpdatePublisher {

    void publishPendingUpdate(UUID consentId,
                              UUID packetId,
                              String sourceSystem,
                              String payloadHash);

    void publishUpdate(Map<String, Object> ledgerData);
}
