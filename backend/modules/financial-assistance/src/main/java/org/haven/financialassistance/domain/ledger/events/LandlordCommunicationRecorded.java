package org.haven.financialassistance.domain.ledger.events;

import org.haven.financialassistance.domain.ledger.CommunicationType;
import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class LandlordCommunicationRecorded extends DomainEvent {
    private final String communicationId;
    private final String landlordId;
    private final String landlordName;
    private final CommunicationType communicationType;
    private final String subject;
    private final String content;
    private final LocalDate communicationDate;
    private final String recordedBy;

    public LandlordCommunicationRecorded(
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
    ) {
        super(ledgerId, occurredAt);
        this.communicationId = communicationId;
        this.landlordId = landlordId;
        this.landlordName = landlordName;
        this.communicationType = communicationType;
        this.subject = subject;
        this.content = content;
        this.communicationDate = communicationDate;
        this.recordedBy = recordedBy;
    }

    // JavaBean style getters
    public UUID getLedgerId() { return getAggregateId(); }
    public String getCommunicationId() { return communicationId; }
    public String getLandlordId() { return landlordId; }
    public String getLandlordName() { return landlordName; }
    public CommunicationType getCommunicationType() { return communicationType; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public LocalDate getCommunicationDate() { return communicationDate; }
    public String getRecordedBy() { return recordedBy; }

    // Record style getters
    public UUID ledgerId() { return getAggregateId(); }
    public String communicationId() { return communicationId; }
    public String landlordId() { return landlordId; }
    public String landlordName() { return landlordName; }
    public CommunicationType communicationType() { return communicationType; }
    public String subject() { return subject; }
    public String content() { return content; }
    public LocalDate communicationDate() { return communicationDate; }
    public String recordedBy() { return recordedBy; }

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