package org.haven.servicedelivery.application.commands;

import java.time.LocalDateTime;
import java.util.UUID;

public record StartServiceCmd(
    UUID episodeId,
    LocalDateTime startTime,
    String location
) {
}