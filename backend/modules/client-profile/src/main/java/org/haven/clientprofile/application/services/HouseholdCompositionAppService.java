package org.haven.clientprofile.application.services;

import org.haven.clientprofile.application.commands.*;
import org.haven.clientprofile.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class HouseholdCompositionAppService {
    
    private final HouseholdCompositionRepository householdRepository;
    
    public HouseholdCompositionAppService(HouseholdCompositionRepository householdRepository) {
        this.householdRepository = householdRepository;
    }
    
    public HouseholdCompositionId handle(CreateHouseholdCompositionCmd cmd) {
        HouseholdComposition composition = HouseholdComposition.create(
            cmd.headOfHouseholdId(),
            cmd.effectiveDate(),
            cmd.householdType(),
            cmd.recordedBy()
        );
        
        householdRepository.save(composition);
        return composition.getId();
    }
    
    public void handle(AddHouseholdMemberToCompositionCmd cmd) {
        HouseholdComposition composition = householdRepository.findById(cmd.compositionId())
            .orElseThrow(() -> new IllegalArgumentException("Household composition not found: " + cmd.compositionId()));
        
        composition.addMember(
            cmd.memberId(),
            cmd.relationship(),
            cmd.effectiveFrom(),
            cmd.effectiveTo(),
            cmd.recordedBy(),
            cmd.reason()
        );
        
        householdRepository.save(composition);
    }
    
    public void handle(RemoveHouseholdMemberCmd cmd) {
        HouseholdComposition composition = householdRepository.findById(cmd.compositionId())
            .orElseThrow(() -> new IllegalArgumentException("Household composition not found: " + cmd.compositionId()));
        
        composition.removeMember(
            cmd.memberId(),
            cmd.effectiveDate(),
            cmd.recordedBy(),
            cmd.reason()
        );
        
        householdRepository.save(composition);
    }
    
    public void handle(UpdateMemberRelationshipCmd cmd) {
        HouseholdComposition composition = householdRepository.findById(cmd.compositionId())
            .orElseThrow(() -> new IllegalArgumentException("Household composition not found: " + cmd.compositionId()));
        
        composition.updateMemberRelationship(
            cmd.memberId(),
            cmd.newRelationship(),
            cmd.effectiveDate(),
            cmd.recordedBy(),
            cmd.reason()
        );
        
        householdRepository.save(composition);
    }
    
    public void handle(RecordCustodyChangeCmd cmd) {
        HouseholdComposition composition = householdRepository.findById(cmd.compositionId())
            .orElseThrow(() -> new IllegalArgumentException("Household composition not found: " + cmd.compositionId()));
        
        composition.recordCustodyChange(
            cmd.childId(),
            cmd.newCustodyRelationship(),
            cmd.effectiveDate(),
            cmd.courtOrder(),
            cmd.recordedBy()
        );
        
        householdRepository.save(composition);
    }
    
    // Query methods
    
    @Transactional(readOnly = true)
    public HouseholdComposition findById(HouseholdCompositionId id) {
        return householdRepository.findById(id).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<HouseholdComposition> findByHeadOfHousehold(ClientId headOfHouseholdId) {
        return householdRepository.findByHeadOfHouseholdId(headOfHouseholdId);
    }
    
    @Transactional(readOnly = true)
    public List<HouseholdComposition> findByMember(ClientId memberId) {
        return householdRepository.findByMemberId(memberId);
    }
    
    @Transactional(readOnly = true)
    public HouseholdComposition findActiveHouseholdForClient(ClientId clientId, LocalDate asOfDate) {
        return householdRepository.findActiveHouseholdForClient(clientId, asOfDate).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<HouseholdComposition> findWithChangesInDateRange(LocalDate startDate, LocalDate endDate) {
        return householdRepository.findWithChangesInDateRange(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<HouseholdComposition> findByCompositionDateRange(LocalDate startDate, LocalDate endDate) {
        return householdRepository.findByCompositionDateBetween(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<HouseholdComposition> findByHouseholdType(HouseholdComposition.HouseholdType householdType) {
        return householdRepository.findByHouseholdType(householdType);
    }
    
    @Transactional(readOnly = true)
    public List<HouseholdComposition> findWithCustodyChanges() {
        return householdRepository.findWithCustodyChanges();
    }
}