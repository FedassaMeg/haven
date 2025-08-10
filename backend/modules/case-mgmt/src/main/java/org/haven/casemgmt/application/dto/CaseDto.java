package org.haven.casemgmt.application.dto;

import org.haven.casemgmt.domain.CaseRecord.CaseStatus;
import org.haven.casemgmt.domain.CaseAssignment;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import java.time.Instant;
import java.util.UUID;

public record CaseDto(
    UUID id,
    UUID clientId,
    CodeableConcept caseType,
    CodeableConcept priority,
    CaseStatus status,
    String description,
    CaseAssignment assignment,
    int noteCount,
    Instant createdAt,
    Period period
) {}