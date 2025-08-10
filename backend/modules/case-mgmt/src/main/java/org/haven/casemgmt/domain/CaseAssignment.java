package org.haven.casemgmt.domain;

import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;

public record CaseAssignment(
    String assigneeId,
    CodeableConcept role,
    Instant assignedAt
) {
}