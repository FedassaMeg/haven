package org.haven.api.intake.dto;

import java.util.UUID;

public record PromoteClientResponse(
    UUID clientId,
    UUID tempClientId,
    String message
) {}
