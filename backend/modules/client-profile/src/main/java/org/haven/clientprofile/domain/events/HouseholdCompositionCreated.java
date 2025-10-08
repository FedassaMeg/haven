package org.haven.clientprofile.domain.events;

import org.haven.clientprofile.domain.HouseholdComposition.HouseholdType;
import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HouseholdCompositionCreated extends DomainEvent {
    private final UUID compositionId;
    private final UUID headOfHouseholdId;
    private final LocalDate effectiveDate;
    private final HouseholdType householdType;
    private final String recordedBy;

    public HouseholdCompositionCreated(
        UUID compositionId,
        UUID headOfHouseholdId,
        LocalDate effectiveDate,
        HouseholdType householdType,
        String recordedBy,
        Instant occurredAt
    ) {
        super(compositionId, occurredAt);
        this.compositionId = compositionId;
        this.headOfHouseholdId = headOfHouseholdId;
        this.effectiveDate = effectiveDate;
        this.householdType = householdType;
        this.recordedBy = recordedBy;
    }

    public UUID compositionId() {
        return compositionId;
    }

    public UUID headOfHouseholdId() {
        return headOfHouseholdId;
    }

    public LocalDate effectiveDate() {
        return effectiveDate;
    }

    public HouseholdType householdType() {
        return householdType;
    }

    public String recordedBy() {
        return recordedBy;
    }

    // JavaBean-style getters
    public UUID getCompositionId() { return compositionId; }
    public UUID getHeadOfHouseholdId() { return headOfHouseholdId; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public HouseholdType getHouseholdType() { return householdType; }
    public String getRecordedBy() { return recordedBy; }
}