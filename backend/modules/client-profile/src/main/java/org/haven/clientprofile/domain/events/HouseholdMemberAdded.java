package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HouseholdMemberAdded extends DomainEvent {
    private final UUID compositionId;
    private final UUID membershipId;
    private final UUID memberId;
    private final CodeableConcept relationship;
    private final LocalDate effectiveFrom;
    private final LocalDate effectiveTo;
    private final String recordedBy;
    private final String reason;

    public HouseholdMemberAdded(
        UUID compositionId,
        UUID membershipId,
        UUID memberId,
        CodeableConcept relationship,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String recordedBy,
        String reason,
        Instant occurredAt
    ) {
        super(compositionId, occurredAt);
        this.compositionId = compositionId;
        this.membershipId = membershipId;
        this.memberId = memberId;
        this.relationship = relationship;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.recordedBy = recordedBy;
        this.reason = reason;
    }

    public UUID compositionId() {
        return compositionId;
    }

    public UUID membershipId() {
        return membershipId;
    }

    public UUID memberId() {
        return memberId;
    }

    public CodeableConcept relationship() {
        return relationship;
    }

    public LocalDate effectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate effectiveTo() {
        return effectiveTo;
    }

    public String recordedBy() {
        return recordedBy;
    }

    public String reason() {
        return reason;
    }

    // JavaBean-style getters
    public UUID getCompositionId() { return compositionId; }
    public UUID getMembershipId() { return membershipId; }
    public UUID getMemberId() { return memberId; }
    public CodeableConcept getRelationship() { return relationship; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getRecordedBy() { return recordedBy; }
    public String getReason() { return reason; }
}
