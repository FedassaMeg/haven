package org.haven.intake.application.dto;

import org.haven.intake.domain.ReferralSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record PreIntakeContactDto(
    UUID tempClientId,
    String clientAlias,
    LocalDate contactDate,
    ReferralSource referralSource,
    String intakeWorkerName,
    Map<String, Object> workflowData,
    int currentStep,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt,
    boolean expired,
    boolean promoted,
    UUID promotedClientId
) {}
