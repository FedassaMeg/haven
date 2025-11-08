package org.haven.api.intake.dto;

import java.time.Instant;
import java.util.UUID;

public record CreatePreIntakeContactResponse(
    UUID tempClientId,
    String clientAlias,
    Instant createdAt,
    Instant expiresAt
) {}
