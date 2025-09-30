package org.haven.clientprofile.application.queries;

import org.haven.clientprofile.domain.HouseholdCompositionId;
import org.haven.shared.vo.CodeableConcept;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model for household member information
 * Optimized for queries about household composition and membership
 */
public record HouseholdMemberReadModel(
    UUID membershipId,
    UUID householdCompositionId,
    UUID memberId,
    String memberFirstName,
    String memberLastName,
    String memberFullName,
    LocalDate memberDateOfBirth,
    String relationshipCode,
    String relationshipDisplay,
    LocalDate membershipStartDate,
    LocalDate membershipEndDate,
    boolean isActive,
    boolean isHeadOfHousehold,
    String recordedBy,
    String reason,
    Instant recordedAt,
    long membershipDurationDays
) {
    
    public static HouseholdMemberReadModel forHeadOfHousehold(
            UUID householdCompositionId,
            UUID headOfHouseholdId,
            String firstName,
            String lastName,
            String fullName,
            LocalDate dateOfBirth,
            LocalDate compositionDate,
            Instant createdAt) {
        
        return new HouseholdMemberReadModel(
            null, // No membership record for head of household
            householdCompositionId,
            headOfHouseholdId,
            firstName,
            lastName,
            fullName,
            dateOfBirth,
            "HEAD_OF_HOUSEHOLD",
            "Head of Household",
            compositionDate,
            null, // Head of household has no end date
            true, // Always active
            true, // Is head of household
            "system",
            "Household composition created",
            createdAt,
            0 // Duration calculated differently for head of household
        );
    }
}