package org.haven.housingassistance.application.dto;

import org.haven.housingassistance.domain.LandlordCommunication;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LandlordCommunicationDto(
    UUID id,
    UUID landlordId,
    UUID clientId,
    UUID housingAssistanceId,
    String channel,
    String subject,
    String body,
    Map<String, Object> sharedFields,
    String recipientContact,
    Boolean consentChecked,
    String consentType,
    String sentStatus,
    Instant sentAt,
    UUID sentBy,
    Instant createdAt,
    Instant updatedAt
) {
    public static LandlordCommunicationDto from(LandlordCommunication communication) {
        return new LandlordCommunicationDto(
            communication.getId(),
            communication.getLandlordId(),
            communication.getClientId(),
            communication.getHousingAssistanceId(),
            communication.getChannel(),
            communication.getSubject(),
            communication.getBody(),
            communication.getSharedFields(),
            communication.getRecipientContact(),
            communication.getConsentChecked(),
            communication.getConsentType(),
            communication.getSentStatus(),
            communication.getSentAt(),
            communication.getSentBy(),
            communication.getCreatedAt(),
            communication.getUpdatedAt()
        );
    }
}