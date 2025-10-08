package org.haven.programenrollment.domain.events;

import org.haven.clientprofile.domain.HouseholdCompositionId;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.hmis.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EnrollmentTransitionedToRrh extends DomainEvent {
    private final UUID thEnrollmentId;
    private final UUID rrhEnrollmentId;
    private final UUID clientId;
    private final UUID rrhProgramId;
    private final LocalDate residentialMoveInDate;
    private final HouseholdCompositionId householdCompositionId;
    private final RelationshipToHeadOfHousehold relationshipToHoH;
    private final PriorLivingSituation priorLivingSituation;
    private final LengthOfStay lengthOfStay;
    private final DisablingCondition disablingCondition;

    public EnrollmentTransitionedToRrh(
        UUID thEnrollmentId,
        UUID rrhEnrollmentId,
        UUID clientId,
        UUID rrhProgramId,
        LocalDate residentialMoveInDate,
        HouseholdCompositionId householdCompositionId,
        RelationshipToHeadOfHousehold relationshipToHoH,
        PriorLivingSituation priorLivingSituation,
        LengthOfStay lengthOfStay,
        DisablingCondition disablingCondition,
        Instant occurredAt
    ) {
        super(thEnrollmentId, occurredAt);
        this.thEnrollmentId = thEnrollmentId;
        this.rrhEnrollmentId = rrhEnrollmentId;
        this.clientId = clientId;
        this.rrhProgramId = rrhProgramId;
        this.residentialMoveInDate = residentialMoveInDate;
        this.householdCompositionId = householdCompositionId;
        this.relationshipToHoH = relationshipToHoH;
        this.priorLivingSituation = priorLivingSituation;
        this.lengthOfStay = lengthOfStay;
        this.disablingCondition = disablingCondition;
    }

    @Override
    public String eventType() {
        return "program-enrollment.transitioned-to-rrh";
    }

    public UUID thEnrollmentId() {
        return thEnrollmentId;
    }

    public UUID rrhEnrollmentId() {
        return rrhEnrollmentId;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID rrhProgramId() {
        return rrhProgramId;
    }

    public LocalDate residentialMoveInDate() {
        return residentialMoveInDate;
    }

    public HouseholdCompositionId householdCompositionId() {
        return householdCompositionId;
    }

    public RelationshipToHeadOfHousehold relationshipToHoH() {
        return relationshipToHoH;
    }

    public PriorLivingSituation priorLivingSituation() {
        return priorLivingSituation;
    }

    public LengthOfStay lengthOfStay() {
        return lengthOfStay;
    }

    public DisablingCondition disablingCondition() {
        return disablingCondition;
    }

    // JavaBean-style getters
    public UUID getThEnrollmentId() { return thEnrollmentId; }
    public UUID getRrhEnrollmentId() { return rrhEnrollmentId; }
    public UUID getClientId() { return clientId; }
    public UUID getRrhProgramId() { return rrhProgramId; }
    public LocalDate getResidentialMoveInDate() { return residentialMoveInDate; }
    public HouseholdCompositionId getHouseholdCompositionId() { return householdCompositionId; }
    public RelationshipToHeadOfHousehold getRelationshipToHoH() { return relationshipToHoH; }
    public PriorLivingSituation getPriorLivingSituation() { return priorLivingSituation; }
    public LengthOfStay getLengthOfStay() { return lengthOfStay; }
    public DisablingCondition getDisablingCondition() { return disablingCondition; }
}