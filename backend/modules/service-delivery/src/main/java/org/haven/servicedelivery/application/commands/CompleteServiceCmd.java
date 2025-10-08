package org.haven.servicedelivery.application.commands;

import org.haven.servicedelivery.domain.ServiceEpisode.ServiceCompletionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompleteServiceCmd(
    UUID episodeId,
    LocalDateTime endTime,
    String outcome,
    ServiceCompletionStatus status,
    String notes
) {
}