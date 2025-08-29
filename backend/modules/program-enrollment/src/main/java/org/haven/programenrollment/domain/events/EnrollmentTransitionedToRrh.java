package org.haven.programenrollment.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.hmis.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentTransitionedToRrh(
    UUID thEnrollmentId,
    UUID rrhEnrollmentId,
    UUID clientId,
    UUID rrhProgramId,
    LocalDate residentialMoveInDate,
    String householdId,
    RelationshipToHeadOfHousehold relationshipToHoH,
    PriorLivingSituation priorLivingSituation,
    LengthOfStay lengthOfStay,
    DisablingCondition disablingCondition,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public UUID aggregateId() {
        return thEnrollmentId;
    }
    
    @Override
    public String eventType() {
        return "program-enrollment.transitioned-to-rrh";
    }
}