package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaHouseholdCompositionRepository extends JpaRepository<JpaHouseholdCompositionEntity, UUID>, HouseholdCompositionRepository {
    
    @Override
    default void save(HouseholdComposition composition) {
        JpaHouseholdCompositionEntity entity = JpaHouseholdCompositionEntity.fromDomain(composition);
        save(entity);
    }
    
    @Override
    default Optional<HouseholdComposition> findById(HouseholdCompositionId id) {
        return findById(id.getValue())
            .map(JpaHouseholdCompositionEntity::toDomain);
    }
    
    @Override
    default List<HouseholdComposition> findByHeadOfHouseholdId(ClientId headOfHouseholdId) {
        return findByHeadOfHouseholdId(headOfHouseholdId.value())
            .stream()
            .map(JpaHouseholdCompositionEntity::toDomain)
            .toList();
    }
    
    @Query("SELECT hc FROM JpaHouseholdCompositionEntity hc WHERE hc.headOfHouseholdId = :headOfHouseholdId")
    List<JpaHouseholdCompositionEntity> findByHeadOfHouseholdId(@Param("headOfHouseholdId") UUID headOfHouseholdId);
    
    @Override
    default List<HouseholdComposition> findByMemberId(ClientId memberId) {
        return findCompositionsWithMember(memberId.value())
            .stream()
            .map(JpaHouseholdCompositionEntity::toDomain)
            .toList();
    }
    
    @Query("SELECT DISTINCT hc FROM JpaHouseholdCompositionEntity hc " +
           "JOIN hc.memberships m WHERE m.memberId = :memberId")
    List<JpaHouseholdCompositionEntity> findCompositionsWithMember(@Param("memberId") UUID memberId);
    
    @Override
    default Optional<HouseholdComposition> findActiveHouseholdForClient(ClientId clientId, LocalDate asOfDate) {
        // Check if client is head of household
        Optional<JpaHouseholdCompositionEntity> asHead = findActiveAsHeadOfHousehold(clientId.value(), asOfDate);
        if (asHead.isPresent()) {
            return asHead.map(JpaHouseholdCompositionEntity::toDomain);
        }
        
        // Check if client is a member
        return findActiveAsMember(clientId.value(), asOfDate)
            .map(JpaHouseholdCompositionEntity::toDomain);
    }
    
    @Query("SELECT hc FROM JpaHouseholdCompositionEntity hc " +
           "WHERE hc.headOfHouseholdId = :clientId " +
           "AND hc.compositionDate <= :asOfDate " +
           "ORDER BY hc.compositionDate DESC")
    Optional<JpaHouseholdCompositionEntity> findActiveAsHeadOfHousehold(@Param("clientId") UUID clientId, 
                                                                       @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT hc FROM JpaHouseholdCompositionEntity hc " +
           "JOIN hc.memberships m " +
           "WHERE m.memberId = :clientId " +
           "AND m.effectiveFrom <= :asOfDate " +
           "AND (m.effectiveTo IS NULL OR m.effectiveTo >= :asOfDate) " +
           "ORDER BY hc.compositionDate DESC")
    Optional<JpaHouseholdCompositionEntity> findActiveAsMember(@Param("clientId") UUID clientId, 
                                                              @Param("asOfDate") LocalDate asOfDate);
    
    @Override
    default List<HouseholdComposition> findWithChangesInDateRange(LocalDate startDate, LocalDate endDate) {
        return findWithMembershipChangesInRange(startDate, endDate)
            .stream()
            .map(JpaHouseholdCompositionEntity::toDomain)
            .toList();
    }
    
    @Query("SELECT DISTINCT hc FROM JpaHouseholdCompositionEntity hc " +
           "JOIN hc.memberships m " +
           "WHERE (m.effectiveFrom BETWEEN :startDate AND :endDate) " +
           "OR (m.effectiveTo BETWEEN :startDate AND :endDate)")
    List<JpaHouseholdCompositionEntity> findWithMembershipChangesInRange(@Param("startDate") LocalDate startDate,
                                                                         @Param("endDate") LocalDate endDate);
    
    @Override
    default List<HouseholdComposition> findByCompositionDateBetween(LocalDate startDate, LocalDate endDate) {
        return findByCompositionDateBetweenOrderByDate(startDate, endDate)
            .stream()
            .map(JpaHouseholdCompositionEntity::toDomain)
            .toList();
    }
    
    @Query("SELECT hc FROM JpaHouseholdCompositionEntity hc " +
           "WHERE hc.compositionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY hc.compositionDate DESC")
    List<JpaHouseholdCompositionEntity> findByCompositionDateBetweenOrderByDate(@Param("startDate") LocalDate startDate,
                                                                               @Param("endDate") LocalDate endDate);
    
    @Override
    default List<HouseholdComposition> findByHouseholdType(HouseholdComposition.HouseholdType householdType) {
        return findByHouseholdTypeEntity(householdType)
            .stream()
            .map(JpaHouseholdCompositionEntity::toDomain)
            .toList();
    }
    
    @Query("SELECT hc FROM JpaHouseholdCompositionEntity hc WHERE hc.householdType = :householdType")
    List<JpaHouseholdCompositionEntity> findByHouseholdTypeEntity(@Param("householdType") HouseholdComposition.HouseholdType householdType);
    
    @Override
    default List<HouseholdComposition> findWithCustodyChanges() {
        return findWithCustodyRelatedChanges()
            .stream()
            .map(JpaHouseholdCompositionEntity::toDomain)
            .toList();
    }
    
    @Query("SELECT DISTINCT hc FROM JpaHouseholdCompositionEntity hc " +
           "JOIN hc.memberships m " +
           "WHERE m.reason LIKE '%custody%' OR m.reason LIKE '%court order%'")
    List<JpaHouseholdCompositionEntity> findWithCustodyRelatedChanges();
    
    @Override
    default void delete(HouseholdComposition composition) {
        deleteById(composition.getId().getValue());
    }
    
    @Override
    default HouseholdCompositionId nextId() {
        return HouseholdCompositionId.from(java.util.UUID.randomUUID());
    }
}