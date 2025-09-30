package org.haven.financialassistance.domain.ledger.events;

import org.haven.financialassistance.domain.ledger.CommunicationType;
import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LandlordCommunicationRecorded(
    UUID ledgerId,
    String communicationId,
    String landlordId,
    String landlordName,
    CommunicationType communicationType,
    String subject,
    String content,
    LocalDate communicationDate,
    String recordedBy,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return ledgerId;
    }

    @Override
    public String eventType() {
        return "LandlordCommunicationRecorded";
    }

    public static LandlordCommunicationRecorded create(UUID ledgerId, String communicationId,
                                                     String landlordId, String landlordName,
                                                     CommunicationType communicationType, String subject,
                                                     String content, LocalDate communicationDate,
                                                     String recordedBy) {
        return new LandlordCommunicationRecorded(
            ledgerId, communicationId, landlordId, landlordName, communicationType, subject,
            content, communicationDate, recordedBy, Instant.now()
        );
    }
}