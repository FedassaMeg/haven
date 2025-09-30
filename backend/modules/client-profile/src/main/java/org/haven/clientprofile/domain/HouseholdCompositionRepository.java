package org.haven.clientprofile.domain;

import org.haven.shared.domain.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HouseholdCompositionRepository extends Repository<HouseholdComposition, HouseholdCompositionId> {
    
    /**
     * Find all compositions where the client is head of household
     */
    List<HouseholdComposition> findByHeadOfHouseholdId(ClientId headOfHouseholdId);
    
    /**
     * Find all compositions where the client is a member at any point
     */
    List<HouseholdComposition> findByMemberId(ClientId memberId);
    
    /**
     * Find active household composition for a client as of a specific date
     */
    Optional<HouseholdComposition> findActiveHouseholdForClient(ClientId clientId, LocalDate asOfDate);
    
    /**
     * Find compositions that have changes within a date range (for history queries)
     */
    List<HouseholdComposition> findWithChangesInDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find all compositions created between dates
     */
    List<HouseholdComposition> findByCompositionDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find compositions by household type
     */
    List<HouseholdComposition> findByHouseholdType(HouseholdComposition.HouseholdType householdType);
    
    /**
     * Find compositions with membership changes requiring custody documentation
     */
    List<HouseholdComposition> findWithCustodyChanges();
}