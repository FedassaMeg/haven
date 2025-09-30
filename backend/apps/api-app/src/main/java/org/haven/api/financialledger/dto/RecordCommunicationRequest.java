package org.haven.api.financialledger.dto;

import org.haven.financialassistance.domain.ledger.CommunicationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RecordCommunicationRequest(
    @NotBlank String communicationId,
    @NotBlank String landlordId,
    @NotBlank String landlordName,
    @NotNull CommunicationType communicationType,
    @NotBlank String subject,
    String content,
    @NotNull LocalDate communicationDate
) {}