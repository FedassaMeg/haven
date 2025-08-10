package org.haven.casemgmt.domain;

import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;

public record CaseParticipant(
    String participantId,
    CodeableConcept role,
    Period period
) {
    public boolean isActive() {
        return period == null || period.isActive();
    }
}