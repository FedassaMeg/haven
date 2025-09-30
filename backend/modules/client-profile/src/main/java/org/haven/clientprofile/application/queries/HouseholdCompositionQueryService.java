package org.haven.clientprofile.application.queries;

import org.haven.clientprofile.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Query service for household composition read models
 * Provides optimized queries for household member information
 */
@Service
@Transactional(readOnly = true)
public class HouseholdCompositionQueryService {
    
    private final HouseholdCompositionRepository householdRepository;
    private final ClientRepository clientRepository;
    
    public HouseholdCompositionQueryService(
            HouseholdCompositionRepository householdRepository,
            ClientRepository clientRepository) {
        this.householdRepository = householdRepository;
        this.clientRepository = clientRepository;
    }
    
    /**
     * Get complete household composition with all member details
     */
    public Optional<HouseholdCompositionReadModel> getHouseholdComposition(HouseholdCompositionId compositionId) {
        return householdRepository.findById(compositionId)
            .map(this::buildCompleteReadModel);
    }
    
    /**
     * Get active household members as of a specific date
     */
    public List<HouseholdMemberReadModel> getActiveHouseholdMembers(HouseholdCompositionId compositionId, LocalDate asOfDate) {
        return householdRepository.findById(compositionId)
            .map(composition -> buildActiveMemberReadModels(composition, asOfDate))
            .orElse(List.of());
    }
    
    /**
     * Find active household for a client with full member details
     */
    public Optional<HouseholdCompositionReadModel> getActiveHouseholdForClient(ClientId clientId, LocalDate asOfDate) {
        return householdRepository.findActiveHouseholdForClient(clientId, asOfDate)
            .map(this::buildCompleteReadModel);
    }
    
    /**
     * Get households by head of household with summary information
     */
    public List<HouseholdCompositionReadModel> getHouseholdsByHeadOfHousehold(ClientId headOfHouseholdId) {
        return householdRepository.findByHeadOfHouseholdId(headOfHouseholdId)
            .stream()
            .map(this::buildCompleteReadModel)
            .toList();
    }
    
    /**
     * Get households where client is a member with summary information
     */
    public List<HouseholdCompositionReadModel> getHouseholdsByMember(ClientId memberId) {
        return householdRepository.findByMemberId(memberId)
            .stream()
            .map(this::buildCompleteReadModel)
            .toList();
    }
    
    /**
     * Get household composition history for a client
     */
    public List<HouseholdMemberReadModel> getClientHouseholdHistory(ClientId clientId) {
        List<HouseholdComposition> compositions = householdRepository.findByMemberId(clientId);
        
        return compositions.stream()
            .flatMap(composition -> composition.getMembershipHistory().stream()
                .filter(membership -> membership.getMemberId().equals(clientId))
                .map(membership -> buildMemberReadModel(composition, membership)))
            .collect(Collectors.toList());
    }
    
    /**
     * Build complete read model with all member details
     */
    private HouseholdCompositionReadModel buildCompleteReadModel(HouseholdComposition composition) {
        LocalDate today = LocalDate.now();
        
        // Get head of household information
        Client headOfHousehold = clientRepository.findById(composition.getHeadOfHouseholdId())
            .orElse(null);
        
        // Build member read models including head of household
        List<HouseholdMemberReadModel> allMembers = buildAllMemberReadModels(composition);
        List<HouseholdMemberReadModel> activeMembers = allMembers.stream()
            .filter(member -> isActiveMemberOn(member, today))
            .toList();
        
        // Build custody change models
        List<HouseholdCompositionReadModel.CustodyChangeReadModel> custodyChanges = 
            buildCustodyChangeReadModels(composition);
        
        return new HouseholdCompositionReadModel(
            composition.getId().getValue(),
            composition.getHeadOfHouseholdId().value(),
            headOfHousehold != null && headOfHousehold.getPrimaryName() != null && !headOfHousehold.getPrimaryName().given().isEmpty() ? 
                headOfHousehold.getPrimaryName().given().get(0) : "Unknown",
            headOfHousehold != null && headOfHousehold.getPrimaryName() != null ? 
                headOfHousehold.getPrimaryName().family() : "Unknown",
            headOfHousehold != null && headOfHousehold.getPrimaryName() != null ? 
                headOfHousehold.getPrimaryName().text() : "Unknown",
            headOfHousehold != null ? headOfHousehold.getBirthDate() : null,
            composition.getCompositionDate(),
            composition.getHouseholdType(),
            composition.getNotes(),
            composition.getCreatedAt(),
            composition.getHouseholdSizeOn(today),
            allMembers.size(),
            (int) activeMembers.stream().filter(this::isChildRelationship).count(),
            allMembers,
            activeMembers,
            custodyChanges
        );
    }
    
    /**
     * Build all member read models including head of household
     */
    private List<HouseholdMemberReadModel> buildAllMemberReadModels(HouseholdComposition composition) {
        List<HouseholdMemberReadModel> members = composition.getMembershipHistory()
            .stream()
            .map(membership -> buildMemberReadModel(composition, membership))
            .collect(Collectors.toList());
        
        // Add head of household
        Client headOfHousehold = clientRepository.findById(composition.getHeadOfHouseholdId())
            .orElse(null);
        
        if (headOfHousehold != null) {
            HouseholdMemberReadModel headReadModel = HouseholdMemberReadModel.forHeadOfHousehold(
                composition.getId().getValue(),
                composition.getHeadOfHouseholdId().value(),
                headOfHousehold.getPrimaryName() != null && !headOfHousehold.getPrimaryName().given().isEmpty() ? 
                    headOfHousehold.getPrimaryName().given().get(0) : "Unknown",
                headOfHousehold.getPrimaryName() != null ? 
                    headOfHousehold.getPrimaryName().family() : "Unknown",
                headOfHousehold.getPrimaryName() != null ? 
                    headOfHousehold.getPrimaryName().text() : "Unknown",
                headOfHousehold.getBirthDate(),
                composition.getCompositionDate(),
                composition.getCreatedAt()
            );
            members.add(0, headReadModel); // Add head of household first
        }
        
        return members;
    }
    
    /**
     * Build active member read models for a specific date
     */
    private List<HouseholdMemberReadModel> buildActiveMemberReadModels(HouseholdComposition composition, LocalDate asOfDate) {
        return buildAllMemberReadModels(composition).stream()
            .filter(member -> isActiveMemberOn(member, asOfDate))
            .toList();
    }
    
    /**
     * Build member read model from membership record
     */
    private HouseholdMemberReadModel buildMemberReadModel(HouseholdComposition composition, HouseholdMembershipRecord membership) {
        Client member = clientRepository.findById(membership.getMemberId()).orElse(null);
        
        return new HouseholdMemberReadModel(
            membership.getMembershipId(),
            composition.getId().getValue(),
            membership.getMemberId().value(),
            member != null && member.getPrimaryName() != null && !member.getPrimaryName().given().isEmpty() ? 
                member.getPrimaryName().given().get(0) : "Unknown",
            member != null && member.getPrimaryName() != null ? 
                member.getPrimaryName().family() : "Unknown",
            member != null && member.getPrimaryName() != null ? 
                member.getPrimaryName().text() : "Unknown",
            member != null ? member.getBirthDate() : null,
            membership.getRelationship().coding().isEmpty() ? null : 
                membership.getRelationship().coding().get(0).code(),
            membership.getRelationship().coding().isEmpty() ? null : 
                membership.getRelationship().coding().get(0).display(),
            membership.getStartDate(),
            membership.getEndDate(),
            membership.isActive(),
            false, // Not head of household
            membership.getRecordedBy(),
            membership.getReason(),
            membership.getRecordedAt(),
            membership.getDurationDays()
        );
    }
    
    /**
     * Build custody change read models
     */
    private List<HouseholdCompositionReadModel.CustodyChangeReadModel> buildCustodyChangeReadModels(HouseholdComposition composition) {
        return composition.getMembershipHistory().stream()
            .filter(membership -> membership.getReason() != null && 
                   (membership.getReason().toLowerCase().contains("custody") || 
                    membership.getReason().toLowerCase().contains("court order")))
            .map(membership -> {
                Client child = clientRepository.findById(membership.getMemberId()).orElse(null);
                return new HouseholdCompositionReadModel.CustodyChangeReadModel(
                    membership.getMembershipId(),
                    membership.getMemberId().value(),
                    child != null && child.getPrimaryName() != null && !child.getPrimaryName().given().isEmpty() ? 
                        child.getPrimaryName().given().get(0) : "Unknown",
                    child != null && child.getPrimaryName() != null ? 
                        child.getPrimaryName().family() : "Unknown",
                    null, // Previous relationship would need to be tracked separately
                    membership.getRelationship().coding().isEmpty() ? null : 
                        membership.getRelationship().coding().get(0).code(),
                    membership.getStartDate(),
                    extractCourtOrderFromReason(membership.getReason()),
                    membership.getRecordedBy(),
                    membership.getRecordedAt()
                );
            })
            .toList();
    }
    
    private boolean isActiveMemberOn(HouseholdMemberReadModel member, LocalDate date) {
        if (member.isHeadOfHousehold()) {
            return true; // Head of household is always active
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
    
    private String extractCourtOrderFromReason(String reason) {
        if (reason == null) return null;
        
        // Simple extraction - in reality this would be more sophisticated
        String[] parts = reason.split("court order:");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return reason;
    }
}