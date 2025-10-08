package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class CustodyChangeRecorded extends DomainEvent {
    private final UUID compositionId;
    private final UUID childId;
    private final CodeableConcept newCustodyRelationship;
    private final LocalDate effectiveDate;
    private final String courtOrder;
    private final String recordedBy;

    public CustodyChangeRecorded(
        UUID compositionId,
        UUID childId,
        CodeableConcept newCustodyRelationship,
        LocalDate effectiveDate,
        String courtOrder,
        String recordedBy,
        Instant occurredAt
    ) {
        super(compositionId, occurredAt);
        this.compositionId = compositionId;
        this.childId = childId;
        this.newCustodyRelationship = newCustodyRelationship;
        this.effectiveDate = effectiveDate;
        this.courtOrder = courtOrder;
        this.recordedBy = recordedBy;
    }

    public UUID compositionId() {
        return compositionId;
    }

    public UUID childId() {
        return childId;
    }

    public CodeableConcept newCustodyRelationship() {
        return newCustodyRelationship;
    }

    public LocalDate effectiveDate() {
        return effectiveDate;
    }

    public String courtOrder() {
        return courtOrder;
    }

    public String recordedBy() {
        return recordedBy;
    }

    // JavaBean-style getters
    public UUID getCompositionId() { return compositionId; }
    public UUID getChildId() { return childId; }
    public CodeableConcept getNewCustodyRelationship() { return newCustodyRelationship; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getCourtOrder() { return courtOrder; }
    public String getRecordedBy() { return recordedBy; }
}