package org.haven.api.households.dto;

import org.haven.clientprofile.domain.HouseholdMembershipRecord;
import org.haven.shared.vo.CodeableConcept;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberResponse(
    UUID membershipId,
    UUID memberId,
    CodeableConcept relationship,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String recordedBy,
    String reason,
    Instant recordedAt,
    boolean isActive
) {
    
    public static HouseholdMemberResponse fromDomain(HouseholdMembershipRecord membership) {
        return new HouseholdMemberResponse(
            membership.getMembershipId(),
            membership.getMemberId().value(),
            membership.getRelationship(),
            membership.getStartDate(),
            membership.getEndDate(),
            membership.getRecordedBy(),
            membership.getReason(),
            membership.getRecordedAt(),
            membership.isActive()
        );
    }
}