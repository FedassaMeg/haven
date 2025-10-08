package org.haven.api.households.dto;

import org.haven.clientprofile.domain.HouseholdComposition;
import org.haven.clientprofile.domain.HouseholdMembershipRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HouseholdCompositionResponse(
    UUID id,
    UUID headOfHouseholdId,
    LocalDate compositionDate,
    HouseholdComposition.HouseholdType householdType,
    String notes,
    Instant createdAt,
    int currentHouseholdSize,
    List<HouseholdMemberResponse> currentMembers
) {
    
    public static HouseholdCompositionResponse fromDomain(HouseholdComposition composition) {
        LocalDate today = LocalDate.now();
        List<HouseholdMembershipRecord> activeMembers = composition.getActiveMemberships(today);
        
        List<HouseholdMemberResponse> memberResponses = activeMembers.stream()
            .map(HouseholdMemberResponse::fromDomain)
            .toList();
        
        return new HouseholdCompositionResponse(
            composition.getId().getValue(),
            composition.getHeadOfHouseholdId().value(),
            composition.getCompositionDate(),
            composition.getHouseholdType(),
            composition.getNotes(),
            composition.getCreatedAt(),
            composition.getHouseholdSizeOn(today),
            memberResponses
        );
    }
}