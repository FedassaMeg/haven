package org.haven.api.households.dto;

import org.haven.clientprofile.domain.HouseholdComposition;
import org.haven.clientprofile.domain.HouseholdMembershipRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HouseholdHistoryResponse(
    UUID id,
    UUID headOfHouseholdId,
    LocalDate compositionDate,
    HouseholdComposition.HouseholdType householdType,
    String notes,
    Instant createdAt,
    List<HouseholdMemberResponse> allMemberships,
    List<CustodyChangeEvent> custodyChanges
) {
    
    public static HouseholdHistoryResponse fromDomain(HouseholdComposition composition) {
        List<HouseholdMemberResponse> allMembers = composition.getAllMemberships().stream()
            .map(HouseholdMemberResponse::fromDomain)
            .toList();
        
        // Extract custody changes from membership history
        List<CustodyChangeEvent> custodyChanges = composition.getAllMemberships().stream()
            .filter(m -> m.getReason() != null && 
                        (m.getReason().toLowerCase().contains("custody") || 
                         m.getReason().toLowerCase().contains("court order")))
            .map(m -> new CustodyChangeEvent(
                m.getMembershipId(),
                m.getMemberId().value(),
                m.getRelationship(),
                m.getStartDate(),
                m.getReason(),
                m.getRecordedBy(),
                m.getRecordedAt()
            ))
            .toList();
        
        return new HouseholdHistoryResponse(
            composition.getId().getValue(),
            composition.getHeadOfHouseholdId().value(),
            composition.getCompositionDate(),
            composition.getHouseholdType(),
            composition.getNotes(),
            composition.getCreatedAt(),
            allMembers,
            custodyChanges
        );
    }
    
    public record CustodyChangeEvent(
        UUID membershipId,
        UUID childId,
        org.haven.shared.vo.CodeableConcept newCustodyRelationship,
        LocalDate effectiveDate,
        String courtOrderDetails,
        String recordedBy,
        Instant recordedAt
    ) {}
}