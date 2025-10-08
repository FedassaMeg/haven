package org.haven.clientprofile.domain.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HouseholdMemberRemoved extends DomainEvent {
    private final UUID compositionId;
    private final UUID membershipId;
    private final UUID memberId;
    private final LocalDate effectiveDate;
    private final String recordedBy;
    private final String reason;

    public HouseholdMemberRemoved(
        UUID compositionId,
        UUID membershipId,
        UUID memberId,
        LocalDate effectiveDate,
        String recordedBy,
        String reason,
        Instant occurredAt
    ) {
        super(compositionId, occurredAt);
        this.compositionId = compositionId;
        this.membershipId = membershipId;
        this.memberId = memberId;
        this.effectiveDate = effectiveDate;
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

    public LocalDate effectiveDate() {
        return effectiveDate;
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
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getRecordedBy() { return recordedBy; }
    public String getReason() { return reason; }
}