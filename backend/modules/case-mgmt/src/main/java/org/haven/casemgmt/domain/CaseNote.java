package org.haven.casemgmt.domain;

import java.time.Instant;
import java.util.UUID;

public record CaseNote(
    UUID id,
    String content,
    String authorId,
    Instant createdAt
) {
}