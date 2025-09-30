package org.haven.clientprofile.application.queries;

import org.haven.clientprofile.domain.HouseholdComposition;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read model for complete household composition information
 * Includes head of household and all members with their details
 */
public record HouseholdCompositionReadModel(
    UUID id,
    UUID headOfHouseholdId,
    String headOfHouseholdFirstName,
    String headOfHouseholdLastName,
    String headOfHouseholdFullName,
    LocalDate headOfHouseholdDateOfBirth,
    LocalDate compositionDate,
    HouseholdComposition.HouseholdType householdType,
    String notes,
    Instant createdAt,
    int currentHouseholdSize,
    int totalMembersCount, // Including head of household
    int activeChildrenCount,
    List<HouseholdMemberReadModel> allMembers, // Including head of household
    List<HouseholdMemberReadModel> activeMembers, // Active as of today
    List<CustodyChangeReadModel> custodyChanges
) {
    
    /**
     * Get members active as of a specific date
     */
    public List<HouseholdMemberReadModel> getMembersActiveOn(LocalDate date) {
        return allMembers.stream()
            .filter(member -> isActiveMemberOn(member, date))
            .toList();
    }
    
    /**
     * Get count of household members active as of a specific date (including head of household)
     */
    public int getHouseholdSizeOn(LocalDate date) {
        return getMembersActiveOn(date).size();
    }
    
    /**
     * Get children active as of a specific date
     */
    public List<HouseholdMemberReadModel> getChildrenActiveOn(LocalDate date) {
        return getMembersActiveOn(date).stream()
            .filter(this::isChildRelationship)
            .toList();
    }
    
    private boolean isActiveMemberOn(HouseholdMemberReadModel member, LocalDate date) {
        if (member.isHeadOfHousehold()) {
            return date.isEqual(compositionDate) || date.isAfter(compositionDate);
        }
        
        LocalDate startDate = member.membershipStartDate();
        LocalDate endDate = member.membershipEndDate();
        
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        
        return endDate == null || date.isBefore(endDate) || date.isEqual(endDate);
    }
    
    private boolean isChildRelationship(HouseholdMemberReadModel member) {
        String relationship = member.relationshipCode();
        return relationship != null && 
               (relationship.toLowerCase().contains("child") || 
                relationship.toLowerCase().contains("son") || 
                relationship.toLowerCase().contains("daughter"));
    }
    
    public record CustodyChangeReadModel(
        UUID membershipId,
        UUID childId,
        String childFirstName,
        String childLastName,
        String previousRelationshipCode,
        String newRelationshipCode,
        LocalDate effectiveDate,
        String courtOrderReference,
        String recordedBy,
        Instant recordedAt
    ) {}
}